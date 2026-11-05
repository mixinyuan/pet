package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.CouponService;
import com.pawbites.petshop.service.SubscriptionService;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

/**
 * 邮件订阅控制器
 * <p>路由前缀 /api/subscriptions，提供邮箱订阅接口。
 * 接口对游客放行：未登录仅记录邮箱（行为不变）；请求携带有效登录态时，
 * 订阅成功后额外给当前用户发放一张「新人专享券」。</p>
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

  /** 邮件订阅服务 */
  @Resource
  private SubscriptionService subscriptionService;
  /** 优惠券服务（登录态订阅成功后发放新人专享券） */
  @Resource
  private CouponService couponService;

  /**
   * 提交邮箱订阅（POST /api/subscriptions）
   * <p>未登录：仅记录邮箱，行为与历史版本一致；已登录：订阅成功后发放一张新人专享券
   * （满99减15、自发放起 30 天有效，每人限领一张）。</p>
   *
   * @param request 订阅请求（email 订阅邮箱，须为合法邮箱格式）
   * @return 订阅结果（id、订阅邮箱、状态）
   */
  @PostMapping
  public ApiResponse<SubscriptionVO> subscribe(@Valid @RequestBody SubscriptionRequest request) {
    SubscriptionVO subscription = subscriptionService.subscribe(request);
    // 携带有效登录态（Bearer token 经 JwtAuthenticationFilter 解析）时发券；未登录跳过，订阅行为不变。
    // 发券每人限领一张（服务内按用户+模板去重）；发券为 best-effort，异常仅告警，不影响已成功的订阅结果
    Long userId = SecurityUtils.currentUserId();
    if (userId != null) {
      try {
        couponService.issueNewUserCoupon(userId);
      } catch (RuntimeException exception) {
        log.warn("用户 {} 订阅成功但新人券发放失败：{}", userId, exception.getMessage());
      }
    }
    return ApiResponse.success(subscription);
  }
}
