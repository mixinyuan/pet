package com.pawbites.petshop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求
 * <p>用户名与邮箱均要求唯一，昵称为空时默认以用户名填充。</p>
 */
public record RegisterRequest(
    /** 用户名（全局唯一，服务端去除首尾空白后查重，长度 3-64 位） */
    @NotBlank
    @Size(min = 3, max = 64, message = "用户名长度须为 3-64 位")
    String username,
    /** 邮箱（格式须合法且全局唯一，可选填，存储时统一转小写，最长 128 字符） */
    @Email
    @Size(max = 128, message = "邮箱长度不能超过 128 字符")
    String email,
    /** 登录密码（明文传输，服务端以 BCrypt 加密后存储，长度 6-72 位） */
    @NotBlank
    @Size(min = 6, max = 72, message = "密码长度须为 6-72 位")
    String password,
    /** 昵称（可选，前端展示用，为空时默认取用户名，最长 64 字符） */
    @Size(max = 64, message = "昵称长度不能超过 64 字符")
    String nickname
) {
}
