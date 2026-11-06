package com.pawbites.petshop.vo.auth;

/**
 * 用户信息
 * <p>不含密码等敏感字段，用于前端展示当前登录用户资料。</p>
 */
public record UserProfileVO(
    /** 用户 ID */
    Long id,
    /** 用户名（全局唯一） */
    String username,
    /** 邮箱（全局唯一，可为空） */
    String email,
    /** 昵称（前端优先展示的称呼，可为空） */
    String nickname,
    /** 角色（USER-普通用户 ADMIN-管理员），后台登录门据此判断是否有权进入管理端 */
    String role
) {
}
