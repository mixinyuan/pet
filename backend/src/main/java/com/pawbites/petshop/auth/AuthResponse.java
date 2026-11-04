package com.pawbites.petshop.auth;

public record AuthResponse(
    String token,
    UserProfileVO user
) {
}
