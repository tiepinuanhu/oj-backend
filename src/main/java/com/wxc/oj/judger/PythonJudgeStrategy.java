package com.wxc.oj.judger;

import com.wxc.oj.constant.LanguageConfigs;
import com.wxc.oj.model.dto.sandbox.LanguageConfig;
import com.wxc.oj.model.dto.sandbox.Result;
import org.springframework.stereotype.Service;

/**
 * C++判题策略
 */
@Service
public class PythonJudgeStrategy extends AbstractJudgeStrategy {

    @Override
    protected String getExecutableFileName() {
        return "main.py";
    }

    //    public Result compile(String sourceCode) throws IOException {
//        LanguageConfig config = getLanguageConfig();
//        // 构建沙箱编译请求（复用通用方法）
//        SandBoxRequest request = buildCompileRequest(sourceCode, config);
//        List<Result> results = sandboxFeignClient.run(request);
//        return results.get(0);
//    }
//
//    private SandBoxRequest buildCompileRequest(String sourceCode, LanguageConfig config) {
//
//    }
//
//    @Override
//    public Result run(String executableFileId, String input) {
//        LanguageConfig config = getLanguageConfig();
//        SandBoxRequest request = buildRunRequest(executableFileId, input, config);
//        List<Result> results = sandboxFeignClient.run(request);
//        return results.get(0);
//    }
//
    @Override
    public LanguageConfig getLanguageConfig() {
        // 从配置中读取C++的配置（替代硬编码的LanguageConfigs.CPP）
        return LanguageConfigs.PYTHON;
    }

//    @Override
//    protected Result compile(String sourceCode) {
//        return null;
//    }

    @Override
    protected boolean needCompile() {
        return false;
    }
//
//    @Override
//    public String getExecutableFileName() {
//        return "main";
//    }
//
//    @Override
//    protected boolean needCompile() {
//        return true;
//    }

    

    // C++特定的请求构建方法（略）
}