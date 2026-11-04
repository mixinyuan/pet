package com.pawbites.petshop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求
 * <p>用户名与邮箱均要求唯一，昵称为空时默认以用户名填充。</p>
 */
public record RegisterRequest(
    /** 用户名（全局唯一，服务端去除首尾空白后查重） */
    @NotBlank String username,
    /** 邮箱（格式须合法且全局唯一，可选填，存储时统一转小写） */
    @Email String email,
    /** 登录密码（明文传输，服务端以 BCrypt 加密后存储） */
    @NotBlank String password,
    /** 昵称（可选，前端展示用，为空时默认取用户名） */
    String nickname
) {
}
