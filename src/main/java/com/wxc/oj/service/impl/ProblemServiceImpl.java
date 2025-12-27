package com.wxc.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.CommonConstant;
import com.wxc.oj.constant.RedisConstant;
import com.wxc.oj.enums.problem.ProblemLevel;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.exception.ThrowUtils;
import com.wxc.oj.mapper.ProblemMapper;
import com.wxc.oj.model.dto.problem.ProblemAddRequest;
import com.wxc.oj.model.dto.problem.ProblemEditRequest;
import com.wxc.oj.model.dto.problem.ProblemQueryRequest;
import com.wxc.oj.model.dto.problem.ProblemTag;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Tag;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.problem.ListProblemVO;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.springframework.beans.BeanUtils.copyProperties;

/**
* @author 王新超
* @description 针对表【problem】的数据库操作Service实现
* @createDate 2024-02-28 14:24:47
*/
@Service
@Slf4j(topic = "ProblemServiceImpl🏍🏍🏍🏍🏍🏍🏍🏍🏍")
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    @Resource
    private UserService userService;

    @Resource
    private TagService tagService;



    @Resource
    ProblemTagService problemTagService;


    @Resource
    StringRedisTemplate stringRedisTemplate;



    @Override
    public ProblemVO getProblemVOById(Long problemId) {
        if (problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法题目id");
        }
        if (stringRedisTemplate.hasKey(RedisConstant.CACHE_PROBLEM_KEY + problemId)) {
            String s = stringRedisTemplate.opsForValue().get(RedisConstant.CACHE_PROBLEM_KEY + problemId);
            ProblemVO problemVO = JSONUtil.toBean(s, ProblemVO.class);
            return problemVO;
        }
        Problem problem = this.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        ProblemVO problemVOWithContent = this.problem2VO(problem);
        stringRedisTemplate.opsForValue().set(RedisConstant.CACHE_PROBLEM_KEY + problemId, JSONUtil.toJsonStr(problemVOWithContent));
        return problemVOWithContent;
    }



    /**
     * 校验题目是否合法
     * 题目的有些数据创建时可以省略,
     * 等待后期修改
     * @param problem
     * @param add
     */
    @Override
    public void validProblem(Problem problem, boolean add) {

    }

    /**
     * 根据请求的封装对象获取查询包装类
     * 前端会根据题目的标题,内容,难度,标签进行查询
     * @param problemQueryRequest
     * @return
     */
    @Override
    public LambdaQueryWrapper<Problem> getQueryWrapper(ProblemQueryRequest problemQueryRequest) {
        var queryWrapper = new QueryWrapper<Problem>();
        if (problemQueryRequest == null) {
            return queryWrapper.lambda();
        }
        String title = problemQueryRequest.getTitle();

        // 第1次查数据库,根据tags筛选ids
        List<Integer> tags = problemQueryRequest.getTags(); // 获取标签列表
        LambdaQueryWrapper<ProblemTag> queryWrapper1 = new LambdaQueryWrapper<>();
        if (CollUtil.isNotEmpty(tags)) {
//            List<Long> problemIds = tagService.getProblemIdsByTagNames(tags);
            queryWrapper1.in(tags != null && !tags.isEmpty(), ProblemTag::getTagId, tags);
        }
        queryWrapper1.select(ProblemTag::getProblemId);
        List<Long> pids = problemTagService.listObjs(queryWrapper1);
        if (CollUtil.isNotEmpty(pids)) {
            queryWrapper.in("id", pids);
        }
        Integer level = problemQueryRequest.getLevel();
        String sortField = problemQueryRequest.getSortField();
        String sortOrder = problemQueryRequest.getSortOrder();
        if (sortOrder == null) {
            sortOrder = CommonConstant.SORT_ORDER_ASC;
        }
        if (sortField == null) {
            sortField = "id";
        }

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title)
                .like(StringUtils.isNotBlank(title), "content", title)
                .eq(level != null && level != 6,"level", level);

        Long userId = problemQueryRequest.getUserId();
        User byId = userService.getById(userId);
        if (byId.getUserRole() < 1) {
            queryWrapper.eq("is_public", 1);
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper.lambda();
    }




    /**
     * 接受DTO对象, 查询满足请求的所有Problem对象,并封装成VO对象
     * @param problemQueryRequest
     * @return
     */
    @Override
    public Page<ListProblemVO> listProblemVO(ProblemQueryRequest problemQueryRequest) {
        int current = problemQueryRequest.getCurrent();
        int pageSize = problemQueryRequest.getPageSize();
        if (problemQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (problemQueryRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户id未填写");
        }
        // 获取查询条件
        LambdaQueryWrapper<Problem> queryWrapper = getQueryWrapper(problemQueryRequest);
        // 查询
        Page<Problem> problemPage = this.page(new Page<>(current, pageSize), queryWrapper);
        Page<ListProblemVO> problemVOPage = this.getProblemVOPage(problemPage);
        // 返回
        return problemVOPage;
    }



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
     * 生成要返回给前端的VO对象
     * 进行了数据脱敏
     * 题目对应的用户信息和标签信息需要再次查询
     * @param problem
     * @return
     */
    public ProblemVO getProblemVOWithoutContent(Problem problem) {
        // 将entity转为vo
        ProblemVO problemVO = new ProblemVO();
        copyProperties(problem, problemVO);
        problemVO.setContent(null);
        // 补充vo的信息
        Long userId = problem.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problem.getId());

        List<Tag> tags = new ArrayList<>();
        List<ProblemTag> list = problemTagService.list(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            tags = tagService.listTagsByProblemId(problem.getId());
        }
        problemVO.setJudgeConfig(JSONUtil.toBean(problem.getJudgeConfig(), JudgeConfig.class));
        problemVO.setTags(tags);
        problemVO.setPublisherId(userVO.getId());
        problemVO.setPublisherName(userVO.getUserName());
        problemVO.setIsPublic(problem.getIsPublic() == 1? true:  false);

        return problemVO;
    }
    /**
     * 生成要返回给前端的VO对象
     * 进行了数据脱敏
     * 题目对应的用户信息和标签信息需要再次查询
     * @param problem
     * @return
     */
    @Override
    public ProblemVO problem2VO(Problem problem) {
        // 将entity转为vo
        ProblemVO problemVO = new ProblemVO();
        copyProperties(problem, problemVO);
        // 补充vo的信息
        Long userId = problem.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problem.getId());
        List<Tag> tags = new ArrayList<>();
        List<ProblemTag> list = problemTagService.list(queryWrapper);
        if (list  != null && !list.isEmpty()) {
            tags = tagService.listTagsByProblemId(problem.getId());
        }
        problemVO.setTags(tags);
        problemVO.setPublisherId(userVO.getId());
        problemVO.setPublisherName(userVO.getUserName());
        problemVO.setIsPublic(problem.getIsPublic() == 1 ?  true : false);
        problemVO.setJudgeConfig(JSONUtil.toBean(problem.getJudgeConfig(), JudgeConfig.class));
        return problemVO;
    }

    /**
     * 生成要返回给前端的VO对象
     * 进行了数据脱敏
     */
    @Override
    public List<ListProblemVO> getProblemVOListByProblemList(List<Problem> problemList) {
        List<ListProblemVO> problemVOList = new ArrayList<>();
        for (Problem problem : problemList) {
            ListProblemVO listProblemVO = problem2ListVO(problem);
            problemVOList.add(listProblemVO);
        }
        return problemVOList;
    }


    public ListProblemVO problem2ListVO (Problem problem) {
        ListProblemVO listProblemVO = new ListProblemVO();
        copyProperties(problem, listProblemVO);
        List<Tag> tags = tagService.listTagsByProblemId(problem.getId());
        listProblemVO.setTags(tags);
        listProblemVO.setIsPublic(problem.getIsPublic() == 1);
        listProblemVO.setPublisherId(problem.getUserId());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, problem.getUserId())
                         .select(User::getUserAccount);
        User user = userService.getOne(queryWrapper);
        listProblemVO.setPublisherName(user.getUserAccount());
        return  listProblemVO;
    }

    /**
     * 生成分页的VO对象
     * 主要是修改Page对象的records属性
     * records属性就是 List<Problem>
     * 将Page的records属性从List<Problem>修改为List<ProblemVO>
     * @param problemPage
     * @return
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



    private void checkLevel(Integer level) {
        Boolean isLevel = ProblemLevel.fromValue(level);
        if (!isLevel) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "等级不合法");
        }
    }


    /**
     * 更新题目后删除Redis中的数据
     * @param request
     * @return
     */
    @Override
    public ProblemVO editProblem(ProblemEditRequest request) {
        Long id = request.getId();
        Problem problem = new Problem();
        copyProperties(request, problem);
        problem.setId(id);
        Boolean isPublic = request.getIsPublic();
        Long userId = request.getUserId();
        Integer level = request.getLevel();
        this.checkLevel(level);
        JudgeConfig judgeConfig = request.getJudgeConfig();


        problem.setUserId(userId);
        if (isPublic) {
            problem.setIsPublic(1);
        } else {
            problem.setIsPublic(0);
        }
        problem.setLevel(level);

        if (judgeConfig != null) {
            problem.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }

        this.validProblem(problem, true);

        boolean result = this.updateById(problem);
        // 添加失败
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);


        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problem.getId());
        problemTagService.remove(queryWrapper);
        int tagSize = tagService.list().size();
        List<Integer> tags1 = request.getTags();
        Set<Integer> set = new HashSet<>(tags1);

        List<Integer> tags = new ArrayList<>(set);
        for (Integer tagId : tags) {
            if (tagId <= 0 || tagId > tagSize) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签id不合法");
            }
            ProblemTag problemTag = new ProblemTag();
            problemTag.setProblemId(problem.getId());
            problemTag.setTagId(tagId);
            boolean save = problemTagService.save(problemTag);
            if (!save) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }


        }

        stringRedisTemplate.delete(RedisConstant.CACHE_PROBLEM_KEY + problem.getId());
        ProblemVO problemVOWithContent = this.problem2VO(problem);
        return problemVOWithContent;
    }





    @Override
    public Boolean addProblem(ProblemAddRequest request) {
        List<Integer> tags = request.getTags();
        JudgeConfig judgeConfig = request.getJudgeConfig();
        Long publisherId = request.getPublisherId();
        Boolean isPublic = request.getIsPublic();
        Problem problem = new Problem();
        copyProperties(request, problem);
        problem.setIsPublic(isPublic ? 1 : 0);
        problem.setUserId(publisherId);
        problem.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        boolean save = this.save(problem);
        Long problemId = problem.getId();
        for (Integer tag : tags) {
            ProblemTag problemTag = new ProblemTag();
            problemTag.setProblemId(problemId);
            problemTag.setTagId(tag);
            problemTagService.save(problemTag);
        }
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目创建失败");
        }
        return true;
    }
}




