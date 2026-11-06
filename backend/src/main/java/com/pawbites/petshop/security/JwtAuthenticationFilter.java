package com.pawbites.petshop.security;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.mapper.UserMapper;
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
 * <p>权限按令牌携带的角色授予：role=ADMIN 授予 ROLE_ADMIN，否则授予 ROLE_USER
 * （旧令牌无 role claim 时由 JwtService 默认按 USER 解析），由 SecurityConfig 的授权规则消费。
 *
 * <p>账号状态校验：令牌验签通过后按用户 id 回查一次 users 表，
 * 用户已被禁用（INACTIVE）或已删除（逻辑删除由 MyBatis-Plus 自动过滤）时不写入认证信息，
 * 即禁用后已签发的旧令牌立即失效，按未认证处理（401）。游客链路（X-Guest-Id）不携带
 * Bearer 令牌，本过滤器不做任何处理，不受影响。
 *
 * <p>令牌缺失或无效时不抛异常、不中断请求，仅保持匿名身份，
 * 由 SecurityConfig 中的授权规则决定最终放行或拒绝。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** JWT 令牌服务（解析与校验 Bearer 令牌） */
  @Resource
  private JwtService jwtService;
  /** 用户表 Mapper：认证时回查账号状态，被禁用用户的旧令牌按未认证处理 */
  @Resource
  private UserMapper userMapper;

  /**
   * 拦截每个请求：若携带合法的 Bearer 令牌（前缀忽略大小写比较）且回查账号仍为有效状态，
   * 则以解析出的主体构造认证信息（权限按角色授予 ROLE_ADMIN / ROLE_USER）写入 SecurityContext；
   * 无论是否认证成功都继续执行过滤链。
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
      // 回查账号状态：每次认证请求查一次 user 表，项目规模下可接受；被禁用/已删除用户视为未认证
      if (principal != null && isActiveUser(principal.userId())) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            // Spring Security 的 hasRole("ADMIN") 匹配权限串 ROLE_ADMIN，此处按令牌角色拼接授予
            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * 回查用户是否为有效（ACTIVE）状态。
   * <p>selectById 自动附带逻辑删除条件（deleted=0），已删除用户同样返回 false。</p>
   *
   * @param userId 令牌解析出的用户 id
   * @return 用户存在且状态为 ACTIVE 返回 true；不存在、已删除或已禁用返回 false
   */
  private boolean isActiveUser(Long userId) {
    User user = userMapper.selectById(userId);
    return user != null && StatusConstants.ACTIVE.equals(user.getStatus());
  }
}
