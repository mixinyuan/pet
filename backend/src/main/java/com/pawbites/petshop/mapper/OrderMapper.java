package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.pawbites.petshop.entity.OrderEntity;

/**
 * 订单表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 orders 表的基础 CRUD，
 * 并包含取消订单的条件抢占更新。</p>
 */
public interface OrderMapper extends BaseMapper<OrderEntity> {

  /**
   * 条件抢占取消订单（防并发竞态）
   * <p>单条 UPDATE 语句原子执行：仅当订单仍处于已创建（CREATED）状态且未被逻辑删除时，
   * 才将状态置为 CANCELLED 并刷新 updated_at；并发取消同一订单时只有一个请求能抢占成功，
   * 其余请求影响行数为 0。</p>
   *
   * @param orderId 订单 ID
   * @return 受影响行数：1 表示抢占取消成功；0 表示订单不存在、已删除或状态已不是 CREATED
   */
  @Update("""
      UPDATE orders
      SET status = 'CANCELLED', updated_at = NOW()
      WHERE id = #{orderId}
        AND status = 'CREATED'
        AND deleted = 0
      """)
  int cancelIfCreated(@Param("orderId") Long orderId);
}
