package com.wxc.oj.advice;

import com.wxc.oj.annotation.AuthCheck;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.UserRoleEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.vo.UserVO;
import com.wxc.oj.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthCheckAdvice {

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        UserVO loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Integer mustRole = authCheck.mustRole().getValue();
        Integer userRole = loginUser.getUserRole();

        // 被封禁用户直接拒绝
        if (UserRoleEnum.BAN.getValue().equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "账号已被封禁");
        }

        // 需要管理员权限但当前不是管理员
        if (UserRoleEnum.ADMIN.getValue().equals(mustRole)
                && !UserRoleEnum.ADMIN.getValue().equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不足");
        }

        return joinPoint.proceed();
    }
}
