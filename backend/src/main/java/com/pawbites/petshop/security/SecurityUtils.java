package com.pawbites.petshop.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全工具类：提供获取“当前请求者身份”的静态方法。
 *
 * <p>登录用户：从 SecurityContext 中的 {@link AuthPrincipal} 取用户 id；
 * 游客：从当前线程绑定请求的 X-Guest-Id 请求头取游客标识，用于购物车等对游客开放的接口区分不同游客的数据。
 * 控制器无需接收 HttpServletRequest，直接调用本工具类的静态方法即可取得当前请求者身份。
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
   * 获取当前请求的游客标识（无需调用方传递 HttpServletRequest）。
   * <p>内部通过 {@link RequestContextHolder} 取当前线程绑定的请求，再解析其 X-Guest-Id 请求头。</p>
   *
   * @return 去除首尾空白后的游客标识；不在 Web 请求上下文中，或请求头缺失、为空白时返回 {@code null}
   */
  public static String guestId() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return guestId(attributes.getRequest());
    }
    return null;
  }

  /**
   * 从指定请求头 X-Guest-Id 解析游客标识。
   *
   * @param request 当前请求
   * @return 去除首尾空白后的游客标识；请求头缺失或为空白时返回 {@code null}
   */
  public static String guestId(HttpServletRequest request) {
    String guestId = request.getHeader("X-Guest-Id");
    return StringUtils.hasText(guestId) ? guestId.trim() : null;
  }
}
