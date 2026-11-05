package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户持券实体
 * <p>对应数据表 user_coupon，存储用户持有券实例的发放/核销/返还生命周期。
 * 不继承 {@link BaseEntity}：本表无 created_at/deleted 通用列，时间列为
 * received_at/expires_at/used_at（驼峰映射一致），状态流转均显式更新。</p>
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

  /** 持券状态：未使用 */
  public static final int STATUS_UNUSED = 0;
  /** 持券状态：已使用（核销） */
  public static final int STATUS_USED = 1;
  /** 持券状态：已过期（展示态，按 expires_at 懒判定输出，不回写数据库） */
  public static final int STATUS_EXPIRED = 2;

  /** 主键 ID（数据库自增；下单接口的 couponId 即此 id，而非模板 id） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 持有用户 id（游客不可持券、下单不可用券） */
  private Long userId;

  /** 券模板 id（名称/金额/门槛随模板读取） */
  private Long couponId;

  /** 状态（0-未使用 1-已使用；2-已过期为展示态，不落库） */
  private Integer status;

  /** 发放时间（received_at + 模板 valid_days = expires_at） */
  private LocalDateTime receivedAt;

  /** 过期时间（发放时按模板 valid_days 计算写入） */
  private LocalDateTime expiresAt;

  /** 核销时间（下单用券时写入；取消订单返还券时清空） */
  private LocalDateTime usedAt;

  /** 核销关联订单 id（下单用券时写入；取消订单返还时清空） */
  private Long orderId;
}
