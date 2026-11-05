package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.pawbites.petshop.entity.UserCoupon;

/**
 * 用户持券表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 user_coupon 表的基础 CRUD，
 * 并包含下单核销与取消返还两个原子条件更新操作。</p>
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

  /**
   * 核销持券（下单用券时调用）
   * <p>单条 UPDATE 语句原子执行：仅当券仍为未使用（status = 0）时，才置为已使用并写入
   * 核销时间与关联订单 id；用于与并发用券请求竞争，失败方影响行数为 0。</p>
   *
   * @param userCouponId 持券实例 id（user_coupon.id）
   * @param orderId      核销关联的订单 id
   * @return 受影响行数：1 表示核销成功；0 表示券不存在或已被使用（并发抢占失败）
   */
  @Update("""
      UPDATE user_coupon
      SET status = 1, used_at = NOW(), order_id = #{orderId}
      WHERE id = #{userCouponId}
        AND status = 0
      """)
  int markUsed(@Param("userCouponId") Long userCouponId, @Param("orderId") Long orderId);

  /**
   * 按订单返还持券（取消订单时调用）
   * <p>单条 UPDATE 语句原子执行：将该订单核销的券（status = 1 且 order_id 匹配）改回未使用，
   * 并清空 used_at/order_id。简单起见即使此时已超过 expires_at 也按未使用返还，
   * 是否过期由列表/下单校验按 expires_at 懒判定，不在此处展开。</p>
   *
   * @param orderId 订单 id
   * @return 受影响行数：1 表示返还成功；0 表示该订单未核销过券（无需返还）
   */
  @Update("""
      UPDATE user_coupon
      SET status = 0, used_at = NULL, order_id = NULL
      WHERE order_id = #{orderId}
        AND status = 1
      """)
  int returnByOrder(@Param("orderId") Long orderId);
}
