package com.wxc.oj.judger;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.enums.sandbox.SandBoxResponseStatus;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.req.sandbox.Cmd;
import com.wxc.oj.model.req.sandbox.LanguageConfig;
import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.SandBoxRequest;
import com.wxc.oj.openFeign.SandboxFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 沙箱编译/运行封装，供判题与标程造样例复用。
 */
@Slf4j(topic = "SandboxCodeRunner")
@Component
public class SandboxCodeRunner {

    @Resource
    private SandboxFeignClient sandboxFeignClient;

    @Value("${oj.judge.cpu-limit}")
    private Long cpuLimit;

    @Value("${oj.judge.compile-cpu-limit}")
    private Long compileCpuLimit;

    @Value("${oj.judge.memory-limit}")
    private Long memoryLimit;

    @Value("${oj.judge.proc-limit}")
    private Integer procLimit;

    public boolean needCompile(LanguageConfigEnum languageEnum) {
        return languageEnum != LanguageConfigEnum.PYTHON;
    }

    /**
     * 编译源码。解释型语言直接返回 null（无需编译）。
     */
    public Result compile(LanguageConfigEnum languageEnum, String sourceCode) {
        if (!needCompile(languageEnum)) {
            return null;
        }
        LanguageConfig languageConfig = languageEnum.getConfig();
        Cmd cmd = new Cmd();
        cmd.setArgs(languageConfig.getCmpArgs());
        cmd.setEnv(languageConfig.getEnvs());

        JSONArray files = new JSONArray();
        files.add(new JSONObject().set("content", ""));
        files.add(new JSONObject().set("name", "stdout").set("max", 64 * 1024 * 1024));
        files.add(new JSONObject().set("name", "stderr").set("max", 64 * 1024 * 1024));
        cmd.setFiles(files);
        cmd.setCpuLimit(compileCpuLimit);
        cmd.setMemoryLimit(memoryLimit);
        cmd.setProcLimit(procLimit);
        cmd.setStrictMemoryLimit(true);
        cmd.setCopyOut(Arrays.asList("stdout", "stderr"));
        cmd.setCopyOutCached(languageConfig.getExeArgs());

        JSONObject copyIn = new JSONObject();
        copyIn.set(languageConfig.getSourceFileName(), new JSONObject().set("content", sourceCode));
        cmd.setCopyIn(copyIn);

        SandBoxRequest request = new SandBoxRequest();
        request.setCmd(Collections.singletonList(cmd));
        List<Result> results = sandboxFeignClient.run(request);
        if (results == null || results.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "sandbox runtime error");
        }
        Result result = results.get(0);
        log.info("compile result status={}", result.getStatus());
        return result;
    }

    /**
     * 运行代码。编译型语言传入 exeFileId；解释型语言传 sourceCode。
     */
    public Result run(LanguageConfigEnum languageEnum, String sourceCode, String exeFileId, String input) {
        LanguageConfig languageConfig = languageEnum.getConfig();
        Cmd cmd = new Cmd();
        cmd.setArgs(languageConfig.getExeArgs());
        cmd.setEnv(languageConfig.getEnvs());

        JSONArray files = new JSONArray();
        files.add(new JSONObject().set("content", input == null ? "" : input));
        files.add(new JSONObject().set("name", "stdout").set("max", 10240));
        files.add(new JSONObject().set("name", "stderr").set("max", 10240));
        cmd.setFiles(files);
        cmd.setCpuLimit(cpuLimit);
        cmd.setMemoryLimit(memoryLimit);
        cmd.setProcLimit(procLimit);

        JSONObject copyIn = new JSONObject();
        if (needCompile(languageEnum) && exeFileId != null) {
            copyIn.set(languageConfig.getExeFileName(), new JSONObject().set("fileId", exeFileId));
        } else {
            copyIn.set(languageConfig.getExeFileName(), new JSONObject().set("content", sourceCode));
        }
        cmd.setCopyIn(copyIn);

        SandBoxRequest request = new SandBoxRequest();
        request.setCmd(Collections.singletonList(cmd));
        List<Result> results = sandboxFeignClient.run(request);
        if (results == null || results.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "sandbox runtime error");
        }
        Result result = results.get(0);
        if (SandBoxResponseStatus.ACCEPTED.getValue().equals(result.getStatus())) {
            log.info("run accepted");
        } else {
            log.warn("run failed status={}, error={}", result.getStatus(), result.getError());
        }
        return result;
    }

    /**
     * 从编译结果取出可执行文件 id；解释型语言返回 null。
     */
    public String extractExeFileId(LanguageConfigEnum languageEnum, Result compileResult) {
        if (!needCompile(languageEnum) || compileResult == null || compileResult.getFileIds() == null) {
            return null;
        }
        return compileResult.getFileIds().get(languageEnum.getConfig().getExeFileName());
    }

    public void deleteFile(String fileId) {
        if (fileId != null) {
            sandboxFeignClient.deleteFile(fileId);
        }
    }
}
