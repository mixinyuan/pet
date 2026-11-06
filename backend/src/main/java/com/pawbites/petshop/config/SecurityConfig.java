package com.pawbites.petshop.config;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.JwtAuthenticationFilter;

/**
 * Spring Security 安全配置。
 *
 * <p>整体采用无状态（STATELESS）JWT 会话：不创建 HttpSession、不使用 Cookie 会话，
 * 由 {@link JwtAuthenticationFilter} 在用户名密码认证过滤器之前解析 Bearer 令牌完成认证；
 * 同时关闭 CSRF（纯令牌认证无 CSRF 风险）、启用 CORS、密码以 BCrypt 加密存储。
 *
 * <p>放行策略（permitAll）：登录/注册/登出、服务探活、商品/分类/套餐/评价的只读浏览、订阅提交；
 * 购物车与订单接口对游客开放（游客数据通过 X-Guest-Id 请求头关联）；其余接口一律要求认证。
 *
 * <p>认证/授权失败响应：未认证（401）与无权访问（403）由 exceptionHandling 统一输出
 * {@link ApiResponse} JSON，而非默认的空体/HTML，便于前端按统一结构处理错误。
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  /** Jackson 序列化器：用于将认证/授权失败的 ApiResponse 写为 JSON 响应体。 */
  @Resource
  private ObjectMapper objectMapper;

  /**
   * 构建安全过滤链。
   *
   * <p>放行范围（permitAll）：
   * <ul>
   *   <li>POST /api/auth/login、/api/auth/register、/api/auth/logout —— 登录、注册与登出
   *       （登出为无状态接口，无需携带有效令牌即可调用，故一并放行）；</li>
   *   <li>GET /api、/api/health —— 服务探活；</li>
   *   <li>GET /api/categories/**、/api/products/**、/api/bundles/**、/api/reviews/** —— 商品类只读浏览；</li>
   *   <li>POST /api/subscriptions —— 订阅提交；</li>
   *   <li>/api/cart/**、/api/orders/** —— 购物车与订单，对游客开放。</li>
   * </ul>
   * 日志管理接口已迁移至 /api/admin/logs（随 /api/admin/** 统一要求 ADMIN 角色，
   * 修复旧 /api/logs 仅要求登录的越权隐患）；收藏接口 /api/favorites/**、优惠券接口
   * /api/coupons/** 显式要求认证（Bearer token，无角色体系，游客不可访问——游客不可收藏、不可持券/用券）；
   * 后台管理接口 /api/admin/** 要求 ADMIN 角色（普通登录用户返回 403，游客/未认证返回 401）；
   * 其余请求（anyRequest）均要求认证；会话策略为 STATELESS；
   * 未认证访问受保护接口返回 401（{"code":401,"message":"请先登录"}），
   * 认证后权限不足返回 403（{"code":403,"message":"无权访问"}），均为 ApiResponse JSON；
   * JWT 过滤器挂载在 UsernamePasswordAuthenticationFilter 之前。
   *
   * @param http                    HttpSecurity 构建器
   * @param jwtAuthenticationFilter JWT 认证过滤器
   * @return 配置完成的 SecurityFilterChain
   * @throws Exception 过滤链构建失败时抛出
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> {
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(handling -> handling
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpStatus.UNAUTHORIZED.value());
              response.setContentType("application/json;charset=UTF-8");
              response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, "请先登录")));
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(HttpStatus.FORBIDDEN.value());
              response.setContentType("application/json;charset=UTF-8");
              response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(403, "无权访问")));
            })
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
            .requestMatchers(HttpMethod.GET, "/api", "/api/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/products/**", "/api/bundles/**", "/api/reviews/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
            .requestMatchers("/api/cart/**", "/api/orders/**").permitAll()
            // 后台管理接口：统一 /api/admin 前缀，仅管理员角色可访问
            //（hasRole("ADMIN") 匹配权限串 ROLE_ADMIN，由 JwtAuthenticationFilter 按令牌角色授予；
            // 含自 /api/logs 迁移而来的日志管理 /api/admin/logs，旧 /api/logs 已下线）
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            // 收藏与优惠券接口：需登录（游客不可收藏、不可持券/用券，前端引导登录）
            .requestMatchers("/api/favorites/**", "/api/coupons/**").authenticated()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * CORS 配置源：应用于所有路径（/**）。
   *
   * <p>未配置允许来源时默认放行所有来源（allowedOriginPatterns=*）；
   * 允许常见 HTTP 方法与任意请求头；暴露 Authorization 响应头供前端读取令牌；
   * 不携带凭证（allowCredentials=false）。
   *
   * @param properties CORS 配置属性（前缀 app.cors）
   * @return CORS 配置源
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        properties.allowedOriginPatterns() == null || properties.allowedOriginPatterns().isEmpty()
            ? List.of("*")
            : properties.allowedOriginPatterns()
    );
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * 密码编码器：BCrypt（自带随机盐），用于用户密码的加密存储与登录比对。
   *
   * @return BCryptPasswordEncoder 实例
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
