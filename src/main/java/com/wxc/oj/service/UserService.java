package com.wxc.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxc.oj.model.req.user.UserQueryRequest;
import com.wxc.oj.model.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxc.oj.model.vo.login.LoginVO;
import com.wxc.oj.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author 王新超
* @description 针对表【user】的数据库操作Service
* @createDate 2024-02-28 10:12:35
*/
public interface UserService extends IService<User> {


    UserVO userRegister(String userAccount, String userPassword, String checkPassword);


    LoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    List<User> queryUserVOByAccount(String userAccount);

    boolean isAdmin();

    boolean isAdmin(UserVO user);


    boolean userLogout(HttpServletRequest request);


    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
