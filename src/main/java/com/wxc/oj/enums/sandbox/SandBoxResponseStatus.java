package com.wxc.oj.enums.sandbox;

/**
 * enum Status {
 *     Accepted = 'Accepted', // 正常情况
 *     MemoryLimitExceeded = 'Memory Limit Exceeded', // 内存超限
 *     TimeLimitExceeded = 'Time Limit Exceeded', // 时间超限
 *     OutputLimitExceeded = 'Output Limit Exceeded', // 输出超限
 *     FileError = 'File Error', // 文件错误
 *     NonzeroExitStatus = 'Nonzero Exit Status', // 非 0 退出值
 *     Signalled = 'Signalled', // 进程被信号终止
 *     InternalError = 'Internal Error', // 内部错误
 * }
 */
public enum SandBoxResponseStatus {
    ACCEPTED("Accepted"),
    COMPILE_ERROR("Compile Error"),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded"),
    TIME_LIMIT_EXCEEDED ("Time Limit Exceeded"),
    OUTPUT_LIMIT_EXCEEDED("Output Limit Exceeded"),
    FILE_ERROR("File Error"),
    NON_ZERO_ERROR("Non Zero Exit Status"),
    DANGEROUS_SYSCALL("Dangerous Syscall"),
    INTERNAL_ERROR("Internal Error");

    private String value;
    SandBoxResponseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
