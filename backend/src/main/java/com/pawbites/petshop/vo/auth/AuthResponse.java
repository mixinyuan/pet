package com.pawbites.petshop.vo.auth;

public record AuthResponse(
    String token,
    UserProfileVO user
) {
}
