package com.pawbites.petshop.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求
 * <p>账号支持用户名或邮箱，登录成功后返回 JWT 令牌及用户信息。</p>
 */
public record LoginRequest(
    /** 登录账号（用户名或邮箱） */
    @NotBlank
    String username,
    /** 登录密码（明文传输，服务端与 BCrypt 密文比对） */
    @NotBlank
    String password
) {
}
