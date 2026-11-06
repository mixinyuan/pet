package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pawbites.petshop.entity.OrderEntity;

/**
 * 订单表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 orders 表的基础 CRUD，
 * 并包含取消订单的条件抢占更新与看板用的实付总额聚合查询。</p>
 */
public interface OrderMapper extends BaseMapper<OrderEntity> {

  /**
   * 条件抢占取消订单（防并发竞态，C 端用户取消与管理端取消共用）
   * <p>单条 UPDATE 语句原子执行：仅当订单仍处于已创建（CREATED）状态且未被逻辑删除时，
   * 才将状态置为 CANCELLED 并写入取消审计列（cancel_time=当前时间、cancel_by=取消方）；
   * 并发取消同一订单时只有一个请求能抢占成功，其余请求影响行数为 0。</p>
   *
   * @param orderId  订单 ID
   * @param cancelBy 取消方（USER-用户取消 / ADMIN-管理员取消）
   * @return 受影响行数：1 表示抢占取消成功；0 表示订单不存在、已删除或状态已不是 CREATED
   */
  @Update("""
      UPDATE orders
      SET status = 'CANCELLED', cancel_time = NOW(), cancel_by = #{cancelBy}, updated_at = NOW()
      WHERE id = #{orderId}
        AND status = 'CREATED'
        AND deleted = 0
      """)
  int cancelIfCreated(@Param("orderId") Long orderId, @Param("cancelBy") String cancelBy);

  /**
   * 按创建时间闭区间汇总指定状态订单的实付总额（管理端看板聚合）
   * <p>SUM 下推数据库执行，只返回标量结果不拉回订单行；条件包含 deleted = 0、
   * status = 'CREATED'（看板口径：已取消订单不计入销售额）。区间内无订单时
   * IFNULL 兜底返回 0，不会返回 null。</p>
   *
   * @param from 区间起（含，通常为某日 00:00:00）
   * @param to   区间止（含，通常为某日 23:59:59）
   * @return 区间内 CREATED 订单 total_amount 之和（单位：元；无订单时为 0）
   */
  @Select("""
      SELECT IFNULL(SUM(total_amount), 0)
      FROM orders
      WHERE status = 'CREATED'
        AND deleted = 0
        AND created_at >= #{from}
        AND created_at <= #{to}
      """)
  BigDecimal sumCreatedAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
