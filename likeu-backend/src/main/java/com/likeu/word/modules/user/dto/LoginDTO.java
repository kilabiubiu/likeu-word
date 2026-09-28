package com.likeu.word.modules.user.dto;

import lombok.Data;

/**
 * 登录请求DTO
 *
 * <p>{@code code} 是可选的：经云托管 {@code callContainer} 调用时后端直接从请求头拿到 openid，
 * 不需要 wx.login 换取的 code；只有本地开发、普通 HTTP 客户端才必须携带 code。</p>
 */
@Data
public class LoginDTO {

    private String code;

    private String nickname;

    private String avatar;
}