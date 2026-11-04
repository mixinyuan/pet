package com.pawbites.petshop.security;
import jakarta.annotation.Resource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器：从请求头 {@code Authorization: Bearer <token>} 中取出令牌
 * （Bearer 前缀大小写不敏感，兼容 "bearer"/"BEARER" 等写法），
 * 校验通过后以 {@link AuthPrincipal} 为主体构造认证信息并写入 SecurityContext，
 * 供后续业务代码通过 SecurityUtils 获取当前登录用户。
 *
 * <p>令牌缺失或无效时不抛异常、不中断请求，仅保持匿名身份，
 * 由 SecurityConfig 中的授权规则决定最终放行或拒绝。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Resource
  private JwtService jwtService;

  /**
   * 拦截每个请求：若携带合法的 Bearer 令牌（前缀忽略大小写比较），则以解析出的主体构造认证信息
   * （权限固定为 ROLE_USER）写入 SecurityContext；无论是否认证成功都继续执行过滤链。
   *
   * @param request     当前请求
   * @param response    当前响应
   * @param filterChain 过滤链
   * @throws ServletException 过滤链执行异常
   * @throws IOException      过滤链执行 I/O 异常
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (StringUtils.hasText(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      AuthPrincipal principal = jwtService.parseToken(authorization.substring("Bearer ".length()));
      if (principal != null) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }
    filterChain.doFilter(request, response);
  }
}
