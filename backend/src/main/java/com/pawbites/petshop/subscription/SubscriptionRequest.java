package com.pawbites.petshop.subscription;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
    @NotBlank @Email String email
) {
}
