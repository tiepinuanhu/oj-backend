package com.wxc.oj.judger;

import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;

/**
 * 判题策略接口（定义每种语言的特定行为）
 */
public interface JudgeStrategy {
//    /**
//     * 编译代码（解释型语言如Python可返回空结果）
//     * @param sourceCode 源码
//     * @return 沙箱编译结果
//     */
//    Result compile(String sourceCode) throws IOException;
//
//    /**
//     * 运行代码（针对单个测试用例）
//     * @param executableFileId 编译后的文件ID（解释型语言为源码文件名）
//     * @param input 测试用例输入
//     * @return 沙箱运行结果
//     */
//    Result run(String executableFileId, String input);
//
//    /**
//     * 获取语言配置
//     * @return 语言配置
//     */
//    LanguageConfig getLanguageConfig();
//
//    /**
//     * 获取可执行文件名称（用于沙箱的copyIn）
//     * @return 可执行文件名
//     */
//    String getExecutableFileName();


    void doJudge(Submission submission, Problem problem);

}