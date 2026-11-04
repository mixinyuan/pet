package com.pawbites.petshop.security;

public record AuthPrincipal(Long userId, String username, String nickname) {
}
