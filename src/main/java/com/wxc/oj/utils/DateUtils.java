package com.wxc.oj.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final String DATE_PATTERN = "yyyyMMdd";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * 获取当前日期的yyyyMMdd字符串
     */
    public static String getCurrentDateStr() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 解析yyyyMMdd字符串为LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}