package com.pawbites.petshop.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求
 * <p>账号支持用户名或邮箱，登录成功后返回 JWT 令牌及用户信息。</p>
 */
public record LoginRequest(
    /** 登录账号（用户名或邮箱，最长 128 字符） */
    @NotBlank
    @Size(max = 128, message = "账号长度不能超过 128 字符")
    String username,
    /** 登录密码（明文传输，服务端与 BCrypt 密文比对，最长 72 字符） */
    @NotBlank
    @Size(max = 72, message = "密码长度不能超过 72 字符")
    String password
) {
}
