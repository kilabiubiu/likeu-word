package com.likeu.word.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信登录请求DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "微信登录code不能为空")
    private String code;

    private String nickname;

    private String avatar;
}