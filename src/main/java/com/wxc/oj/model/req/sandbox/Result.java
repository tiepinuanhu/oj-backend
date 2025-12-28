package com.wxc.oj.model.req.sandbox;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
* @author wangxinchao
* @date 2025/12/2 20:19
* typescript 定义
* interface Result {
*     status: Status;
*     error?: string; // 详细错误信息
*     exitStatus: number; // 程序返回值
*     time: number;   // 程序运行 CPU 时间，单位纳秒
*     memory: number; // 程序运行内存，单位 byte
*     runTime: number; // 程序运行现实时间，单位纳秒
*     // copyOut 和 pipeCollector 指定的文件内容
*     files?: {[name:string]:string};
*     // copyFileCached 指定的文件 id
*     fileIds?: {[name:string]:string};
*     // 文件错误详细信息
*     fileError?: FileError[];
* }
*/
@Data
public class Result {
    private String status;
    private String error;
    private int exitStatus;
    private long time;
    private long memory;
    private long runTime;
    private Files files;
    private Map<String, String> fileIds;
    private List<FileError> fileError;
}