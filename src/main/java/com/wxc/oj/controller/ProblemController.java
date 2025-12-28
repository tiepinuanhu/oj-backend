package com.wxc.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.annotation.AuthCheck;
import com.wxc.oj.common.BaseResponse;
import com.wxc.oj.common.DeleteRequest;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.common.ResultUtils;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.exception.ThrowUtils;
import com.wxc.oj.model.req.problem.*;
import com.wxc.oj.model.po.ContestProblem;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.model.vo.problem.ProblemVO;
import com.wxc.oj.model.vo.contest.ContestProblemSimpleVO;
import com.wxc.oj.service.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.wxc.oj.enums.UserRoleEnum.ADMIN;
import static org.springframework.beans.BeanUtils.copyProperties;
import static org.springframework.beans.BeanUtils.findPropertyForMethod;

/**
 * 题目
 */
@RestController
@RequestMapping("problem")
@Slf4j(topic = "ProblemController🛴🛴🛴🛴🛴🛴")
public class ProblemController {

    @Resource
    private ProblemService problemService;

    @Resource
    private UserService userService;

    @Resource
    ProblemTagService problemTagService;


    @Resource
    ContestProblemService contestProblemService;

    @Resource
    TagService tagService;



    private static final String UPLOAD_ROOT = "src/main/resources/data";

    /**
     * 实现了接收一个文件到服务端
     * todo:
     *  接收一组输入输出样例, 保存到resouces/data/{pid}
     *
     * @param files
     * @throws Exception
     */

    @PostMapping("uploadCase")
    public void getCaseLoad(@RequestParam("file") List<MultipartFile> files, @RequestParam Long pid)
            throws Exception {
        for (MultipartFile file : files) {
            // 1. 验证文件是否为空
            if (file.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
            }

            // 2. 构建基于PID的存储目录（例如：resources/data/123）
            Path pidDirectory = Paths.get(UPLOAD_ROOT, String.valueOf(pid));

            // 3. 确保目录存在，不存在则创建（包括父目录）
            Files.createDirectories(pidDirectory);

            // 4. 获取原始文件名并构建完整存储路径
            String fileName = file.getOriginalFilename();
            Path targetLocation = pidDirectory.resolve(fileName);

            // 5. 保存文件到目标位置（使用REPLACE_EXISTING避免文件已存在错误）
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }
    }

//    @GetMapping("notPublished")
//    @AuthCheck(mustRole = ADMIN)
//    public BaseResponse<List<AddingProblemVO>> getProblemVOForContest() {
//        LambdaQueryWrapper<Problem> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Problem::getIsPublic, 0)
//                .select(Problem::getId, Problem::getTitle);
//        List<Problem> problemList = problemService.list(queryWrapper);
//        if (problemList == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
//        }
//        List<AddingProblemVO> resultList = problemList.stream()
//                .map(problem -> {
//                    AddingProblemVO vo = new AddingProblemVO();
//                    vo.setId(problem.getId());
//                    vo.setTitle(problem.getTitle());
//                    return vo;
//                })
//                .collect(Collectors.toList());
//        return ResultUtils.success(resultList);
//    }
    @PostMapping("edit")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse<ProblemVO> editProblem(@RequestBody ProblemEditRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求为null");
        }
        ProblemVO problemVO = problemService.editProblem(request);
        return ResultUtils.success(problemVO);
    }

//    @PutMapping("create")
//    @AuthCheck(mustRole = ADMIN)
//    public BaseResponse<Long> createProblem(@RequestParam Long userId) {
//        Problem problem = new Problem();
//        problem.setTitle("temp");
//        problem.setContent("");
//        problem.setLevel(0);
//        problem.setJudgeConfig(JSONUtil.toJsonStr(new JudgeConfig()));
//        problem.setUserId(userId);
//        problem.setIsPublic(0);
//        boolean save = problemService.save(problem);
//        return ResultUtils.success(problem.getId());
//    }

    /**
     * 添加题目（基本信息，不包含样例）
     * @param request
     * @return
     */
    @PostMapping("add")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse<Boolean> addProblem(@RequestBody
                                                ProblemAddRequest request) {
        Boolean b = problemService.addProblem(request);
        return ResultUtils.success(b);
    }



    @GetMapping("get/notPublic")
    public BaseResponse getAllNotPublicProblem() {
        List<ProblemVO> allProblemNotPublic = problemService.getAllProblemNotPublic();
        return ResultUtils.success(allProblemNotPublic);
    }
//    /**
//     * 创建比赛使用题目
//     */
//    @PostMapping("addtocontest")
//    @AuthCheck(mustRole = ADMIN)
//    public BaseResponse<Problem> addProblemToContest(@RequestBody ProblemAddRequest problemAddRequest,
//                                                     HttpServletRequest request) {
//
//        if (problemAddRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Problem problem = new Problem();
//        copyProperties(problemAddRequest, problem);
//        List<Integer> tags = problemAddRequest.getTags();
////        if (tags != null) {
////            problem.setTags(JSONUtil.toJsonStr(tags));
////        }
////        List<JudgeCase> judgeCase = problemAddRequest.getJudgeCase();
////        if (judgeCase != null) {
////            problem.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
////        }
//        JudgeConfig judgeConfig = problemAddRequest.getJudgeConfig();
//        if (judgeConfig != null) {
//            problem.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
//        }
//        problemService.validProblem(problem, true);
//        // 获取当前用户
//        User loginUser = userService.getLoginUser(request);
//        // 初始化题目信息
//        problem.setUserId(loginUser.getId());
//        problem.setSubmittedNum(0);
//        problem.setAcceptedNum(0);
//        problem.setIsPublic(0); // 用于比赛的题目, 比赛结束前所有人不可见
//        // 保存答案
//        boolean result = problemService.save(problem);
//        // 添加失败
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        long newProblemId = problem.getId();
//
//        Problem newProblem = problemService.getById(newProblemId);
//        return ResultUtils.success(newProblem);
//    }
    /**
     * 删除题目(逻辑删除)
     */
    @PostMapping("delete")
    public BaseResponse deleteProblem(@RequestBody DeleteRequest deleteRequest,
                                      HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        // 判断是否存在
        Problem oldProblem = problemService.getById(id);
        ThrowUtils.throwIf(oldProblem == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldProblem.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = problemService.removeById(id);
        return ResultUtils.success(b);
    }



    /**
     * 根据 id 获取题目
     * GET方法
     * 使用redis 缓存
     * 更新题目后，可以使用的还是Redis缓存
     */
    @GetMapping("/get/vo")
    public BaseResponse<ProblemVO> getProblemVOById(@RequestParam Long id) {
        ProblemVO problemVOById = problemService.getProblemVOById(id);
        return ResultUtils.success(problemVOById);
    }

    @GetMapping("/get/check")
    public BaseResponse<ContestProblemSimpleVO> checkProblemCanUsedInContest(
            @RequestParam Long contestId, @RequestParam Long problemId) {
        if (problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Problem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Problem::getId, problemId)
                .eq(Problem::getIsPublic, 0);
        Problem problem = problemService.getOne(queryWrapper);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "改题目不是私有");
        }
        User publisher = userService.getById(problem.getUserId());
        LambdaQueryWrapper<ContestProblem> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(ContestProblem::getProblemId, problemId)
                .eq(ContestProblem::getContestId, contestId);
        ContestProblemSimpleVO contestProblemSimpleVO = new ContestProblemSimpleVO();
        contestProblemSimpleVO.setProblemId(problemId);
        contestProblemSimpleVO.setProblemIndex(0);
        contestProblemSimpleVO.setFullScore(100);
        contestProblemSimpleVO.setTitle(problem.getTitle());
        contestProblemSimpleVO.setPublisherName(publisher.getUserName());
        contestProblemSimpleVO.setPublisherId(publisher.getId());
        contestProblemSimpleVO.setCreateTime(problem.getCreateTime());
        contestProblemSimpleVO.setIsPublic(problem.getIsPublic());
        return ResultUtils.success(contestProblemSimpleVO);
    }



    /**
     * 根据 id 获取题目
     * GET方法 不脱敏
     */
    @GetMapping("/get")
    public BaseResponse<Problem> getProblemById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Problem problem = problemService.getById(id);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (!problem.getId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能查看其它用户的题目的全部信息");
        }
        return ResultUtils.success(problem);
    }


    /**
     * 分页获取列表（封装类）
     * 展示用户可见的部分(普通用户使用)
     * @param
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse listProblemVOByPage(@RequestBody ProblemQueryRequest problemQueryRequest) {
        // 限制爬虫
        long size = problemQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<ListProblemVO> listProblemVOPage = problemService.listProblemVO(problemQueryRequest);
        return ResultUtils.success(listProblemVOPage);
    }
}
