package com.pawbites.petshop.auth;

import com.pawbites.petshop.common.result.ApiResponse;
import com.pawbites.petshop.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }

  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.success();
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileVO> me() {
    return ApiResponse.success(authService.currentUser(SecurityUtils.currentUserId()));
  }
}
