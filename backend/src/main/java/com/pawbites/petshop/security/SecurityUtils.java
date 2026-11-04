package com.pawbites.petshop.security;

import com.pawbites.petshop.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
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
 * 游客标识在服务端统一做格式校验（{@link #GUEST_ID_PATTERN}），不合法直接抛出业务异常，
 * 防止伪造任意字符串或超长值直写数据库；请求头缺失或为空白时仍返回 {@code null}（视为未提供身份）。
 * 控制器无需接收 HttpServletRequest，直接调用本工具类的静态方法即可取得当前请求者身份。
 */
public final class SecurityUtils {

  /**
   * 游客标识合法格式：8-80 位字母、数字或连字符。
   * <p>兼容标准 UUID（36 位，含连字符，前端使用 crypto.randomUUID() 生成），
   * 上限 80 与 cart / orders 表 guest_id VARCHAR(80) 列长一致，杜绝超长写入。</p>
   */
  private static final Pattern GUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{8,80}$");

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
   * <p>内部通过 {@link RequestContextHolder} 取当前线程绑定的请求，再解析其 X-Guest-Id 请求头；
   * 请求头存在时按 {@link #GUEST_ID_PATTERN} 校验格式。</p>
   *
   * @return 去除首尾空白且通过格式校验的游客标识；不在 Web 请求上下文中，或请求头缺失、为空白时返回 {@code null}
   * @throws BusinessException 游客标识格式不合法（400），由 GlobalExceptionHandler 统一接管
   */
  public static String guestId() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return guestId(attributes.getRequest());
    }
    return null;
  }

  /**
   * 从指定请求头 X-Guest-Id 解析游客标识。
   * <p>请求头存在时按 {@link #GUEST_ID_PATTERN} 校验格式（8-80 位字母/数字/连字符，兼容 UUID），
   * 服务端拒绝任意伪造与超长标识。</p>
   *
   * @param request 当前请求
   * @return 去除首尾空白且通过格式校验的游客标识；请求头缺失或为空白时返回 {@code null}
   * @throws BusinessException 游客标识格式不合法（400），由 GlobalExceptionHandler 统一接管
   */
  public static String guestId(HttpServletRequest request) {
    String guestId = request.getHeader("X-Guest-Id");
    return StringUtils.hasText(guestId) ? validateGuestId(guestId) : null;
  }

  /**
   * 校验游客标识格式：必须匹配 {@link #GUEST_ID_PATTERN}（8-80 位字母/数字/连字符）。
   *
   * @param guestId 原始请求头值
   * @return 去除首尾空白且校验通过的游客标识
   * @throws BusinessException 游客标识格式不合法（400）
   */
  private static String validateGuestId(String guestId) {
    String trimmed = guestId.trim();
    if (!GUEST_ID_PATTERN.matcher(trimmed).matches()) {
      throw new BusinessException(400, "游客标识格式不合法");
    }
    return trimmed;
  }
}
