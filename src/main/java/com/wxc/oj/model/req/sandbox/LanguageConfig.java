package com.wxc.oj.model.req.sandbox;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * 语言配置类
 */
public class LanguageConfig {


    List<String> cmpArgs;

    List<String> exeArgs;

    List<String> envs;

    String exeFileName;

    String sourceFileName;

}
