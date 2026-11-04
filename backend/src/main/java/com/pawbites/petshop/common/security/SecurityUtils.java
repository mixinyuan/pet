package com.pawbites.petshop.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

public final class SecurityUtils {

  private SecurityUtils() {
  }

  public static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
      return null;
    }
    return principal.userId();
  }

  public static String guestId(HttpServletRequest request) {
    String guestId = request.getHeader("X-Guest-Id");
    return StringUtils.hasText(guestId) ? guestId.trim() : null;
  }
}
