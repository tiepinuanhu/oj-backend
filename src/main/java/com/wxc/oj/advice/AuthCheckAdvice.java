package com.wxc.oj.advice;

import com.wxc.oj.annotation.AuthCheck;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.UserRoleEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.po.User;
import com.wxc.oj.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * 权限校验 AOP
 */
@Aspect
@Component
@Slf4j(topic = "AuthCheckAdvice🌹🌹🌹🌹🌹🌹🌹")
public class AuthCheckAdvice {

    @Autowired
    private UserService userService;

    /**
     * 执行拦截
     * 切入所有带@AuthCheck注解的Controlller
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 通过注解获取需要的权限
        Integer mustRole = authCheck.mustRole().getValue();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 获取当前请求
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 必须有该权限才通过
        if (mustRole != null) {
            Integer userRole = loginUser.getUserRole();
            // 如果被封号，直接拒绝
            if (UserRoleEnum.BAN.getValue() == mustRole) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 必须有管理员权限
            if (UserRoleEnum.ADMIN.getValue() == mustRole) {
                if (mustRole != userRole) {
                    log.info("用户权限不足");
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
            }
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}

