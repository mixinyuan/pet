package com.pawbites.petshop.service;

import com.pawbites.petshop.dto.auth.LoginRequest;
import com.pawbites.petshop.dto.auth.RegisterRequest;
import com.pawbites.petshop.vo.auth.AuthResponse;
import com.pawbites.petshop.vo.auth.UserProfileVO;

public interface AuthService {

  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  UserProfileVO currentUser(Long userId);
}
