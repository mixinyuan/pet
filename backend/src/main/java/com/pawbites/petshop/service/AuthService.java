package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.User;

import com.pawbites.petshop.dto.auth.LoginRequest;
import com.pawbites.petshop.dto.auth.RegisterRequest;
import com.pawbites.petshop.vo.auth.AuthResponse;
import com.pawbites.petshop.vo.auth.UserProfileVO;

/**
 * 认证服务
 * <p>覆盖注册（用户名/邮箱查重、BCrypt 加密）、登录（用户名或邮箱 + 密码比对）与当前登录用户查询。</p>
 */
public interface AuthService extends IService<User> {

  /**
   * 注册新用户
   * <p>用户名或邮箱已被占用时抛出业务异常；密码经 BCrypt 加密后入库。</p>
   *
   * @param request 注册请求（用户名、密码、邮箱、昵称）
   * @return 认证响应（JWT 令牌 + 用户资料）
   * @throws BusinessException 用户名已存在或邮箱已被注册
   */
  AuthResponse register(RegisterRequest request);

  /**
   * 账号登录
   * <p>账号支持用户名或邮箱，仅匹配有效状态用户。</p>
   *
   * @param request 登录请求（账号 + 密码）
   * @return 认证响应（JWT 令牌 + 用户资料）
   * @throws BusinessException 账号或密码错误
   */
  AuthResponse login(LoginRequest request);

  /**
   * 查询当前登录用户资料
   * <p>用于 JWT 解析出用户 id 后回查用户信息。</p>
   *
   * @param userId 登录用户 id
   * @return 用户资料视图
   * @throws BusinessException 未登录（401）或登录状态已失效（用户不存在/非有效状态）
   */
  UserProfileVO currentUser(Long userId);
}
