package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Subscription;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

public interface SubscriptionService extends IService<Subscription> {

  SubscriptionVO subscribe(SubscriptionRequest request);
}
