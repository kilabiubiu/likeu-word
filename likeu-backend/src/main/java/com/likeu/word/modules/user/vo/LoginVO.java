package com.likeu.word.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 微信登录返回VO
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private Long userId;
    private String nickname;
    private String avatar;
}