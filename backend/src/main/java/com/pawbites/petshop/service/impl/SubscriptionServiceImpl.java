package com.pawbites.petshop.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class SubscriptionServiceImpl extends ServiceImpl<SubscriptionMapper, Subscription> implements SubscriptionService {

  @Transactional
  @Override
  public SubscriptionVO subscribe(SubscriptionRequest request) {
    String email = request.email().trim().toLowerCase();
    Subscription subscription = baseMapper.selectOne(Wrappers.<Subscription>lambdaQuery()
        .eq(Subscription::getEmail, email)
        .last("LIMIT 1"));

    if (subscription == null) {
      subscription = new Subscription();
      subscription.setEmail(email);
      subscription.setStatus(StatusConstants.ACTIVE);
      baseMapper.insert(subscription);
    } else if (!StatusConstants.ACTIVE.equals(subscription.getStatus())) {
      subscription.setStatus(StatusConstants.ACTIVE);
      baseMapper.updateById(subscription);
    }

    return new SubscriptionVO(subscription.getId(), subscription.getEmail(), subscription.getStatus());
  }
}
