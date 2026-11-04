package com.pawbites.petshop.auth;

public record UserProfileVO(
    Long id,
    String username,
    String email,
    String nickname
) {
}
