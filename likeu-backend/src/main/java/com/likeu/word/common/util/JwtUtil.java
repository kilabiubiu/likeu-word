package com.likeu.word.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（使用 HMAC-SHA256 实现，无需第三方库）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-seconds}")
    private Long expireSeconds;

    /**
     * 生成JWT token
     */
    public String createToken(Long userId) {
        long now = System.currentTimeMillis();
        long exp = now + expireSeconds * 1000;

        // Header
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        // Payload
        String payloadJson = "{\"uid\":" + userId + ",\"iat\":" + (now / 1000) + ",\"exp\":" + (exp / 1000) + "}";
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Signature
        String signature = hmacSha256(header + "." + payload, secret);

        return header + "." + payload + "." + signature;
    }

    /**
     * 解析token，返回payload中的claims；失败返回null
     */
    public Map<String, Object> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // 验证签名
            String expectedSignature = hmacSha256(header + "." + payload, secret);
            if (!signature.equals(expectedSignature)) return null;

            // 解析payload
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            Map<String, Object> claims = new HashMap<>();

            // 简单JSON解析（只解析数字和字符串）
            json = json.replaceAll("[{}\"]", "");
            for (String entry : json.split(",")) {
                String[] kv = entry.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    if (value.matches("-?\\d+(\\.\\d+)?")) {
                        claims.put(key, Long.valueOf(value));
                    } else {
                        claims.put(key, value);
                    }
                }
            }

            // 检查过期
            Object expObj = claims.get("exp");
            if (expObj != null) {
                long exp = ((Number) expObj).longValue() * 1000;
                if (System.currentTimeMillis() > exp) return null;
            }

            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从token中获取userId
     */
    public Long getUserId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) return null;
        Object uid = claims.get("uid");
        return uid == null ? null : ((Number) uid).longValue();
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }
}