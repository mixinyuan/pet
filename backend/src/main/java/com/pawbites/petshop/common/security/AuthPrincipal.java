package com.pawbites.petshop.common.security;

public record AuthPrincipal(Long userId, String username, String nickname) {
}
