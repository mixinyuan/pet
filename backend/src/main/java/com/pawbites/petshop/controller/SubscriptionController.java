package com.pawbites.petshop.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.SubscriptionService;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

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
