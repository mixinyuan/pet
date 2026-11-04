package com.pawbites.petshop.service.impl;
import com.pawbites.petshop.service.SubscriptionService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.entity.Subscription;
import com.pawbites.petshop.mapper.SubscriptionMapper;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

  private final SubscriptionMapper subscriptionMapper;

  public SubscriptionServiceImpl(SubscriptionMapper subscriptionMapper) {
    this.subscriptionMapper = subscriptionMapper;
  }

  @Transactional
  @Override
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
