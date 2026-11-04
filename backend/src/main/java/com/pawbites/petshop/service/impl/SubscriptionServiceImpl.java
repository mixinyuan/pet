package com.pawbites.petshop.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.SubscriptionService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.subscription.SubscriptionRequest;
import com.pawbites.petshop.entity.Subscription;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.SubscriptionMapper;
import com.pawbites.petshop.vo.subscription.SubscriptionVO;

/**
 * 订阅服务实现
 * <p>subscribe 在事务中执行：邮箱统一转小写后查库，不存在则新建有效订阅，
 * 已存在但非有效状态则重新激活，已有效的重复提交保持不变（幂等）；
 * 并发插入撞 uk_subscription_email 唯一键时改为重查一次该邮箱记录并按已有逻辑返回，
 * 存在即视为订阅成功，保证重复提交的幂等语义。</p>
 */
@Service
public class SubscriptionServiceImpl extends ServiceImpl<SubscriptionMapper, Subscription> implements SubscriptionService {

  @Transactional
  @Override
  public SubscriptionVO subscribe(SubscriptionRequest request) {
    String email = request.email().trim().toLowerCase();
    Subscription subscription = selectByEmail(email);

    if (subscription == null) {
      subscription = new Subscription();
      subscription.setEmail(email);
      subscription.setStatus(StatusConstants.ACTIVE);
      try {
        baseMapper.insert(subscription);
      } catch (DuplicateKeyException exception) {
        // 并发插入撞 uk_subscription_email：用锁定读重查一次该邮箱记录（可重复读快照看不到并发提交行，FOR UPDATE 读最新已提交数据），存在即视为订阅成功
        subscription = baseMapper.selectOne(Wrappers.<Subscription>lambdaQuery()
            .eq(Subscription::getEmail, email)
            .last("LIMIT 1 FOR UPDATE"));
        // 极端时序兜底：撞键行又被并发物理删除导致重查为空，提示重试而非空指针
        if (subscription == null) {
          throw new BusinessException("订阅失败，请稍后重试");
        }
      }
    } else if (!StatusConstants.ACTIVE.equals(subscription.getStatus())) {
      subscription.setStatus(StatusConstants.ACTIVE);
      baseMapper.updateById(subscription);
    }

    return new SubscriptionVO(subscription.getId(), subscription.getEmail(), subscription.getStatus());
  }

  /** 按邮箱查询订阅记录（uk_subscription_email 唯一键约束下最多一条） */
  private Subscription selectByEmail(String email) {
    return baseMapper.selectOne(Wrappers.<Subscription>lambdaQuery()
        .eq(Subscription::getEmail, email)
        .last("LIMIT 1"));
  }
}
