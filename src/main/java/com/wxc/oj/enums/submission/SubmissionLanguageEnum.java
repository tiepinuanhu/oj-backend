package com.wxc.oj.enums.submission;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 可供提交的编程语言枚举
 */
@AllArgsConstructor
@Getter
public enum SubmissionLanguageEnum {
    JAVA("java"),
    CPP("cpp"),
    C("c"),
    PYTHON("python");

    private final String value;


//    SubmissionLanguageEnum(String value) {
//        this.value = value;
//    }
//
//
//    public String getValue() {
//        return value;
//    }

    /**
     * 获取值列表
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }


    /**
     * 使用Map存储，不用每次from都遍历所有value
     */
    public static final Map<String, SubmissionLanguageEnum> VALUE_MAP
            = Arrays.stream(values())
            .collect(Collectors.toMap(item -> item.value, item -> item));

    public static SubmissionLanguageEnum from(String value) {
//        return Arrays.stream(values())
//                .filter(item -> item.getValue().equals(value))
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("submission language value:" + value + " not exists"));
        if (!VALUE_MAP.containsKey(value)) {
            throw new IllegalArgumentException("submission language value:" + value + " not exists");
        }
        return VALUE_MAP.get(value);
    }

}
