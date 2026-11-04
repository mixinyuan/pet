package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Subscription;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

/**
 * 邮箱订阅服务
 * <p>处理营销邮箱订阅：新邮箱创建订阅记录，已取消的订阅重新激活。</p>
 */
public interface SubscriptionService extends IService<Subscription> {

  /**
   * 邮箱订阅
   * <p>邮箱统一转小写后处理；已订阅且有效的邮箱重复提交时保持不变（幂等）。</p>
   *
   * @param request 订阅请求（邮箱）
   * @return 订阅视图（id、邮箱、状态）
   */
  SubscriptionVO subscribe(SubscriptionRequest request);
}
