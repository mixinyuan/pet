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

/**
 * 订阅服务实现
 * <p>subscribe 在事务中执行：邮箱统一转小写后查库，不存在则新建有效订阅，
 * 已存在但非有效状态则重新激活，已有效的重复提交保持不变（幂等）。</p>
 */
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
