package com.wxc.oj.utils;

import com.wxc.oj.model.vo.UserVO;

public class UserHolder {
    private static final ThreadLocal<UserVO> userTl = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenTl = new ThreadLocal<>();

    public static void saveUser(UserVO user) {
        userTl.set(user);
    }

    public static void saveToken(String token) {
        tokenTl.set(token);
    }

    public static UserVO getUser() {
        return userTl.get();
    }

    public static String getToken() {
        return tokenTl.get();
    }

    public static void removeUser() {
        userTl.remove();
    }

    public static void removeToken() {
        tokenTl.remove();
    }

    public static void clear() {
        removeUser();
        removeToken();
    }
}
