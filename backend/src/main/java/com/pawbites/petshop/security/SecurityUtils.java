package com.pawbites.petshop.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * 安全工具类：提供获取“当前请求者身份”的静态方法。
 *
 * <p>登录用户：从 SecurityContext 中的 {@link AuthPrincipal} 取用户 id；
 * 游客：从请求头 X-Guest-Id 取游客标识，用于购物车等对游客开放的接口区分不同游客的数据。
 */
public final class SecurityUtils {

  /** 工具类，禁止实例化。 */
  private SecurityUtils() {
  }

  /**
   * 获取当前登录用户 id。
   *
   * @return 当前认证主体的用户 id；未登录或主体不是 {@link AuthPrincipal} 时返回 {@code null}
   */
  public static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
      return null;
    }
    return principal.userId();
  }

  /**
   * 从请求头 X-Guest-Id 获取游客标识。
   *
   * @param request 当前请求
   * @return 去除首尾空白后的游客标识；请求头缺失或为空白时返回 {@code null}
   */
  public static String guestId(HttpServletRequest request) {
    String guestId = request.getHeader("X-Guest-Id");
    return StringUtils.hasText(guestId) ? guestId.trim() : null;
  }
}
