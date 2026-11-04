package com.pawbites.petshop.vo.auth;

public record UserProfileVO(
    Long id,
    String username,
    String email,
    String nickname
) {
}
