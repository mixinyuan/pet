package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.auth.LoginRequest;
import com.pawbites.petshop.dto.auth.RegisterRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.AuthService;
import com.pawbites.petshop.vo.auth.AuthResponse;
import com.pawbites.petshop.vo.auth.UserProfileVO;

/**
 * 认证控制器
 * <p>路由前缀 /api/auth，提供用户注册、登录、登出与当前用户信息接口。
 * 登录成功返回 JWT，后续接口通过 Authorization: Bearer 请求头携带令牌。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /** 认证服务 */
  @Resource
  private AuthService authService;

  /**
   * 注册新用户（POST /api/auth/register）
   *
   * @param request 注册请求（username 账号、email 邮箱、password 密码、nickname 昵称）
   * @return 注册结果，含 JWT 令牌与用户资料
   * @throws BusinessException 账号已存在或邮箱已被注册时抛出
   */
  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }

  /**
   * 用户登录（POST /api/auth/login）
   *
   * @param request 登录请求（username 账号、password 密码）
   * @return 登录结果，含 JWT 令牌与用户资料
   * @throws BusinessException 账号或密码错误时抛出
   */
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  /**
   * 登出（POST /api/auth/logout）
   * <p>JWT 为无状态令牌，服务端不存储会话，直接返回成功，由客户端丢弃令牌即可。</p>
   */
  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.success();
  }

  /**
   * 查询当前登录用户信息（GET /api/auth/me）
   *
   * @return 当前登录用户资料（id、账号、邮箱、昵称）
   * @throws BusinessException 未登录（401 请先登录）或登录状态已失效（401）时抛出
   */
  @GetMapping("/me")
  public ApiResponse<UserProfileVO> me() {
    return ApiResponse.success(authService.currentUser(SecurityUtils.currentUserId()));
  }
}
