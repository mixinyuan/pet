package com.pawbites.petshop.service;

import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

public interface SubscriptionService {

  SubscriptionVO subscribe(SubscriptionRequest request);
}
