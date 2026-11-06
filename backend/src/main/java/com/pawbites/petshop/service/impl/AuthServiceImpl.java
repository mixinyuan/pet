package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.AuthService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.auth.LoginRequest;
import com.pawbites.petshop.dto.auth.RegisterRequest;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.UserMapper;
import com.pawbites.petshop.security.JwtService;
import com.pawbites.petshop.service.CouponService;
import com.pawbites.petshop.vo.auth.AuthResponse;
import com.pawbites.petshop.vo.auth.UserProfileVO;

/**
 * 认证服务实现
 * <p>注册走事务：先按用户名/邮箱查重，密码经 BCrypt 加密后入库，
 * 入库成功后自动发放一张「新人专享券」（满99减15、30 天有效，每人限领一张；
 * 发券为 best-effort，异常不回滚注册）；登录支持用户名或邮箱 + 密码比对，
 * 密码正确后再校验账号状态，被禁用（INACTIVE）账号报「账号已被禁用」；
 * 用户不存在时同样执行一次与占位密文的 BCrypt 比对再抛出统一错误，拉平响应时间以防账号枚举；
 * currentUser 按 id 回查并校验状态，失效时返回 401。</p>
 */
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  /** 占位密文：用户不存在时用它执行一次等长的 BCrypt 比对以拉平响应时间，防止通过时间差枚举账号 */
  private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

  /** 密码加密器（BCrypt，负责密码加密与比对） */
  @Resource
  private PasswordEncoder passwordEncoder;
  /** JWT 令牌服务（签发登录令牌） */
  @Resource
  private JwtService jwtService;
  /** 优惠券服务（注册成功后发放新人专享券） */
  @Resource
  private CouponService couponService;

  @Transactional
  @Override
  public AuthResponse register(RegisterRequest request) {
    String username = request.username().trim();
    String email = StringUtils.hasText(request.email()) ? request.email().trim().toLowerCase() : null;

    if (baseMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
      throw new BusinessException("账号已存在");
    }
    if (StringUtils.hasText(email) && baseMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email)) > 0) {
      throw new BusinessException("邮箱已被注册");
    }

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
    user.setStatus(StatusConstants.ACTIVE);
    // 注册用户固定为普通用户角色（管理员由 SQL 手工指定，不开放注册管理员）
    user.setRole(StatusConstants.ROLE_USER);
    baseMapper.insert(user);
    // 注册成功自动发放新人专享券（满99减15、30 天有效，每人限领一张）。
    // best-effort：发券异常仅告警不回滚，注册主流程可用性优先（券可由订阅入口补发）
    try {
      couponService.issueNewUserCoupon(user.getId());
    } catch (RuntimeException exception) {
      log.warn("用户 {} 注册成功但新人券发放失败：{}", user.getId(), exception.getMessage());
    }
    return buildAuthResponse(user);
  }

  /**
   * 用户名或邮箱 + 密码登录
   * <p>查询不限账号状态，密码比对通过后再校验状态：被禁用（INACTIVE）账号报「账号已被禁用」，
   * 便于用户知晓无法登录的原因；用户不存在时也执行一次与占位密文的 BCrypt 比对再抛出统一错误，
   * 使"账号不存在"与"密码错误"两条路径的响应耗时一致，防止账号枚举。</p>
   */
  @Override
  public AuthResponse login(LoginRequest request) {
    String account = request.username().trim();
    User user = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
        .and(wrapper -> wrapper.eq(User::getUsername, account).or().eq(User::getEmail, account.toLowerCase()))
        .last("LIMIT 1"));

    if (user == null) {
      // 用户不存在：先消耗一次等长的 BCrypt 比对再抛错，拉平响应时间以防账号枚举
      passwordEncoder.matches(request.password(), DUMMY_HASH);
      throw new BusinessException("账号或密码错误");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException("账号或密码错误");
    }
    // 密码正确但账号已被管理员禁用：明确提示而非笼统的「账号或密码错误」
    if (!StatusConstants.ACTIVE.equals(user.getStatus())) {
      throw new BusinessException("账号已被禁用");
    }
    return buildAuthResponse(user);
  }

  @Override
  public UserProfileVO currentUser(Long userId) {
    if (userId == null) {
      throw new BusinessException(401, "请先登录");
    }
    User user = baseMapper.selectById(userId);
    if (user == null || !StatusConstants.ACTIVE.equals(user.getStatus())) {
      throw new BusinessException(401, "登录状态已失效");
    }
    return toProfile(user);
  }

  /** 组装认证响应：签发 JWT 令牌并附带用户资料（含角色，后台登录门据此判断是否有权进入管理端） */
  private AuthResponse buildAuthResponse(User user) {
    UserProfileVO profile = toProfile(user);
    return new AuthResponse(jwtService.generateToken(user), profile, profile.role());
  }

  /** 用户实体转用户资料视图 */
  private UserProfileVO toProfile(User user) {
    // 角色非 ADMIN 时一律按普通用户输出，防脏值透出（与 JwtService 签发令牌的口径一致）
    String role = StatusConstants.ROLE_ADMIN.equals(user.getRole())
        ? StatusConstants.ROLE_ADMIN
        : StatusConstants.ROLE_USER;
    return new UserProfileVO(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(), role);
  }
}
