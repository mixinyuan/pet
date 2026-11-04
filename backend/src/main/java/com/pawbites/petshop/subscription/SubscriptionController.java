package com.pawbites.petshop.subscription;

import com.pawbites.petshop.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping
  public ApiResponse<SubscriptionVO> subscribe(@Valid @RequestBody SubscriptionRequest request) {
    return ApiResponse.success(subscriptionService.subscribe(request));
  }
}
