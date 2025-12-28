package com.wxc.oj.model.req.sandbox;

import com.wxc.oj.enums.sandbox.FileErrorType;
import lombok.Data;

/**
 * interface FileError {
 *     name: string; // 错误文件名称
 *     type: FileErrorType; // 错误代码
 *     message?: string; // 错误信息
 * }
 */
@Data
public class FileError {
    private String name;        // 错误文件名称
    private FileErrorType type; // 错误代码
    private String message;     // 错误信息 (可选)
}

