package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.AuthService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.pawbites.petshop.vo.auth.AuthResponse;
import com.pawbites.petshop.vo.auth.UserProfileVO;

@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

  @Resource
  private PasswordEncoder passwordEncoder;
  @Resource
  private JwtService jwtService;

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
    baseMapper.insert(user);
    return buildAuthResponse(user);
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    String account = request.username().trim();
    User user = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
        .and(wrapper -> wrapper.eq(User::getUsername, account).or().eq(User::getEmail, account.toLowerCase()))
        .eq(User::getStatus, StatusConstants.ACTIVE)
        .last("LIMIT 1"));

    if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException("账号或密码错误");
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

  private AuthResponse buildAuthResponse(User user) {
    return new AuthResponse(jwtService.generateToken(user), toProfile(user));
  }

  private UserProfileVO toProfile(User user) {
    return new UserProfileVO(user.getId(), user.getUsername(), user.getEmail(), user.getNickname());
  }
}
