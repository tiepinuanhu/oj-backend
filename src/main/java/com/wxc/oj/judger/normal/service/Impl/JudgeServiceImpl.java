package com.wxc.oj.judger.normal.service.Impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.LanguageConfigs;
import com.wxc.oj.constant.RabbitMQConstant;
import com.wxc.oj.enums.JudgeResultEnum;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import com.wxc.oj.enums.submission.SubmissionStatusEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.judger.model.TestCase;
import com.wxc.oj.judger.model.TestCases;
import com.wxc.oj.judger.normal.service.JudgeService;
import com.wxc.oj.model.queueMessage.ProblemMessage;
import com.wxc.oj.model.queueMessage.SubmissionMessage;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.judge.JudgeCaseResult;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.submission.SubmissionResult;
import com.wxc.oj.openFeign.SandboxFeignClient;
import com.wxc.oj.model.req.sandbox.Cmd;
import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.SandBoxRequest;
import com.wxc.oj.enums.sandbox.SandBoxResponseStatus;
import com.wxc.oj.model.req.sandbox.LanguageConfig;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.SubmissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * todo:
 *  一个样例是用一个字符串还是一组字符串呢???
 *  {
 *   "problemId": 1763440748296044545,
 *   "sourceCode": "",
 *   "language": "cpp"
 * }
 */
//@Service
@Slf4j(topic = "✔✔✔✔JudgeServiceImpl✔✔✔✔")
public class JudgeServiceImpl implements JudgeService {


    @Resource
    private SandboxFeignClient sandboxFeignClient;



    @Resource
    private SubmissionService submissionService;

    @Resource
    private ProblemService problemService;

    @Resource
    private RabbitTemplate rabbitTemplate;



    private static final String EXCHANGE = "problem_exchange";

    private static final String ROUTING_KEY = "problem_key";


    /**
     * 每个样例时间限制1s
     */
    public static final Long CPU_LIMIT = 1_000_000_000L;
    public static final Long COMPILE_CPU_LIMIT = 3_000_000_000L;
    /**
     * 内存限制512MB
     */
    public static final Long MEMORY_LIMIT = 536870912L;

//    public static final String QUEUE = "submission";
//    public static final String DATA_PATH = System.getProperty("user.home") + "/oj-data";
    public static final String DATA_PATH = "C:\\Users\\WangXinchao\\Desktop\\oj-backend\\src\\main\\resources\\data";
    public static final Integer PROC_LIMIT = 50;
    @Value("${oj.data.path}")
    protected String dataPath;

    @RabbitListener(queues = RabbitMQConstant.SUBMISSION_QUEUE, messageConverter = "jacksonConverter")
    public void listenSubmission(SubmissionMessage message) throws IOException {
        Long id = message.getId();
        log.info("🔆🔆🔆🔆🔆接收到的id: " + id);
        doJudge(id);
    }






    private boolean changeStatus(Submission submissionUpd,
                                 SubmissionResult submissionResult, SubmissionStatusEnum statusUpd) {
        submissionResult.setStatus(statusUpd.getStatus());
        submissionResult.setStatusDescription(statusUpd.getDescription());
        submissionUpd.setStatus(statusUpd.getStatus());
        submissionUpd.setStatusDescription(statusUpd.getDescription());
        submissionUpd.setSubmissionResult(JSONUtil.toJsonStr(submissionResult));
        return submissionService.updateById(submissionUpd);
    }
    /**
     * 删除为了比对生成的临时文件index.ans
     * @param pid
     * @param index
     * @return
     */
    public boolean deleteDotAnsFile(Long pid, int index) {
        String filePath = DATA_PATH + File.separator + pid + File.separator + index + ".ans";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }

    /**
     * cppJudge
     * 1.
     * @param submission
     * @param problem
     * @throws IOException
     */
    public void cppJudge(Submission submission, Problem problem) throws IOException {
        Long pid = problem.getId();

        Long submissionId = submission.getId();
        // 更新数据库中的submission的status字段 COMPILING, 以便前端即时查看到submission的状态

        // 封装传入代码沙箱的请求
        String sourceCode = submission.getSourceCode();

        SubmissionResult submissionResult = new SubmissionResult();
        this.changeStatus(submission, submissionResult, SubmissionStatusEnum.COMPILING);


//        String exeId = compileCppFile(sourceCode);
        Result compileResult = compileCode(sourceCode, LanguageConfigs.CPP);
        // 获取返回得文件id
        if (!compileResult.getStatus().equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            // 返回编译错误
            submissionResult.setTotalTime(0L);
            submissionResult.setMemoryUsed(0L);
            submissionResult.setScore(0);
            String stderr = compileResult.getFiles().getStderr();
            submissionResult.setCompileErrorMessage(stderr);
            boolean b = this.changeStatus(submission, submissionResult,
                    SubmissionStatusEnum.COMPILE_ERROR);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "submission更新失败");
            }
            return;
        }
        Map<String, String> fileIds = compileResult.getFileIds();
        String exeId = fileIds.get("main");
        // 编译成功，修改状态为JUDGING
        this.changeStatus(submission, submissionResult, SubmissionStatusEnum.JUDGING);

        List<JudgeCaseResult> judgeCaseResults = new ArrayList<>();
        // 读取判题配置
        String judgeConfigStr = problem.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        // 统计时间和内存使用
        Long totalTime = 0L;
        Long maxMemoryUsed = 0L;
        // 3.17
        // 将config.json转为TestCases
        String filePath = DATA_PATH + File.separator + pid + File.separator + "config.json";
        String jsonStr = FileUtil.readUtf8String(filePath);
        TestCases testCases = JSONUtil.toBean(jsonStr, TestCases.class);
        List<TestCase> testCaseList = testCases.getCases();

        // 计算得分
        int totalScore = 0;
        for (TestCase testCase : testCaseList) {
            // 获取第index个测试样例的输入文件, 并转化为字符串
            int index = testCase.getIndex();

            String inputFile = DATA_PATH +File.separator + pid + File.separator + index + ".in";
            System.out.println("📍📍📍inputFile = " + inputFile);
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            String input = content.toString();
            System.out.println("🚛🚛🚛🚛🚛🚛🚛🚛input = " + input);

            // 运行第index个测试样例
            Result runResult = runCode(exeId, input, LanguageConfigs.CPP);
            String status = runResult.getStatus();

            JudgeCaseResult judgeCaseResult = new JudgeCaseResult();
            judgeCaseResult.setIndex(index);
            judgeCaseResult.setInput(input);
            judgeCaseResult.setFullScore(testCase.getFullScore());
            // ns => ms
            Long timeCost = runResult.getRunTime() / 1000_000;
            Long memoryUsed = runResult.getMemory();

            totalTime += timeCost;
            maxMemoryUsed = Math.max(maxMemoryUsed, memoryUsed);

            judgeCaseResult.setMemoryUsed(memoryUsed);
            judgeCaseResult.setTimeCost(timeCost);

            // 执行成功
            if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
                // 获取输出文件.ans
                String output = runResult.getFiles().getStdout();
                FileWriter fileWriter = new FileWriter(DATA_PATH + File.separator + pid + File.separator + index + ".ans");
                fileWriter.write(output);
                fileWriter.flush();
                judgeCaseResult.setOutput(output);
                // 比较.ans和.out文件
                boolean accepted = checker(pid, index);
                // 删除临时用于比对的.ans文件
                deleteDotAnsFile(pid, index);
                // 根据.out和.ans文件的比对结果, 更新judgeCaseResult
                if (accepted) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
                    judgeCaseResult.setGainScore(testCase.getFullScore());
                } else {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                    judgeCaseResult.setGainScore(0);

                }

                // 判断超时
                if (timeCost > judgeConfig.getTimeLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
                // 判断超内存
                if (memoryUsed / 1024 / 1024 > judgeConfig.getMemoryLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
            } else if (status.equals(SandBoxResponseStatus.TIME_LIMIT_EXCEEDED.getValue())){
                judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else if (status.equals(SandBoxResponseStatus.MEMORY_LIMIT_EXCEEDED.getValue())) {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                judgeCaseResult.setGainScore(0);
            }
            totalScore += judgeCaseResult.getGainScore();

            // 返回该样例的标准答案
            String stdoutFilePath = DATA_PATH + File.separator + pid + File.separator + index + ".out";
            String line;
            StringBuilder ansFile = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(stdoutFilePath));
            while ((line = reader.readLine()) != null) {
                ansFile.append(line).append("\n");
            }
            String ansFileString = ansFile.toString();
            judgeCaseResult.setAns(ansFileString);

            judgeCaseResults.add(judgeCaseResult);
        }


        // 运行完样例后, 每组样例的运行时间, 内存占用都保存在judgeCaseResult
        // 样例输出文件, 保存在data/xxx/1.ans中
        // 比对data/xxx/中 1.out和1.ans的内容
        // 设置程序的总运行
        submissionResult.setMemoryUsed(maxMemoryUsed);
        submissionResult.setTotalTime(totalTime);

        // 根据AC样例数与总样例数, 计算分数

        // 删除沙箱服务中保存的文件
        if (exeId != null) {
//            sandboxRun.delFile(exeId);
            sandboxFeignClient.deleteFile(exeId);
        }

        submissionResult.setScore(totalScore);
        // 提交结果中包含所有测试样例的测试结果
        submissionResult.setJudgeCaseResults(judgeCaseResults);
        // 判题结束后, 修改数据库中的submission的信息
        submission.setId(submissionId);
        if (totalScore == 100) {
            this.changeStatus(submission, submissionResult, SubmissionStatusEnum.ACCEPTED);
        } else {
            for (JudgeCaseResult judgeCaseResult : judgeCaseResults) {
                if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.TIME_LIMIT_EXCEEDED);
                    break;
                } else if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.WRONG_ANSWER.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.WRONG_ANSWER);
                    break;
                }
            }
        }
        //❗❗❗❗❗❗❗❗❗❗ 给normalproblem队列发送消息，修改题目的统计数据 ❗❗❗❗❗❗❗
        ProblemMessage problemMessage = new ProblemMessage();
        problemMessage.setSid(submissionId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, problemMessage);
    }

    public void javaJudge(Submission submission, Problem problem) throws IOException {
        Long pid = problem.getId();

        Long submissionId = submission.getId();
        // 更新数据库中的submission的status字段 COMPILING, 以便前端即时查看到submission的状态

        // 封装传入代码沙箱的请求
        String sourceCode = submission.getSourceCode();

        SubmissionResult submissionResult = new SubmissionResult();
        this.changeStatus(submission, submissionResult, SubmissionStatusEnum.COMPILING);


//        String exeId = compileCppFile(sourceCode);
        Result compileResult = compileCode(sourceCode, LanguageConfigs.JAVA);
        // 获取返回得文件id
        if (!compileResult.getStatus().equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            // 返回编译错误
            submissionResult.setTotalTime(0L);
            submissionResult.setMemoryUsed(0L);
            submissionResult.setScore(0);
            String stderr = compileResult.getFiles().getStderr();
            submissionResult.setCompileErrorMessage(stderr);
            boolean b = this.changeStatus(submission, submissionResult,
                    SubmissionStatusEnum.COMPILE_ERROR);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "submission更新失败");
            }
            return;
        }
        Map<String, String> fileIds = compileResult.getFileIds();
        String exeId = fileIds.get("Main.class");
        // 编译成功，修改状态为JUDGING
        this.changeStatus(submission, submissionResult, SubmissionStatusEnum.JUDGING);

        List<JudgeCaseResult> judgeCaseResults = new ArrayList<>();
        // 读取判题配置
        String judgeConfigStr = problem.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        // 统计时间和内存使用
        Long totalTime = 0L;
        Long maxMemoryUsed = 0L;
        // 3.17
        // 将config.json转为TestCases
        String filePath = DATA_PATH + File.separator + pid + File.separator + "config.json";
        String jsonStr = FileUtil.readUtf8String(filePath);
        TestCases testCases = JSONUtil.toBean(jsonStr, TestCases.class);
        List<TestCase> testCaseList = testCases.getCases();

        // 计算得分
        int totalScore = 0;
        for (TestCase testCase : testCaseList) {
            // 获取第index个测试样例的输入文件, 并转化为字符串
            int index = testCase.getIndex();

            String inputFile = DATA_PATH +File.separator + pid + File.separator + index + ".in";
            System.out.println("📍📍📍inputFile = " + inputFile);
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            String input = content.toString();
            System.out.println("🚛🚛🚛🚛🚛🚛🚛🚛input = " + input);

            // 运行第index个测试样例
            Result runResult = runCode(exeId, input, LanguageConfigs.CPP);
            String status = runResult.getStatus();

            JudgeCaseResult judgeCaseResult = new JudgeCaseResult();
            judgeCaseResult.setIndex(index);
            judgeCaseResult.setInput(input);
            judgeCaseResult.setFullScore(testCase.getFullScore());
            // ns => ms
            Long timeCost = runResult.getRunTime() / 1000_000;
            Long memoryUsed = runResult.getMemory();

            totalTime += timeCost;
            maxMemoryUsed = Math.max(maxMemoryUsed, memoryUsed);

            judgeCaseResult.setMemoryUsed(memoryUsed);
            judgeCaseResult.setTimeCost(timeCost);

            // 执行成功
            if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
                // 获取输出文件.ans
                String output = runResult.getFiles().getStdout();
                FileWriter fileWriter = new FileWriter(DATA_PATH + File.separator + pid + File.separator + index + ".ans");
                fileWriter.write(output);
                fileWriter.flush();
                judgeCaseResult.setOutput(output);
                // 比较.ans和.out文件
                boolean accepted = checker(pid, index);
                // 删除临时用于比对的.ans文件
                deleteDotAnsFile(pid, index);
                // 根据.out和.ans文件的比对结果, 更新judgeCaseResult
                if (accepted) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
                    judgeCaseResult.setGainScore(testCase.getFullScore());
                } else {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                    judgeCaseResult.setGainScore(0);

                }

                // 判断超时
                if (timeCost > judgeConfig.getTimeLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
                // 判断超内存
                if (memoryUsed / 1024 / 1024 > judgeConfig.getMemoryLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
            } else if (status.equals(SandBoxResponseStatus.TIME_LIMIT_EXCEEDED.getValue())){
                judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else if (status.equals(SandBoxResponseStatus.MEMORY_LIMIT_EXCEEDED.getValue())) {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                judgeCaseResult.setGainScore(0);
            }
            totalScore += judgeCaseResult.getGainScore();

            // 返回该样例的标准答案
            String stdoutFilePath = DATA_PATH + File.separator + pid + File.separator + index + ".out";
            String line;
            StringBuilder ansFile = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(stdoutFilePath));
            while ((line = reader.readLine()) != null) {
                ansFile.append(line).append("\n");
            }
            String ansFileString = ansFile.toString();
            judgeCaseResult.setAns(ansFileString);

            judgeCaseResults.add(judgeCaseResult);
        }


        // 运行完样例后, 每组样例的运行时间, 内存占用都保存在judgeCaseResult
        // 样例输出文件, 保存在data/xxx/1.ans中
        // 比对data/xxx/中 1.out和1.ans的内容
        // 设置程序的总运行
        submissionResult.setMemoryUsed(maxMemoryUsed);
        submissionResult.setTotalTime(totalTime);

        // 根据AC样例数与总样例数, 计算分数

        // 删除沙箱服务中保存的文件
        if (exeId != null) {
//            sandboxRun.delFile(exeId);
            sandboxFeignClient.deleteFile(exeId);
        }

        submissionResult.setScore(totalScore);
        // 提交结果中包含所有测试样例的测试结果
        submissionResult.setJudgeCaseResults(judgeCaseResults);
        // 判题结束后, 修改数据库中的submission的信息
        submission.setId(submissionId);
        if (totalScore == 100) {
            this.changeStatus(submission, submissionResult, SubmissionStatusEnum.ACCEPTED);
        } else {
            for (JudgeCaseResult judgeCaseResult : judgeCaseResults) {
                if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.TIME_LIMIT_EXCEEDED);
                    break;
                } else if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.WRONG_ANSWER.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.WRONG_ANSWER);
                    break;
                }
            }
        }
        //❗❗❗❗❗❗❗❗❗❗ 给normalproblem队列发送消息，修改题目的统计数据 ❗❗❗❗❗❗❗
        ProblemMessage problemMessage = new ProblemMessage();
        problemMessage.setSid(submissionId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, problemMessage);
    }
    public boolean checker(Long pid, int index) throws IOException {
        String stdoutFilePath = DATA_PATH + File.separator + pid + File.separator + index + ".out";
        String useroutFilePath = DATA_PATH + File.separator + pid + File.separator + index + ".ans";
        BufferedReader br1 = new BufferedReader(new FileReader(stdoutFilePath));
        BufferedReader br2 = new BufferedReader(new FileReader(useroutFilePath));
        String line1, line2;
        int lineNumber = 0;
        while ((line1 = br1.readLine()) != null && (line2 = br2.readLine()) != null) {
            lineNumber++;
            // 移除行首和行尾空格
            line1 = line1.trim();
            line2 = line2.trim();

            if (!line1.equals(line2)) {
                return false;
            }
        }
        // 检查是否有剩余的行
        if (br1.readLine() != null || br2.readLine() != null) {
            return false;
        }
        return true;

    }
    /**
     * @param submissionId
     * @return 返回每个测试用例的判题信息
     * 根据不同得语言选择不同得判题逻辑
     */
    @Override
    public void doJudge(Long submissionId) throws IOException {
        Submission submission = submissionService.getById(submissionId);
        // 获取提交
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交不存在");
        }
        // 获取题目信息
        Long problemId = submission.getProblemId();
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 获取语言信息, todo: 后续会根据语言执行不同参数的运行
        String language = submission.getLanguage();



        if (language.equals(SubmissionLanguageEnum.CPP.getValue())) {
            cppJudge(submission, problem);
        } else if (language.equals(SubmissionLanguageEnum.PYTHON.getValue())) {
            pythonJudge(submission, problem);
        } else if (language.equals(SubmissionLanguageEnum.JAVA.getValue())) {
            javaJudge(submission, problem);
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不支持的语言");
        }
        SubmissionResult submissionResult = new SubmissionResult();
        submissionResult.setStatusDescription("编程语言不支持");
    }

    @Override
    public void pythonJudge(Submission submission, Problem problem) throws IOException {
        Long pid = problem.getId();

        Long submissionId = submission.getId();
        // 更新数据库中的submission的status字段 COMPILING, 以便前端即时查看到submission的状态

        SubmissionResult submissionResult = new SubmissionResult();

        List<JudgeCaseResult> judgeCaseResults = new ArrayList<>();
        // 读取判题配置
        String judgeConfigStr = problem.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        // 统计时间和内存使用
        Long totalTime = 0L;
        Long maxMemoryUsed = 0L;
        // 3.17
        // 将config.json转为TestCases
        String filePath = DATA_PATH + File.separator + pid + File.separator + "config.json";
        String jsonStr = FileUtil.readUtf8String(filePath);
        TestCases testCases = JSONUtil.toBean(jsonStr, TestCases.class);
        List<TestCase> testCaseList = testCases.getCases();

        // 计算得分
        int totalScore = 0;
//        String fileId = "main.py";
        for (TestCase testCase : testCaseList) {
            // 获取第index个测试样例的输入文件, 并转化为字符串
            int index = testCase.getIndex();

            String inputFile = DATA_PATH +File.separator + pid + File.separator + index + ".in";
            System.out.println("📍📍📍inputFile = " + inputFile);
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            String input = content.toString();
            System.out.println("🚛🚛🚛🚛🚛🚛🚛🚛input = " + input);

            // 运行第index个测试样例
            Result runResult = runPython(submission.getSourceCode(), input, LanguageConfigs.PYTHON);

            String status = runResult.getStatus();

            JudgeCaseResult judgeCaseResult = new JudgeCaseResult();
            judgeCaseResult.setIndex(index);
            judgeCaseResult.setInput(input);
            judgeCaseResult.setFullScore(testCase.getFullScore());
            // ns => ms
            Long timeCost = runResult.getRunTime() / 1000_000;
            Long memoryUsed = runResult.getMemory();

            totalTime += timeCost;
            maxMemoryUsed = Math.max(maxMemoryUsed, memoryUsed);

            judgeCaseResult.setMemoryUsed(memoryUsed);
            judgeCaseResult.setTimeCost(timeCost);

            // 执行成功
            if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
                // 获取输出文件.ans
                String output = runResult.getFiles().getStdout();
                FileWriter fileWriter = new FileWriter(DATA_PATH + File.separator + pid + File.separator + index + ".ans");
                fileWriter.write(output);
                fileWriter.flush();
                judgeCaseResult.setOutput(output);
                // 比较.ans和.out文件
                boolean accepted = checker(pid, index);
                // 删除临时用于比对的.ans文件
                deleteDotAnsFile(pid, index);
                // 根据.out和.ans文件的比对结果, 更新judgeCaseResult
                if (accepted) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
                    judgeCaseResult.setGainScore(testCase.getFullScore());
                } else {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                    judgeCaseResult.setGainScore(0);

                }

                // 判断超时
                if (timeCost > judgeConfig.getTimeLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
                // 判断超内存
                if (memoryUsed / 1024 / 1024 > judgeConfig.getMemoryLimit()) {
                    judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    judgeCaseResult.setGainScore(0);
                }
            } else if (status.equals(SandBoxResponseStatus.TIME_LIMIT_EXCEEDED.getValue())){
                judgeCaseResult.setJudgeResult(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else if (status.equals(SandBoxResponseStatus.MEMORY_LIMIT_EXCEEDED.getValue())) {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                judgeCaseResult.setGainScore(0);
            } else {
                judgeCaseResult.setJudgeResult(JudgeResultEnum.WRONG_ANSWER.getValue());
                judgeCaseResult.setGainScore(0);
            }
            totalScore += judgeCaseResult.getGainScore();

            // 返回该样例的标准答案
            String stdoutFilePath = DATA_PATH + File.separator + pid + File.separator + index + ".out";
            String line;
            StringBuilder ansFile = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(stdoutFilePath));
            while ((line = reader.readLine()) != null) {
                ansFile.append(line).append("\n");
            }
            String ansFileString = ansFile.toString();
            judgeCaseResult.setAns(ansFileString);

            judgeCaseResults.add(judgeCaseResult);
        }


        // 运行完样例后, 每组样例的运行时间, 内存占用都保存在judgeCaseResult
        // 样例输出文件, 保存在data/xxx/1.ans中
        // 比对data/xxx/中 1.out和1.ans的内容
        // 设置程序的总运行
        submissionResult.setMemoryUsed(maxMemoryUsed);
        submissionResult.setTotalTime(totalTime);

        // 根据AC样例数与总样例数, 计算分数

        // 删除沙箱服务中保存的文件
//        if (fileId != null) {
////            sandboxRun.delFile(fileId);
//            sandboxFeignClient.deleteFile(fileId);
//        }

        submissionResult.setScore(totalScore);
        // 提交结果中包含所有测试样例的测试结果
        submissionResult.setJudgeCaseResults(judgeCaseResults);
        // 判题结束后, 修改数据库中的submission的信息
        submission.setId(submissionId);
        if (totalScore == 100) {
            this.changeStatus(submission, submissionResult, SubmissionStatusEnum.ACCEPTED);
        } else {
            for (JudgeCaseResult judgeCaseResult : judgeCaseResults) {
                if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.TIME_LIMIT_EXCEEDED);
                    break;
                } else if (judgeCaseResult.getJudgeResult().equals(JudgeResultEnum.WRONG_ANSWER.getValue())) {
                    this.changeStatus(submission, submissionResult, SubmissionStatusEnum.WRONG_ANSWER);
                    break;
                }
            }
        }
        //❗❗❗❗❗❗❗❗❗❗ 给normalproblem队列发送消息，修改题目的统计数据 ❗❗❗❗❗❗❗
        ProblemMessage problemMessage = new ProblemMessage();
        problemMessage.setSid(submissionId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, problemMessage);
    }

    private Result runPython(String sourceCode, String input, LanguageConfig languageConfig) {
        Cmd cmd = new Cmd();
        // args
        List<String> args = languageConfig.getExeArgs();
        cmd.setArgs(args);
        // envs
        List<String> envs = languageConfig.getEnvs();
        cmd.setEnv(envs);
        // files
        JSONArray files = new JSONArray();
        files.add(new JSONObject().set("content", input));
        files.add(new JSONObject().set("name","stdout").set("max", 10240));
        files.add(new JSONObject().set("name","stderr").set("max", 10240));
        cmd.setFiles(files);
        // limit
        cmd.setCpuLimit(COMPILE_CPU_LIMIT);
        cmd.setMemoryLimit(MEMORY_LIMIT);
        cmd.setProcLimit(PROC_LIMIT);
        // copyIn
        JSONObject copyIn = new JSONObject();
        // python不需要编译，所以直接拷贝python文件
        copyIn.set(languageConfig.getExeFileName(), new JSONObject().set("content", sourceCode));
        cmd.setCopyIn(copyIn);

        SandBoxRequest sandBoxRequest = new SandBoxRequest();

        List<Cmd> cmds = Arrays.asList(cmd);
        sandBoxRequest.setCmd(cmds);

        List<Result> results = sandboxFeignClient.run(sandBoxRequest);
        Result result = results.get(0);
        String status = result.getStatus();
        // 执行成功
        if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            log.info("执行成功");
            String stdout = result.getFiles().getStdout();
            log.info("代码输出 = " + stdout);
        } else {
            log.info("运行失败");
            log.info(result.getError());
        }
        return result;
    }

    /**
     * 运行代码
     * 一次运行结束后不删除exe文件
     * 等待调用方测试完多组数据后删除
     * @param fileId
     * @return
     */
    public Result runCode(String fileId, String input, LanguageConfig languageConfig) {
        Cmd cmd = new Cmd();
        // args
        List<String> args = languageConfig.getExeArgs();
        cmd.setArgs(args);
        // envs
        List<String> envs = languageConfig.getEnvs();
        cmd.setEnv(envs);
        // files
        JSONArray files = new JSONArray();
        // 这里的JSON使用了content字段, 即使用了MemoryFile, 直接指定输入文件的内容
        // interface MemoryFile {
        //    content: string | Buffer; // 文件内容
        //}
        // 其实也可以指定本地的路径, 例如data/1763440748296044545/.in
        // interface LocalFile {
        //    src: string; // 文件绝对路径
        //}
        // 也可以指定到上传到服务器的文件id
        // 这种得提前上传文件, 然后再指定文件id
        files.add(new JSONObject().set("content", input));
        files.add(new JSONObject().set("name","stdout").set("max", 10240));
        files.add(new JSONObject().set("name","stderr").set("max", 10240));
        cmd.setFiles(files);
        // limit
        cmd.setCpuLimit(CPU_LIMIT);
        cmd.setMemoryLimit(MEMORY_LIMIT);
        cmd.setProcLimit(PROC_LIMIT);
        // copyIn
        JSONObject copyIn = new JSONObject();
        copyIn.set(languageConfig.getExeFileName(),new JSONObject().set("fileId", fileId));
        cmd.setCopyIn(copyIn);

        SandBoxRequest sandBoxRequest = new SandBoxRequest();

        List<Cmd> cmds = Arrays.asList(cmd);
        sandBoxRequest.setCmd(cmds);

//        SandBoxResponse response = sandboxRun.run(sandBoxRequest);
        List<Result> results = sandboxFeignClient.run(sandBoxRequest);
        Result result = results.get(0);
        String status = result.getStatus();
        // 执行成功
        if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            log.info("执行成功");
            String stdout = result.getFiles().getStdout();
            log.info("代码输出 = " + stdout);
        } else {
            log.info("运行失败");
            log.info(result.getError());
        }
        return result;
    }

    /**
     * 编译代码
     * @return
     */
    @Override
    public Result compileCode(String sourceCode, LanguageConfig languageConfig) throws IOException {
        Cmd cmd = new Cmd();
        // args
        List<String> args = languageConfig.getCmpArgs();
        cmd.setArgs(args);
        // envs
        List<String> envs = languageConfig.getEnvs();
        cmd.setEnv(envs);
        // files
        JSONArray files = new JSONArray();
        files.add(new JSONObject().set("content",""));
        files.add(new JSONObject().set("name","stdout").set("max", 64 * 1024 * 1024));
        files.add(new JSONObject().set("name","stderr").set("max", 64 * 1024 * 1024));
        cmd.setFiles(files);
        // limit
        cmd.setCpuLimit(COMPILE_CPU_LIMIT);
        cmd.setMemoryLimit(MEMORY_LIMIT);
        cmd.setProcLimit(PROC_LIMIT);
        // copyOut
        List<String> copyOut = Arrays.asList("stdout", "stderr");
        cmd.setCopyOut(copyOut);
        // copyOutCached
        List<String> copyOutCached = languageConfig.getExeArgs(); // ❗❗
        cmd.setCopyOutCached(copyOutCached);
        // copyIn
        JSONObject copyIn = new JSONObject();
        copyIn.set(languageConfig.getSourceFileName(), new JSONObject().set("content", sourceCode));
        cmd.setCopyIn(copyIn);
        SandBoxRequest sandBoxRequest = new SandBoxRequest();
        cmd.setStrictMemoryLimit(true);
        List<Cmd> cmds = Arrays.asList(cmd);
        sandBoxRequest.setCmd(cmds);

        // 调用sandboxRun编译
//        SandBoxResponse response = sandboxRun.compile(sandBoxRequest);
        List<Result> results = sandboxFeignClient.run(sandBoxRequest);
        Result result = results.get(0);
        log.info(result.toString());
        if (result.getStatus().equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            return result;
        }
        log.info(result.getStatus());
        log.info(result.getError());
        return result;
    }
}
