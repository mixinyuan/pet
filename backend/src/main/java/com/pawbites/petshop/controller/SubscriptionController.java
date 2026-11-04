package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.SubscriptionService;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

/**
 * 邮件订阅控制器
 * <p>路由前缀 /api/subscriptions，提供邮箱订阅接口。</p>
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

  /** 邮件订阅服务 */
  @Resource
  private SubscriptionService subscriptionService;

  /**
   * 提交邮箱订阅（POST /api/subscriptions）
   *
   * @param request 订阅请求（email 订阅邮箱，须为合法邮箱格式）
   * @return 订阅结果（id、订阅邮箱、状态）
   */
  @PostMapping
  public ApiResponse<SubscriptionVO> subscribe(@Valid @RequestBody SubscriptionRequest request) {
    return ApiResponse.success(subscriptionService.subscribe(request));
  }
}
