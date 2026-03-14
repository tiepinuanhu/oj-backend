package com.wxc.oj.utils;

import com.wxc.oj.model.vo.UserVO;

public class UserHolder {
    private static final ThreadLocal<UserVO> tl = new ThreadLocal<>();


    private static String token = new String();
    public static void saveUser(UserVO user){
        tl.set(user);
    }
    public static void saveToken(String token1){
        token = token1;
    }

    public static UserVO getUser(){
        return tl.get();
    }
    public static String getToken(){
        return token;
    }

    public static void removeUser(){
        tl.remove();
    }
}