package com.wxc.oj.constant;

import com.wxc.oj.model.req.sandbox.LanguageConfig;

import java.util.Arrays;

public interface LanguageConfigs {


    LanguageConfig CPP = LanguageConfig.builder()
            .cmpArgs(Arrays.asList("/usr/bin/g++", "main.cpp", "-o", "main"))
            .exeArgs(Arrays.asList("main"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("main")
            .sourceFileName("main.cpp")
            .build();
    LanguageConfig JAVA = LanguageConfig.builder()
            .cmpArgs(Arrays.asList("/usr/bin/javac", "Main.java"))
            .exeArgs(Arrays.asList("/usr/bin/java","Main"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("Main")
            .sourceFileName("Main.java")
            .build();
    LanguageConfig PYTHON = LanguageConfig.builder()
            .exeArgs(Arrays.asList("python", "main.py"))
            .envs(Arrays.asList("PATH=/usr/bin:/bin"))
            .exeFileName("main.py")
            .sourceFileName("main.py")
            .build();
}
