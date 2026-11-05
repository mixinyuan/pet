package com.pawbites.petshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.CouponService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawbites.petshop.entity.Coupon;
import com.pawbites.petshop.entity.UserCoupon;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CouponMapper;
import com.pawbites.petshop.mapper.UserCouponMapper;
import com.pawbites.petshop.vo.coupon.CouponVO;

/**
 * 优惠券服务实现
 * <p>issueNewUserCoupon 按名称定位「新人专享券」模板生成持券记录（缺失时告警跳过，
 * 不阻断注册/订阅；每人限领一张，按用户+模板去重，重复订阅不再重复发放）；listCoupons 联查模板后输出懒判定展示态（0/1/2）与 available 标记；
 * validateForOrder → markUsed 由订单服务在同一事务内先后调用，完成归属/未用/未过期/
 * 门槛校验与原子核销（并发抢占失败即抛异常回滚订单）；returnByOrder 供取消订单返还，
 * 简单起见已过期的券也改回未使用，过期与否始终按 expires_at 懒判定。</p>
 */
@Service
public class CouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements CouponService {

  private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);

  /** 新人专享券模板名称（与 V6 种子数据一致，发放按此名称定位模板） */
  private static final String NEW_USER_COUPON_NAME = "新人专享券";

  /** 券模板 Mapper（按名称定位模板、列表联查名称/金额/门槛） */
  private final CouponMapper couponMapper;

  /** 构造注入券模板 Mapper */
  public CouponServiceImpl(CouponMapper couponMapper) {
    this.couponMapper = couponMapper;
  }

  @Override
  public void issueNewUserCoupon(Long userId) {
    if (userId == null) {
      return;
    }
    Coupon template = couponMapper.selectOne(Wrappers.<Coupon>lambdaQuery()
        .eq(Coupon::getName, NEW_USER_COUPON_NAME)
        .last("LIMIT 1"));
    if (template == null) {
      // 种子模板缺失（如种子数据未执行）：告警并跳过，不阻断注册/订阅主流程
      log.warn("新人专享券模板缺失，跳过给用户 {} 发券", userId);
      return;
    }

    // 按用户去重：每人限领一张新人券。注册与登录态订阅两个入口都会走到这里，
    // 重复订阅/重复触发不再重复发放，防止无限刷券资损。
    // （应用层判重存在极小的并发双击竞态窗口，新人券场景可接受；如需严格互斥可后续加发放流水唯一键）
    Long issued = baseMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
        .eq(UserCoupon::getUserId, userId)
        .eq(UserCoupon::getCouponId, template.getId()));
    if (issued != null && issued > 0) {
      log.debug("用户 {} 已领取过新人专享券，跳过重复发放", userId);
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    UserCoupon userCoupon = new UserCoupon();
    userCoupon.setUserId(userId);
    userCoupon.setCouponId(template.getId());
    userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
    userCoupon.setReceivedAt(now);
    userCoupon.setExpiresAt(now.plusDays(template.getValidDays()));
    baseMapper.insert(userCoupon);
  }

  @Override
  public List<CouponVO> listCoupons(Long userId) {
    if (userId == null) {
      throw new BusinessException(401, "请先登录");
    }
    List<UserCoupon> userCoupons = baseMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
        .eq(UserCoupon::getUserId, userId)
        .orderByDesc(UserCoupon::getId));
    if (userCoupons.isEmpty()) {
      return List.of();
    }

    Map<Long, Coupon> templateMap = couponMapper.selectBatchIds(
            userCoupons.stream().map(UserCoupon::getCouponId).distinct().toList()
        )
        .stream()
        .collect(Collectors.toMap(Coupon::getId, Function.identity()));
    LocalDateTime now = LocalDateTime.now();
    return userCoupons.stream()
        // 模板被物理删除的持券记录跳过（当前无删除路径，防御性处理）
        .filter(userCoupon -> templateMap.containsKey(userCoupon.getCouponId()))
        .map(userCoupon -> toCouponVO(userCoupon, templateMap.get(userCoupon.getCouponId()), now))
        .toList();
  }

  @Override
  public BigDecimal validateForOrder(Long userId, Long userCouponId, BigDecimal goodsTotal) {
    if (userId == null) {
      throw new BusinessException(401, "请先登录");
    }
    UserCoupon userCoupon = baseMapper.selectById(userCouponId);
    // 非本人持有的券与不存在的券同样提示「不存在」，不泄露他人券信息
    if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
      throw new BusinessException(404, "优惠券不存在");
    }
    if (userCoupon.getStatus() != null && userCoupon.getStatus() == UserCoupon.STATUS_USED) {
      throw new BusinessException("优惠券已使用");
    }
    if (userCoupon.getExpiresAt() == null || !userCoupon.getExpiresAt().isAfter(LocalDateTime.now())) {
      throw new BusinessException("优惠券已过期");
    }

    Coupon template = couponMapper.selectById(userCoupon.getCouponId());
    if (template == null) {
      throw new BusinessException(404, "优惠券不存在");
    }
    if (goodsTotal.compareTo(BigDecimal.valueOf(template.getMinSpend())) < 0) {
      throw new BusinessException("订单商品总额未满 " + template.getMinSpend() + " 元，该优惠券不可用");
    }

    // 抵扣金额以商品总额封顶，保证扣减后实付不为负（min_spend ≥ amount 的模板天然不会触顶）
    BigDecimal discount = BigDecimal.valueOf(template.getAmount());
    return discount.min(goodsTotal);
  }

  @Override
  public void markUsed(Long userCouponId, Long orderId) {
    // 条件更新抢占核销权：status 0 → 1，影响行数为 0 说明被并发请求抢先核销
    if (baseMapper.markUsed(userCouponId, orderId) == 0) {
      throw new BusinessException("优惠券已被使用，请重新选择");
    }
  }

  @Transactional
  @Override
  public void returnByOrder(Long orderId) {
    // 订单未用券时影响 0 行，静默跳过；已过期的券同样改回未使用（过期按 expires_at 懒判定）
    int returned = baseMapper.returnByOrder(orderId);
    if (returned > 0) {
      log.info("订单 {} 已返还核销的优惠券", orderId);
    }
  }

  /** 持券记录 + 模板组装我的券视图（status 做懒判定展示态换算） */
  private CouponVO toCouponVO(UserCoupon userCoupon, Coupon template, LocalDateTime now) {
    int displayStatus = userCoupon.getStatus() == null ? UserCoupon.STATUS_UNUSED : userCoupon.getStatus();
    boolean notExpired = userCoupon.getExpiresAt() != null && userCoupon.getExpiresAt().isAfter(now);
    if (displayStatus == UserCoupon.STATUS_UNUSED && !notExpired) {
      // 数据库不回写过期状态，仅输出展示态 2
      displayStatus = UserCoupon.STATUS_EXPIRED;
    }
    return new CouponVO(
        userCoupon.getId(),
        template.getId(),
        template.getName(),
        template.getAmount(),
        template.getMinSpend(),
        displayStatus,
        userCoupon.getReceivedAt(),
        userCoupon.getExpiresAt(),
        displayStatus == UserCoupon.STATUS_UNUSED
    );
  }
}
