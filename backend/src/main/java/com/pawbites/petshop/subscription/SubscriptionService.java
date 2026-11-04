package com.pawbites.petshop.subscription;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawbites.petshop.common.config.StatusConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

  private final SubscriptionMapper subscriptionMapper;

  public SubscriptionService(SubscriptionMapper subscriptionMapper) {
    this.subscriptionMapper = subscriptionMapper;
  }

  @Transactional
  public SubscriptionVO subscribe(SubscriptionRequest request) {
    String email = request.email().trim().toLowerCase();
    Subscription subscription = subscriptionMapper.selectOne(Wrappers.<Subscription>lambdaQuery()
        .eq(Subscription::getEmail, email)
        .last("LIMIT 1"));

    if (subscription == null) {
      subscription = new Subscription();
      subscription.setEmail(email);
      subscription.setStatus(StatusConstants.ACTIVE);
      subscriptionMapper.insert(subscription);
    } else if (!StatusConstants.ACTIVE.equals(subscription.getStatus())) {
      subscription.setStatus(StatusConstants.ACTIVE);
      subscriptionMapper.updateById(subscription);
    }

    return new SubscriptionVO(subscription.getId(), subscription.getEmail(), subscription.getStatus());
  }
}
