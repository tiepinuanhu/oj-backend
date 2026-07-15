package com.wxc.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.CommonConstant;
import com.wxc.oj.constant.RedisConstant;
import com.wxc.oj.enums.UserRoleEnum;
import com.wxc.oj.enums.problem.ProblemLevel;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.exception.ThrowUtils;
import com.wxc.oj.mapper.ProblemMapper;
import com.wxc.oj.model.req.problem.ProblemAddRequest;
import com.wxc.oj.model.req.problem.ProblemEditRequest;
import com.wxc.oj.model.req.problem.ProblemQueryRequest;
import com.wxc.oj.model.req.problem.ProblemTag;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Tag;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.service.ProblemEsService;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.model.vo.problem.ProblemVO;
import com.wxc.oj.model.vo.UserVO;
import com.wxc.oj.service.ProblemTagService;
import com.wxc.oj.service.TagService;
import com.wxc.oj.service.UserService;
import com.wxc.oj.utils.SqlUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.BeanUtils.copyProperties;

/**
 * 题目业务实现，对应表【problem】。
 * <p>
 * 职责概览：题目 CRUD、条件分页查询、实体与 VO 转换、标签关联维护；
 * 详情查询带 Redis 缓存；增删改后同步 ES 并失效缓存。
 *
 * @author 王新超
 * @createDate 2024-02-28 14:24:47
 */
@Service
@Slf4j(topic = "ProblemServiceImpl")
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    /** 用户服务：查询发布者信息、判断管理员可见范围 */
    @Resource
    private UserService userService;

    /** 标签服务：按题目 id 查标签、校验标签是否存在 */
    @Resource
    private TagService tagService;

    /** 题目-标签关联服务 */
    @Resource
    private ProblemTagService problemTagService;

    /** Redis：题目详情缓存读写与失效 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * ES 同步服务。使用 @Lazy 打破与 ProblemEsServiceImpl 的循环依赖
     *（ES 检索实现会再注入 ProblemService）。
     */
    @Lazy
    @Resource
    private ProblemEsService problemEsService;

    /**
     * 按 id 获取题目详情 VO。
     * <p>
     * 流程：先读 Redis → 命中则反序列化返回；未命中或反序列化失败则查库、转 VO、写回缓存。
     *
     * @param problemId 题目 id，须为正数
     * @return 含题面、发布者、标签、评测配置等的详情 VO
     */
    @Override
    public ProblemVO getProblemVOById(Long problemId) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法题目id");
        }
        String key = RedisConstant.CACHE_PROBLEM_KEY + problemId;
        String cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(cachedValue)) {
            try {
                return JSONUtil.toBean(cachedValue, ProblemVO.class);
            } catch (RuntimeException e) {
                // 缓存脏数据：删掉后走库
                log.warn("题目缓存反序列化失败，problemId={}", problemId, e);
                stringRedisTemplate.delete(key);
            }
        }
        Problem problem = this.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        ProblemVO problemVO = this.problem2VO(problem);
        String value = JSONUtil.toJsonStr(problemVO);
        stringRedisTemplate.opsForValue().set(key, value, RedisConstant.CACHE_PROBLEM_TTL, TimeUnit.MINUTES);
        return problemVO;
    }

    /**
     * 校验题目字段合法性。
     * <p>
     * {@code add == true}：创建场景，标题/内容/难度/发布者/是否公开均必填；
     * {@code add == false}：更新场景，仅对请求中非 null 的字段做校验。
     *
     * @param problem 待校验实体（可含 JSON 字符串形式的 judgeConfig）
     * @param add     是否为创建操作
     */
    @Override
    public void validProblem(Problem problem, boolean add) {
        ThrowUtils.throwIf(problem == null, ErrorCode.PARAMS_ERROR);
        String title = problem.getTitle();
        String content = problem.getContent();
        if (add || title != null) {
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR, "题目标题不能为空");
            ThrowUtils.throwIf(title.length() > 255, ErrorCode.PARAMS_ERROR, "题目标题过长");
        }
        if (add || content != null) {
            ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
        }
        if (add || problem.getLevel() != null) {
            checkLevel(problem.getLevel());
        }
        if (add || problem.getUserId() != null) {
            ThrowUtils.throwIf(problem.getUserId() == null || problem.getUserId() <= 0,
                    ErrorCode.PARAMS_ERROR, "发布者id不合法");
        }
        if (add) {
            ThrowUtils.throwIf(problem.getIsPublic() == null, ErrorCode.PARAMS_ERROR, "是否公开未填写");
        }
        // 评测配置可选；有值则须能解析且时间/内存限制为正
        if (StringUtils.isNotBlank(problem.getJudgeConfig())) {
            JudgeConfig judgeConfig;
            try {
                judgeConfig = JSONUtil.toBean(problem.getJudgeConfig(), JudgeConfig.class);
            } catch (RuntimeException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "评测配置格式错误");
            }
            ThrowUtils.throwIf(judgeConfig == null
                            || judgeConfig.getTimeLimit() == null || judgeConfig.getTimeLimit() <= 0
                            || judgeConfig.getMemoryLimit() == null || judgeConfig.getMemoryLimit() <= 0,
                    ErrorCode.PARAMS_ERROR, "评测配置不合法");
        }
    }

    /**
     * 根据查询请求构建 MyBatis-Plus 查询条件。
     * <p>
     * 支持：精确 id、发布者、难度；多标签（AND，须同时包含所有给定标签）；
     * 非管理员仅可见公开题；排序字段经白名单校验。
     * 关键词全文检索走 ES（{@link ProblemEsService}），此处不做 title/content 模糊查询。
     *
     * @param problemQueryRequest 列表筛选与排序参数（含当前登录用户 id，用于权限）
     * @return Lambda 查询包装器
     */
    @Override
    public LambdaQueryWrapper<Problem> getQueryWrapper(ProblemQueryRequest problemQueryRequest) {
        var queryWrapper = new QueryWrapper<Problem>();
        if (problemQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = problemQueryRequest.getId();

        // —— 标签过滤：先查中间表，再要求题目包含全部选中标签 ——
        List<Integer> tagIds = normalizeTagIds(problemQueryRequest.getTags());
        if (CollUtil.isNotEmpty(tagIds)) {
            LambdaQueryWrapper<ProblemTag> tagQuery = new LambdaQueryWrapper<>();
            tagQuery.in(ProblemTag::getTagId, tagIds)
                    .select(ProblemTag::getProblemId, ProblemTag::getTagId);
            List<ProblemTag> relations = problemTagService.list(tagQuery);
            Map<Long, Set<Integer>> problemTagMap = relations.stream()
                    .collect(Collectors.groupingBy(
                            ProblemTag::getProblemId,
                            Collectors.mapping(ProblemTag::getTagId, Collectors.toSet())));
            List<Long> matchedProblemIds = problemTagMap.entrySet().stream()
                    .filter(entry -> entry.getValue().containsAll(tagIds))
                    .map(Map.Entry::getKey)
                    .toList();
            if (matchedProblemIds.isEmpty()) {
                // 无匹配时用不可能存在的 id，使后续 page 结果为空
                queryWrapper.eq("id", -1L);
            } else {
                queryWrapper.in("id", matchedProblemIds);
            }
        }
        Integer level = problemQueryRequest.getLevel();
        String sortField = StringUtils.defaultIfBlank(problemQueryRequest.getSortField(), "id");
        String sortOrder = StringUtils.defaultIfBlank(
                problemQueryRequest.getSortOrder(), CommonConstant.SORT_ORDER_ASC);

        // level == 6 约定为「不限难度」，不拼 level 条件
        queryWrapper.eq(id != null, "id", id)
                .eq(problemQueryRequest.getPublisherId() != null,
                        "user_id", problemQueryRequest.getPublisherId())
                .eq(level != null && level != 6, "level", level);

        // —— 可见性：普通用户只能看公开题 ——
        Long userId = problemQueryRequest.getUserId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id未填写");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!Objects.equals(user.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
            queryWrapper.eq("is_public", 1);
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper.lambda();
    }

    /**
     * 分页查询题目列表并转为列表页 VO。
     *
     * @param problemQueryRequest 筛选、分页与当前用户 id
     * @return Page&lt;ListProblemVO&gt;
     */
    @Override
    public Page<ListProblemVO> listProblemVO(ProblemQueryRequest problemQueryRequest) {
        if (problemQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = Math.max(problemQueryRequest.getCurrent(), 1);
        int pageSize = Math.max(problemQueryRequest.getPageSize(), 1);
        LambdaQueryWrapper<Problem> queryWrapper = getQueryWrapper(problemQueryRequest);
        Page<Problem> problemPage = this.page(new Page<>(current, pageSize), queryWrapper);
        return this.getProblemVOPage(problemPage);
    }

    /**
     * 查询所有未公开题目（仅 id、title），一般供管理侧选用。
     */
    @Override
    public List<ProblemVO> getAllProblemNotPublic() {
        LambdaQueryWrapper<Problem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Problem::getIsPublic, 0);
        List<Problem> problemList = this.list(queryWrapper);
        List<ProblemVO> problemVOList = new ArrayList<>();
        for (Problem problem : problemList) {
            ProblemVO problemVO = new ProblemVO();
            problemVO.setId(problem.getId());
            problemVO.setTitle(problem.getTitle());
            problemVOList.add(problemVO);
        }
        return problemVOList;
    }

    /**
     * 转为详情 VO，但去掉题面 content（减轻列表以外、不需全文场景的体积）。
     *
     * @param problem 题目实体
     * @return content 为 null 的 ProblemVO
     */
    public ProblemVO getProblemVOWithoutContent(Problem problem) {
        ProblemVO problemVO = problem2VO(problem);
        problemVO.setContent(null);
        return problemVO;
    }

    /**
     * 题目实体 → 详情 VO：拷贝字段、补发布者、查标签、解析评测配置。
     *
     * @param problem 题目实体
     * @return 脱敏后的详情 VO
     */
    @Override
    public ProblemVO problem2VO(Problem problem) {
        ThrowUtils.throwIf(problem == null, ErrorCode.PARAMS_ERROR);
        ProblemVO problemVO = new ProblemVO();
        copyProperties(problem, problemVO);
        Long userId = problem.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            if (userVO != null) {
                problemVO.setPublisherId(userVO.getId());
                problemVO.setPublisherName(userVO.getUserName());
            }
        }
        problemVO.setTags(problem.getId() == null
                ? Collections.emptyList()
                : removeNullTags(tagService.listTagsByProblemId(problem.getId())));
        // DB 存 0/1，VO 用 Boolean
        problemVO.setIsPublic(Objects.equals(problem.getIsPublic(), 1));
        problemVO.setJudgeConfig(parseJudgeConfig(problem.getJudgeConfig()));
        return problemVO;
    }

    /**
     * 批量将题目实体转为列表页 VO。
     * <p>
     * 先批量查用户与标签，再组装，避免 N+1 查询。
     *
     * @param problemList 题目实体列表
     * @return 列表页 VO 列表；入参空则返回空列表
     */
    @Override
    public List<ListProblemVO> getProblemVOListByProblemList(List<Problem> problemList) {
        if (CollUtil.isEmpty(problemList)) {
            return Collections.emptyList();
        }
        Set<Long> problemIds = problemList.stream()
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = problemList.stream()
                .map(Problem::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<Tag>> tagsByProblemId = getTagsByProblemIds(problemIds);

        return problemList.stream()
                .map(problem -> problem2ListVO(
                        problem,
                        userMap.get(problem.getUserId()),
                        tagsByProblemId.getOrDefault(problem.getId(), Collections.emptyList())))
                .toList();
    }

    /**
     * 单条实体 → 列表 VO（内部会再查用户与标签；批量场景请用带缓存参数的重载）。
     */
    public ListProblemVO problem2ListVO(Problem problem) {
        User user = problem.getUserId() == null ? null : userService.getById(problem.getUserId());
        List<Tag> tags = problem.getId() == null
                ? Collections.emptyList()
                : tagService.listTagsByProblemId(problem.getId());
        return problem2ListVO(problem, user, removeNullTags(tags));
    }

    /**
     * 使用已查好的用户、标签组装列表 VO，避免循环内重复 IO。
     */
    private ListProblemVO problem2ListVO(Problem problem, User user, List<Tag> tags) {
        ListProblemVO listProblemVO = new ListProblemVO();
        copyProperties(problem, listProblemVO);
        listProblemVO.setTags(tags);
        listProblemVO.setIsPublic(Objects.equals(problem.getIsPublic(), 1));
        listProblemVO.setPublisherId(problem.getUserId());
        if (user != null) {
            listProblemVO.setPublisherName(user.getUserAccount());
        }
        return listProblemVO;
    }

    /**
     * 将分页结果中的 records 从 List&lt;Problem&gt; 转为 List&lt;ListProblemVO&gt;，
     * 保留原分页的 current / size / total。
     *
     * @param problemPage 实体分页
     * @return VO 分页
     */
    public Page<ListProblemVO> getProblemVOPage(Page<Problem> problemPage) {
        List<Problem> problemList = problemPage.getRecords();
        Page<ListProblemVO> problemVOPage = new Page<>(problemPage.getCurrent(), problemPage.getSize(), problemPage.getTotal());
        if (CollUtil.isEmpty(problemList)) {
            return problemVOPage;
        }
        List<ListProblemVO> problemVOList = getProblemVOListByProblemList(problemList);
        problemVOPage.setRecords(problemVOList);
        return problemVOPage;
    }

    /** 校验难度枚举值是否合法 */
    private void checkLevel(Integer level) {
        if (level == null || !ProblemLevel.fromValue(level)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "等级不合法");
        }
    }

    /**
     * 编辑题目：校验 → 更新库 → 替换标签 → 删 Redis 缓存 → 同步 ES。
     *
     * @param request 编辑请求（含 id）
     * @return 更新后的详情 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProblemVO editProblem(ProblemEditRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "题目id不合法");
        Long id = request.getId();
        Problem problem = new Problem();
        copyProperties(request, problem);
        problem.setId(id);
        Boolean isPublic = request.getIsPublic();
        // 兼容 userId / publisherId 两种入参命名
        Long userId = request.getUserId() != null ? request.getUserId() : request.getPublisherId();
        JudgeConfig judgeConfig = request.getJudgeConfig();
        problem.setUserId(userId);
        problem.setIsPublic(isPublic == null ? null : (isPublic ? 1 : 0));
        if (judgeConfig != null) {
            problem.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        this.validProblem(problem, false);
        List<Integer> tags = normalizeAndValidateTagIds(request.getTags());
        boolean result = this.updateById(problem);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        replaceProblemTags(problem.getId(), tags);
        evictProblemCache(problem.getId());
        Problem latest = this.getById(problem.getId());
        ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR, "题目更新失败");
        problemEsService.saveOrUpdate(latest);
        return this.problem2VO(latest);
    }

    /**
     * 新增题目：校验 → 入库 → 写标签关联 → 同步 ES。
     *
     * @param request 创建请求
     * @return 成功返回 true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addProblem(ProblemAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        List<Integer> tags = normalizeAndValidateTagIds(request.getTags());
        JudgeConfig judgeConfig = request.getJudgeConfig();
        Long publisherId = request.getPublisherId();
        Boolean isPublic = request.getIsPublic();
        Problem problem = new Problem();
        copyProperties(request, problem);
        problem.setIsPublic(isPublic == null ? null : (isPublic ? 1 : 0));
        problem.setUserId(publisherId);
        if (judgeConfig != null) {
            problem.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        this.validProblem(problem, true);
        boolean save = this.save(problem);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "题目创建失败");
        saveProblemTags(problem.getId(), tags);
        problemEsService.saveOrUpdate(problem);
        return true;
    }

    /**
     * 批量查询题目 id → 标签列表映射（中间表 + Tag 表二次查询）。
     */
    private Map<Long, List<Tag>> getTagsByProblemIds(Collection<Long> problemIds) {
        if (CollUtil.isEmpty(problemIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<ProblemTag> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.in(ProblemTag::getProblemId, problemIds)
                .select(ProblemTag::getProblemId, ProblemTag::getTagId);
        List<ProblemTag> relations = problemTagService.list(relationQuery);
        if (CollUtil.isEmpty(relations)) {
            return Collections.emptyMap();
        }
        Set<Integer> tagIds = relations.stream()
                .map(ProblemTag::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Tag> tagMap = tagIds.isEmpty()
                ? Collections.emptyMap()
                : tagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<Tag>> result = new HashMap<>();
        for (ProblemTag relation : relations) {
            Tag tag = tagMap.get(relation.getTagId());
            if (tag != null) {
                result.computeIfAbsent(relation.getProblemId(), ignored -> new ArrayList<>()).add(tag);
            }
        }
        return result;
    }

    /** 去 null、去重后的标签 id 列表 */
    private List<Integer> normalizeTagIds(List<Integer> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        return tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 规范化并校验标签：id 合法且全部存在于 Tag 表。
     */
    private List<Integer> normalizeAndValidateTagIds(List<Integer> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        ThrowUtils.throwIf(tagIds.stream().anyMatch(tagId -> tagId == null || tagId <= 0),
                ErrorCode.PARAMS_ERROR, "标签id不合法");
        List<Integer> normalizedTagIds = normalizeTagIds(tagIds);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Tag::getId, normalizedTagIds);
        long existingTagCount = tagService.count(queryWrapper);
        ThrowUtils.throwIf(existingTagCount != normalizedTagIds.size(),
                ErrorCode.PARAMS_ERROR, "标签id不合法");
        return normalizedTagIds;
    }

    /** 先删后插，整体替换某题的标签关联 */
    private void replaceProblemTags(Long problemId, List<Integer> tagIds) {
        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problemId);
        problemTagService.remove(queryWrapper);
        saveProblemTags(problemId, tagIds);
    }

    /** 批量写入题目-标签关联 */
    private void saveProblemTags(Long problemId, List<Integer> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return;
        }
        List<ProblemTag> relations = tagIds.stream().map(tagId -> {
            ProblemTag relation = new ProblemTag();
            relation.setProblemId(problemId);
            relation.setTagId(tagId);
            return relation;
        }).toList();
        ThrowUtils.throwIf(!problemTagService.saveBatch(relations), ErrorCode.OPERATION_ERROR);
    }

    /** 删除题目详情 Redis 缓存 */
    private void evictProblemCache(Long problemId) {
        stringRedisTemplate.delete(RedisConstant.CACHE_PROBLEM_KEY + problemId);
    }

    /** 将库中的 judgeConfig JSON 解析为对象；失败则打日志并返回 null */
    private JudgeConfig parseJudgeConfig(String judgeConfigJson) {
        if (StringUtils.isBlank(judgeConfigJson)) {
            return null;
        }
        try {
            return JSONUtil.toBean(judgeConfigJson, JudgeConfig.class);
        } catch (RuntimeException e) {
            log.warn("题目评测配置反序列化失败", e);
            return null;
        }
    }

    /** 过滤标签列表中的 null 元素 */
    private List<Tag> removeNullTags(List<Tag> tags) {
        if (CollUtil.isEmpty(tags)) {
            return Collections.emptyList();
        }
        return tags.stream().filter(Objects::nonNull).toList();
    }
}
