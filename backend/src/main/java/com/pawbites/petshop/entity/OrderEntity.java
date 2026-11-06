package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单实体
 * <p>对应数据表 orders，存储用户或游客提交的订单主记录。</p>
 */
@Data
@TableName("orders")
@EqualsAndHashCode(callSuper = true)
public class OrderEntity extends BaseEntity {

  /** 订单号（全局唯一） */
  private String orderNo;
  /** 下单用户 ID（外键关联 users.id，游客下单为空） */
  private Long userId;
  /** 游客标识（游客下单时的身份凭据，登录用户下单为空） */
  private String guestId;
  /** 订单总金额（单位：元） */
  private BigDecimal totalAmount;
  /** 订单状态（默认 CREATED） */
  private String status;
  /** 订单取消时间（取消订单时写入；数据库列 cancel_time，驼峰自动映射） */
  private LocalDateTime cancelTime;
  /** 取消方（USER-用户取消 ADMIN-管理员取消，取消订单时写入） */
  private String cancelBy;
}
