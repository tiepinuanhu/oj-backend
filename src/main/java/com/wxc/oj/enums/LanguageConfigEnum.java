package com.wxc.oj.enums;

import com.wxc.oj.model.req.sandbox.LanguageConfig;

import java.util.Arrays;

/**
 * @Author wxc
 * @Date 2026-03-11 15:40:53
 */
public enum LanguageConfigEnum {


    CPP("cpp", LanguageConfig.builder()
            .cmpArgs(Arrays.asList("/usr/bin/g++", "main.cpp", "-o", "main"))
            .exeArgs(Arrays.asList("main"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("main")
            .sourceFileName("main.cpp")
            .build()),

    JAVA("java",com.wxc.oj.model.req.sandbox.LanguageConfig.builder()
            .cmpArgs(Arrays.asList("/usr/bin/javac", "Main.java"))
            .exeArgs(Arrays.asList("/usr/bin/java", "Main"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("Main")
            .sourceFileName("Main.java")
            .build()),

    PYTHON("python",LanguageConfig.builder()
            .exeArgs(Arrays.asList("python", "main.py"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("main.py")
            .sourceFileName("main.py")
            .build());

    private final String value;
    private final LanguageConfig config;

    LanguageConfigEnum(String value, LanguageConfig config) {
        this.value = value;
        this.config = config;
    }

    public String getValue() {
        return value;
    }

    public LanguageConfig getConfig() {
        return config;
    }
    public static LanguageConfigEnum fromValue(String value) {
        for (LanguageConfigEnum item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unsupported language: " + value);
    }

    public static LanguageConfig getConfigByValue(String value) {
        return fromValue(value).getConfig();
    }
}
