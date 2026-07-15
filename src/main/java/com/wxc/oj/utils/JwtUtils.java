package com.wxc.oj.utils;

import com.alibaba.druid.util.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * @Auhor: wxc
 * @Date: 2025年3月25日19点40分
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt.token") // 配置读取属性的前缀
public class JwtUtils {

    // 以下2个值, 会读取application.yaml中配置的属性
    private static long tokenExpiration = 86400000; //有效时间,单位毫秒 1000毫秒 == 1秒

    private static String tokenSignKey = "online judge";  //当前程序签名秘钥

    //定义token返回头部
    public static final String AUTH_HEADER_KEY = "Authorization";

    //token前缀
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * jjwt 0.12+ 要求密钥长度满足算法要求；对配置字符串做 SHA-512 派生，保证 HS512 可用。
     * 注意：密钥派生方式变更后，旧 token 会失效，需要重新登录。
     */
    private static SecretKey getSigningKey() {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-512")
                    .digest(tokenSignKey.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }

    /**
     * 给特定ID的用户生成token字符串
     * @param userId
     * @return
     */
    public static String createToken(Long userId) {
        return Jwts.builder()
                .subject("online judge")
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration * 1000 * 60)) //单位分钟
                .claim("userId", userId)
                .signWith(getSigningKey())
                .compressWith(Jwts.ZIP.GZIP)
                .compact();
    }

    /**
     * 从token字符串获取userid
     * @param token
     * @return
     */
    public Long getUserId(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer userId = (Integer) claims.get("userId");
        return userId.longValue();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    public static boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token 已过期");
        } catch (SignatureException e) {
            System.out.println("Token 签名无效");
        } catch (Exception e) {
            System.out.println("Token 验证出错: " + e.getMessage());
        }
        return false;
    }

}
