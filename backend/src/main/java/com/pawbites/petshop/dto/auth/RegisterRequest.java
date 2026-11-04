package com.pawbites.petshop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String username,
    @Email String email,
    @NotBlank String password,
    String nickname
) {
}
