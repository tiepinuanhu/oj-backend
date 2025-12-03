package com.wxc.oj.model.dto.sandbox;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.Data;

import java.util.List;




/**
 * interface Cmd {
 *     args: string[]; // 程序命令行参数
 *     env?: string[]; // 程序环境变量
 *
 *     // 指定 标准输入、标准输出和标准错误的文件 (null 是为了 pipe 的使用情况准备的，而且必须被 pipeMapping 的 in / out 指定)
 *     files?: (LocalFile | MemoryFile | PreparedFile | Collector | StreamIn | StreamOut | null)[];
 *     tty?: boolean; // 开启 TTY （需要保证标准输出和标准错误为同一文件）同时需要指定 TERM 环境变量 （例如 TERM=xterm）
 *
 *     // 资源限制
 *     cpuLimit?: number;     // CPU时间限制，单位纳秒
 *     clockLimit?: number;   // 等待时间限制，单位纳秒 （通常为 cpuLimit 两倍）
 *     memoryLimit?: number;  // 内存限制，单位 byte
 *     stackLimit?: number;   // 栈内存限制，单位 byte
 *     procLimit?: number;    // 线程数量限制
 *     cpuRateLimit?: number; // 仅 Linux，CPU 使用率限制，1000 等于单核 100%
 *     cpuSetLimit?: string;  // 仅 Linux，限制 CPU 使用，使用方式和 cpuset cgroup 相同 （例如，`0` 表示限制仅使用第一个核）
 *     strictMemoryLimit?: boolean; // deprecated: 使用 dataSegmentLimit （这个选项依然有效）
 *     dataSegmentLimit?: boolean; // 仅linux，开启 rlimit 堆空间限制（如果不使用cgroup默认开启）
 *     addressSpaceLimit?: boolean; // 仅linux，开启 rlimit 虚拟内存空间限制（非常严格，在所以申请时触发限制）
 *
 *     // 在执行程序之前复制进容器的文件列表
 *     copyIn?: {[dst:string]:LocalFile | MemoryFile | PreparedFile | Symlink};
 *
 *     // 在执行程序后从容器文件系统中复制出来的文件列表
 *     // 在文件名之后加入 '?' 来使文件变为可选，可选文件不存在的情况不会触发 FileError
 *     copyOut?: string[];
 *     // 和 copyOut 相同，不过文件不返回内容，而是返回一个对应文件 ID ，内容可以通过 /file/:fileId 接口下载
 *     copyOutCached?: string[];
 *     // 指定 copyOut 复制文件大小限制，单位 byte
 *     copyOutMax?: number;
 * }
 */
@Data
public class Cmd {

    private List<String> args; // 程序命令行参数
    private List<String> env; // 程序环境变量
    private JSONArray files; // 指定标准输入、标准输出和标准错误的文件

    private boolean tty; // 开启 TTY
    // 资源限制
    private long cpuLimit; // CPU时间限制，单位纳秒
    private long clockLimit; // 等待时间限制，单位纳秒 ❗❗❗
    private long memoryLimit; // 内存限制，单位 byte ❗❗❗
    private long stackLimit; // 栈内存限制，单位 byte
    private long procLimit; // 线程数量限制
    private long cpuRateLimit; // CPU 使用率限制，1000 等于单核 100%
    private String cpuSetLimit; // 限制 CPU 使用
    private boolean strictMemoryLimit; // 使用 dataSegmentLimit
    private boolean dataSegmentLimit; // 开启 limit 堆空间限制
    private boolean addressSpaceLimit; // 开启 limit 虚拟内存空间限制

    // 在执行程序之前复制进容器的文件列表
    private JSONObject copyIn;

    // 在执行程序后从容器文件系统中复制出来的文件列表
    private List<String> copyOut;
    private List<String> copyOutCached;
    private long copyOutMax; // 指定 copyOut 复制文件大小限制，单位 byte
}