package com.wxc.oj.judger;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.RabbitMQConstant;
import com.wxc.oj.enums.JudgeResultEnum;
import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.enums.sandbox.SandBoxResponseStatus;
import com.wxc.oj.enums.submission.SubmissionStatusEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.judger.model.TestCase;
import com.wxc.oj.judger.model.TestCases;
import com.wxc.oj.model.req.sandbox.Cmd;
import com.wxc.oj.model.req.sandbox.LanguageConfig;
import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.SandBoxRequest;
import com.wxc.oj.model.judge.JudgeCaseResult;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.queueMessage.ProblemMessage;
import com.wxc.oj.model.queueMessage.SubmissionStatusMessage;
import com.wxc.oj.model.submission.SubmissionResult;
import com.wxc.oj.openFeign.SandboxFeignClient;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.SubmissionService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.wxc.oj.constant.RabbitMQConstant.SUBMISSION_STATUS_TOPIC;

/**
 * 判题模板抽象类（封装通用判题流程）
 * 通用方法抽取到父类中
 * 子类重写抽象方法实现特定的功能
 */
@Slf4j(topic = "💯AbstractJudgeStrategy💯")
public abstract class AbstractJudgeStrategy implements JudgeStrategy {
    @Resource
    protected SandboxFeignClient sandboxFeignClient;
    @Resource
    protected SubmissionService submissionService;
    @Resource
    protected ProblemService problemService;
    @Resource
    protected RabbitTemplate rabbitTemplate;

    @Value("${oj.data.path}")
    protected String dataPath;
    @Value("${oj.judge.cpu-limit}")
    protected Long cpuLimit;
    @Value("${oj.judge.compile-cpu-limit}")
    protected Long compileCpuLimit;
    @Value("${oj.judge.memory-limit}")
    protected Long memoryLimit;
    @Value("${oj.judge.proc-limit}")
    protected Integer procLimit;

    // 通用常量
    protected static final String STDOUT = "stdout";
    protected static final String STDERR = "stderr";
    protected static final int MAX_OUTPUT_SIZE = 10240;



    // 需要子类重写的方法
//    protected abstract String getExecutableFileName();
    protected abstract LanguageConfigEnum getLanguageConfig();
    protected abstract boolean needCompile();


    /**
     * 执行单个测试用例（核心）
     */
    private JudgeCaseResult executeSingleTestCase(Long problemId, TestCase testCase,
                                                    JudgeConfig judgeConfig, String sourceCode, String exeId,
            Long submissionId) throws IOException {
        int caseIndex = testCase.getIndex();
        // 读取对应的测试样例
        String input = readInputFile(problemId, caseIndex);

        log.info("读取到第"+ caseIndex + "个测试样例输入: " + input);


        // 运行第index个测试样例
        Result runResult = runCode(sourceCode, exeId, input);


        String status = runResult.getStatus();

        JudgeCaseResult judgeCaseResult = new JudgeCaseResult();
        judgeCaseResult.setIndex(caseIndex);
        judgeCaseResult.setInput(input);
        judgeCaseResult.setFullScore(testCase.getFullScore());
        // ns => ms
        Long timeCost = runResult.getRunTime() / 1000_000;
        Long memoryUsed = runResult.getMemory();



        judgeCaseResult.setMemoryUsed(memoryUsed);
        judgeCaseResult.setTimeCost(timeCost);

        // 执行成功
        if (status.equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            // 获取输出文件.ans
            String output = runResult.getFiles().getStdout();
            // E:/data/1/submission/1/1.ans
            String ansFilePath = dataPath + File.separator + "submission"
                    + File.separator + submissionId + File.separator + caseIndex + ".ans";
            try (FileWriter fileWriter = new FileWriter(ansFilePath)) {
                fileWriter.write(output);
                fileWriter.flush();
                log.info("写入临时文件成功: {}", ansFilePath);
            } catch (IOException e) {
                log.error("写入临时文件失败: {}", ansFilePath, e);
                throw e;
            }

            judgeCaseResult.setOutput(output);
            // 比较.ans和.out文件
            boolean accepted = checker(problemId, submissionId, caseIndex);
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


        // 返回该样例的标准答案
        // E:/oj-data/ans/1/1.out
        String stdoutFilePath = dataPath + File.separator + "ans" + File.separator + problemId + File.separator + caseIndex + ".out";
        String line;
        StringBuilder ansFile = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(stdoutFilePath));
        while ((line = reader.readLine()) != null) {
            ansFile.append(line).append("\n");
        }
        String ansFileString = ansFile.toString();
        judgeCaseResult.setAns(ansFileString);
        return judgeCaseResult;
    }
    /**
     * 模板方法：完整判题流程（子类无需重写，仅需实现语言特定方法）
     * @param submission 提交记录
     * @param problem 题目
     * @throws IOException IO异常
     */
    @SneakyThrows
    @Override
    public void doJudge(Submission submission, Problem problem)  {
        Long submissionId = submission.getId();
        Long problemId = problem.getId();
        String sourceCode = submission.getSourceCode();
        SubmissionResult submissionResult = new SubmissionResult();
        LanguageConfig languageConfig = getLanguageConfig().getConfig();
        // 1. 更新状态为编译中
        changeStatus(submission, submissionResult, SubmissionStatusEnum.COMPILING);


        String executableFileId = null;
        try {
            // 2. 编译代码（解释型语言会跳过或返回空结果）
            if (needCompile()) {
                Result compileResult = compileCode(submission.getSourceCode());
                if (compileResult == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "sandbox runtime error");
                }
                // 检查是否有编译错误
                checkCompileError(submission, submissionResult, compileResult);
                Map<String, String> fileIds = compileResult.getFileIds();
                executableFileId = fileIds.get(languageConfig.getExeFileName());
            }

            // 读取判题配置
            String judgeConfigStr = problem.getJudgeConfig();
            JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
            // 获取测试样例
            List<TestCase> testCaseList = readTestCases(problemId);

            // 准备评测每个样例
            changeStatus(submission, submissionResult, SubmissionStatusEnum.JUDGING);

            // 计算得分
            // 统计时间和内存使用
            Long totalTime = 0L;
            Long maxMemoryUsed = 0L;
            int totalScore = 0;
            List<JudgeCaseResult> judgeCaseResults = new ArrayList<>();
            // 创建submissionId对应的目录
            FileUtil.mkdir(dataPath + File.separator + "submission" + File.separator + submissionId);
            for (TestCase testCase : testCaseList) {
                // 运行第i个测试样例，并获取结果进行统计


                JudgeCaseResult judgeCaseResult = executeSingleTestCase(problemId, testCase, judgeConfig,
                        sourceCode, executableFileId, submissionId);

                totalTime += judgeCaseResult.getTimeCost();
                maxMemoryUsed = Math.max(maxMemoryUsed, judgeCaseResult.getMemoryUsed());
                totalScore += judgeCaseResult.getGainScore();

                judgeCaseResults.add(judgeCaseResult);
            }
            submissionResult.setMemoryUsed(maxMemoryUsed);
            submissionResult.setTotalTime(totalTime);
            submissionResult.setScore(totalScore);
            // 提交结果中包含所有测试样例的测试结果
            submissionResult.setJudgeCaseResults(judgeCaseResults);
            // 判题结束后, 修改数据库中的submission的信息
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

            ProblemMessage problemMessage = new ProblemMessage();
            problemMessage.setSid(submissionId);
            SubmissionStatusMessage submissionStatusMessage = SubmissionStatusMessage.builder()
                    .userId(submission.getUserId())
                    .problemId(problemId)
                    .submissionId(submissionId)
                    .status(submissionResult.getStatus()).build();
            // 发送消息异步更新题目统计数据，和用户排行榜
            rabbitTemplate.convertAndSend(
                    RabbitMQConstant.SUBMISSION_STATUS_EXCHANGE,
                    SUBMISSION_STATUS_TOPIC,
                    submissionStatusMessage);
            log.info("💕💕💕消息已经发送");
        } finally {
            // 最终清理沙箱文件（无论成功失败都执行）
            cleanSandboxFile(executableFileId);
            // 清理本次提交的所有临时.ans文件
            deleteAllDotAnsFile(submissionId);
        }
    }




    private Result compileCode(String sourceCode) {
        // ❗调用子类的方法，获取子类特有的语言评测指令
        LanguageConfig languageConfig = getLanguageConfig().getConfig();
        Cmd cmd = new Cmd();
        // ❗获取编译指令和参数
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
        cmd.setCpuLimit(compileCpuLimit);
        cmd.setMemoryLimit(memoryLimit);
        cmd.setProcLimit(procLimit);
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
        // ❗❗❗使用RPC调用沙箱服务
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

    /**
     *
     * @param exeFileId
     * @param input
     * @return
     */
    protected Result runCode(String sourceCode, String exeFileId, String input) {
        LanguageConfig languageConfig = getLanguageConfig().getConfig();
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
        cmd.setCpuLimit(cpuLimit);
        cmd.setMemoryLimit(memoryLimit);
        cmd.setProcLimit(procLimit);
        // copyIn
        JSONObject copyIn = new JSONObject();
        // 编译型语言传入编译过后的文件ID，解释型语言传入源码
        /**
         * 编译型语言，C++，Java
         * 运行时，传入编译好的文件，指定文件名称：main，指定文件id：fileId
         * "copyIn": {
         *      "main": {
         *          "fileId": "5LWIZAA45JHX4Y4Z" // 这个缓存文件的 ID 来自上一个请求返回的 fileIds
         *      }
         *  }
         * 解释性语言：python
         * copyIn下面传入文件名称和文件源码，使用的是MemoryFile
         *  "copyIn": {
         *      "main.py": {
         *           "content": "s = input().split()\nprint(int(s[0]) + int(s[1]))"
         *      }
         *   }
         */
        if (needCompile() && exeFileId != null) {
            copyIn.set(languageConfig.getExeFileName(), new JSONObject().set("fileId", exeFileId));
        } else {
            copyIn.set(languageConfig.getExeFileName(), new JSONObject().set("content", sourceCode));
        }
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


    private void checkCompileError(Submission submission, SubmissionResult submissionResult, Result compileResult) {
        if (!compileResult.getStatus().equals(SandBoxResponseStatus.ACCEPTED.getValue())) {
            // 返回编译错误
            submissionResult.setTotalTime(0L);
            submissionResult.setMemoryUsed(0L);
            submissionResult.setScore(0);
            String stderr = compileResult.getFiles().getStderr();
            submissionResult.setCompileErrorMessage(stderr);
            boolean b = this.changeStatus(submission, submissionResult, SubmissionStatusEnum.COMPILE_ERROR);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "submission更新失败");
            }
        }
    }


    /**
     * 读取测试用例（从config.json读取）
     */
    protected List<TestCase> readTestCases(Long problemId)  {

        // 将config.json转为TestCases
        String filePath = dataPath + File.separator  + "ans" + File.separator + problemId + File.separator + "config.json";
        String jsonStr = FileUtil.readUtf8String(filePath);
        TestCases testCases = JSONUtil.toBean(jsonStr, TestCases.class);
        return testCases.getCases();
    }

    /**
     * 读取输入文件内容
     */
    protected String readInputFile(Long problemId, int caseIndex) throws IOException {
        String inputPath = dataPath + File.separator + "ans"+ File.separator + problemId + File.separator + caseIndex + ".in";
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }





    private boolean changeStatus(Submission submissionUpd,
                                 SubmissionResult submissionResult, SubmissionStatusEnum statusUpd) {
        submissionResult.setStatus(statusUpd.getStatus());
        submissionResult.setStatusDescription(statusUpd.getDescription());
        Integer score = submissionResult.getScore();
        if (score != null) {
            submissionUpd.setScore(score);
        }
        submissionUpd.setStatus(statusUpd.getStatus());
        submissionUpd.setStatusDescription(statusUpd.getDescription());
        submissionUpd.setSubmissionResult(JSONUtil.toJsonStr(submissionResult));
        return submissionService.updateById(submissionUpd);
    }
//    /**
//     * 删除用户代码的运行临时输出文件
//     * @param pid
//     * @param index
//     * @return
//     */
//    public boolean deleteDotAnsFile(Long pid, int index) {
//        String filePath = dataPath + File.separator + pid + File.separator + index + ".ans";
//        File file = new File(filePath);
//        if (file.exists()) {
//            boolean deleted = file.delete();
//            if (!deleted) {
//                log.warn("未能删除文件: {}", filePath);
//            }
//            return deleted;
//        }
//        log.debug("文件不存在，无需删除: {}", filePath);
//        return false;
//    }

    /**
     * 删除submission对应的目录
     * @param submissionId
     */
    public void deleteAllDotAnsFile(Long submissionId) throws IOException {
        String subDir = dataPath + File.separator  + "submission" + File.separator
                + submissionId;
        File dir = new File(subDir);
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
                log.info("成功删除提交目录: {}", subDir);
            } catch (IOException e) {
                log.warn("删除提交目录失败: {}, 尝试强制删除", subDir, e);
                // 尝试递归删除
                forceDelete(dir);
            }
        }
    }
    private void forceDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    forceDelete(f);
                }
            }
        }
        if (!file.delete()) {
            log.warn("无法删除文件或目录: {}", file.getAbsolutePath());
        }
    }
    /**
     * 对比用户代码的输出文件和标准输出文件是否相同
     * @param pid
     * @param index
     * @return
     * @throws IOException
     */
    public boolean checker(Long pid, Long submissionId, int index) throws IOException {
        String stdoutFilePath = dataPath + File.separator + "ans"+ File.separator + pid + File.separator + index + ".out";
        String useroutFilePath = dataPath + File.separator + "submission"+ File.separator
                + submissionId + File.separator + index + ".ans";
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
    // 其他通用方法：updateSubmissionStatus、handleCompileError、calculateTotalScoreAndResources等（略）

    /**
     * 清理由于编译/运行在沙箱中产生的临时文件
     * @param fileId 文件ID
     */
    public void cleanSandboxFile(String fileId) {
        if (fileId != null) {
            sandboxFeignClient.deleteFile(fileId);
        }
    }
}