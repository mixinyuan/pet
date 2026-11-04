package com.pawbites.petshop.subscription;

public record SubscriptionVO(
    Long id,
    String email,
    String status
) {
}
