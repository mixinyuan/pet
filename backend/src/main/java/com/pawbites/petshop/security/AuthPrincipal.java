package com.pawbites.petshop.security;

/**
 * 认证主体：JWT 解析成功后封装的当前用户信息，
 * 作为 UsernamePasswordAuthenticationToken 的 principal 存入 SecurityContext。
 *
 * @param userId   用户 id
 * @param username 用户名（登录名）
 * @param nickname 昵称（展示名）
 * @param role     角色（ADMIN-管理员 USER-普通用户；旧 token 无 role claim 时默认 USER）
 */
public record AuthPrincipal(Long userId, String username, String nickname, String role) {
}
