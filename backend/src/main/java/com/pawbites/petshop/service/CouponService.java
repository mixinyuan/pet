package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

import com.pawbites.petshop.vo.coupon.CouponVO;

/**
 * 优惠券服务
 * <p>管理用户持券的完整生命周期：发放（注册/订阅触发）、查询我的券列表、
 * 下单前的用券校验与抵扣金额计算、下单成功后的核销、取消订单时的返还。
 * 仅支持固定满减券（满 min_spend 减 amount）；游客不可持券、不可用券。</p>
 */
public interface CouponService extends IService<UserCoupon> {

  /**
   * 发放一张新人专享券
   * <p>按名称定位「新人专享券」模板，生成 status=0 的持券记录，
   * 过期时间 = 当前时间 + 模板 valid_days；模板缺失（种子数据未执行）时记录告警并跳过，
   * 不阻断注册/订阅主流程。</p>
   *
   * @param userId 接收券的用户 id
   */
  void issueNewUserCoupon(Long userId);

  /**
   * 查询当前用户的持券列表
   * <p>按持券 id 倒序（最新领取在前）；status 为懒判定展示态
   * （0-未使用 1-已使用 2-已过期，过期由 expires_at 判定不回写），
   * available = 未使用且未过期（门槛是否满足由前端结合购物车金额判断，下单以后端校验为准）。</p>
   *
   * @param userId 登录用户 id
   * @return 持券列表（名称、金额、门槛、状态、发放/过期时间、可用标记）
   * @throws BusinessException 未登录（401）时抛出
   */
  List<CouponVO> listCoupons(Long userId);

  /**
   * 下单用券校验并计算抵扣金额
   * <p>校验链：券存在 → 归属当前用户 → 未使用 → 未过期 → 订单商品总额 ≥ 模板 min_spend，
   * 任一不满足抛业务异常（订单整体回滚）；通过后返回抵扣金额
   * （模板 amount，若超过商品总额则收敛为商品总额，保证扣减后不为负）。
   * 须在下单事务内调用。</p>
   *
   * @param userId       登录用户 id（游客携带 couponId 会被上游直接拒绝）
   * @param userCouponId 持券实例 id（user_coupon.id）
   * @param goodsTotal   订单商品总额（用券门槛按此判断、抵扣以此封顶）
   * @return 抵扣金额（单位：元，不超过商品总额）
   * @throws BusinessException 券不存在/不归属（404）、已使用、已过期、未满门槛时抛出
   */
  BigDecimal validateForOrder(Long userId, Long userCouponId, BigDecimal goodsTotal);

  /**
   * 核销持券（下单成功路径中调用）
   * <p>原子条件更新（status 0 → 1 并写入 used_at/order_id）；影响行数为 0 说明券已被
   * 并发请求抢先核销，抛业务异常令当前下单事务整体回滚。须在下单事务内调用。</p>
   *
   * @param userCouponId 持券实例 id
   * @param orderId      关联订单 id
   * @throws BusinessException 券已被使用或不存在时抛出
   */
  void markUsed(Long userCouponId, Long orderId);

  /**
   * 按订单返还持券（取消订单路径中调用）
   * <p>将该订单核销的券改回未使用并清空 used_at/order_id；简单起见即使此刻已超过
   * expires_at 也按未使用返还，过期与否交由列表/下次用券校验按 expires_at 懒判定。
   * 订单未用券时影响行数为 0，静默跳过。须在取消订单事务内调用。</p>
   *
   * @param orderId 订单 id
   */
  void returnByOrder(Long orderId);
}
