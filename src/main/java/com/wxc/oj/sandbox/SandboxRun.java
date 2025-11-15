package com.wxc.oj.sandbox;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wxc.oj.sandbox.dto.SandBoxRequest;
import com.wxc.oj.sandbox.dto.SandBoxResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;


/**
 * 调用代码沙箱服务
 */
@Slf4j(topic = "❗❗❗❗❗❗❗")
@Component
public class SandboxRun {


    /**
     * 发送rest请求
     */
    @Resource
    private RestTemplate restTemplate;



    // ❗❗❗ 云服务器里运行代码沙箱服务
    private static final String SANDBOX_BASE_URL = "http://124.70.131.122:5050";
    private static final String RUN_URI = "/run";

    public static final HashMap<String, Integer> RESULT_MAP_STATUS = new HashMap<>();

    private static final int maxProcessNumber = 128;

    private static final int TIME_LIMIT_MS = 16000;

    private static final int MEMORY_LIMIT_MB = 512;

    private static final int STACK_LIMIT_MB = 128;

    private static final int STDIO_SIZE_MB = 32;

    private SandboxRun() {

    }


    /**
     * 运行用户代码
     * 向代码沙箱的run接口发送请求
     * @return
     */
    public SandBoxResponse run(SandBoxRequest request)  {
        JSONObject param = JSONUtil.parseObj(request);
        System.out.println(param);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request1 = new HttpEntity<>(JSONUtil.toJsonStr(param), headers);
        ResponseEntity<String> postForEntity;
        postForEntity = restTemplate.postForEntity(SANDBOX_BASE_URL + RUN_URI, request1, String.class);

        JSONArray jsonArray = JSONUtil.parseArray(postForEntity.getBody());
        JSONObject jsonObject = (JSONObject)jsonArray.get(0);
        SandBoxResponse response = JSONUtil.toBean(jsonObject, SandBoxResponse.class);
        return response;
    }

    /**
     * /file POST 上传一个文件到文件存储，
     * 返回一个文件 ID 用于提供给 /run 接口
     */


    /**
     * 编译用户代码
     *
     * @return
     */
    public SandBoxResponse compile(SandBoxRequest request) throws IOException {
        // run和compile都有调用run接口, 参数有所不同
        SandBoxResponse compileResponse = run(request);
        return compileResponse;
    }



    /**
     * 删除代码沙箱中的文件
     * @param fileId 文件的id
     */
    public void delFile(String fileId) {
        try {
            restTemplate.delete(SANDBOX_BASE_URL + "/file/{0}", fileId);
            log.info("文件删除成功: " + fileId);
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() != 200) {
                log.error("代码沙箱文件删除失败{}", ex.getResponseBodyAsString());
            }
        }
    }

}