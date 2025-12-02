package com.wxc.oj.interceptor;


import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxc.oj.common.BaseResponse;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.common.ResultUtils;
import com.wxc.oj.constant.RedisConstant;
import com.wxc.oj.model.po.User;
import com.wxc.oj.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录保护拦截器:
 * 检查请求头是否包含有效token
 * 如果有: 放行
 * 没有: 返回504
 * @LastModifiedBy wxc
 * @LastModifiedDate 2025年3月26日15点13分
 */
@Component
@Slf4j
public class LoginProtectInterceptor implements HandlerInterceptor {


    @Resource
    StringRedisTemplate stringRedisTemplate;
    private static final String BEARER_PREFIX = "Bearer ";
    @Resource
    private JwtUtils jwtUtils;

    /**
     * TODO:
     *      1. 从请求头中获取token
     *      2. 检查token是否有效
     *          有效: 放行
     *          无效: 返回504
     * @param request
     * @param response
     * @param handler
     * @return true -> 放行
     *         false -> 不放行
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }
        String token1 = request.getHeader("Authorization");
        if (token1 == null || !token1.startsWith(BEARER_PREFIX)) {
            BaseResponse result = ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().print(json);
            return false;
        }
        String token = token1.substring(7);
        boolean valid = jwtUtils.isTokenValid(token);
        if (valid) {
            Long userId = JwtUtils.getUserIdFromToken(token);
            String s = stringRedisTemplate.opsForValue().get(RedisConstant.USER_KEY + userId);
            User user = JSONUtil.toBean(s, User.class);
            if (s != null && user != null) {
                return true;
            }
        }
        BaseResponse result = ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().print(json);
        return false;
    }

}
