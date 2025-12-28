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

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    UserVO userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    List<User> queryUserVOByAccount(String userAccount);
    /**
     * 通过token获取当前登录用户
     * @return
     */
//    User getLoginUser(String token);
    User getLoginUser(HttpServletRequest request);

//    /**
//     * 获取当前登录用户（允许未登录）
//     *
//     * @param request
//     * @return
//     */
//    UserVO getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
//    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
