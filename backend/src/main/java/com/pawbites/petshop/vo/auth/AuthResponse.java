package com.pawbites.petshop.vo.auth;

/**
 * 认证响应
 * <p>登录或注册成功后返回，前端凭 token 调用需登录的接口。</p>
 */
public record AuthResponse(
    /** JWT 访问令牌（后续请求置于 Authorization 头中携带） */
    String token,
    /** 当前登录用户信息 */
    UserProfileVO user,
    /** 登录用户角色（USER-普通用户 ADMIN-管理员），与 user.role 一致，便于后台登录门直接读取 */
    String role
) {
}
