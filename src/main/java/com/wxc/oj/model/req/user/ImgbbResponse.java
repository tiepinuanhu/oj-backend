package com.wxc.oj.model.req.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * ImgBB 上传响应实体（根据官方返回结构定义）
 * 官方响应示例：https://api.imgbb.com/#response-format
 */
@Data
public class ImgbbResponse {
    private boolean success;  // 是否上传成功
    private int status;       // HTTP 状态码（200=成功）
    private ImgbbData data;   // 图片核心信息（url、尺寸等）
    private String error;     // 错误信息（失败时返回）

    // 嵌套实体：图片详细信息
    @Data
    public static class ImgbbData {
        private String id;                // 图片ID
        private String title;             // 图片标题
        private String url_viewer;        // 图片预览URL（直接访问）
        private String url;               // 图片原始URL
        private String display_url;       // 展示用URL
        @JsonProperty("delete_url")       // JSON字段为delete_url，映射到Java字段
        private String deleteUrl;         // 图片删除URL
        private int width;                // 图片宽度
        private int height;               // 图片高度
        private long size;                // 图片大小（字节）
    }
}