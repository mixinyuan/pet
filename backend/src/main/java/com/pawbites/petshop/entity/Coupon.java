package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 优惠券模板实体
 * <p>对应数据表 coupon，存储券的定义（名称、类型、金额、门槛、有效天数）。
 * 不继承 {@link BaseEntity}：本表时间列命名为 create_time（驼峰映射 createTime），
 * 且无逻辑删除列；create_time 由数据库 DEFAULT CURRENT_TIMESTAMP 维护。</p>
 */
@Data
@TableName("coupon")
public class Coupon {

  /** 主键 ID（数据库自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 券名称（如：新人专享券；发放时按名称定位模板） */
  private String name;

  /** 优惠类型（FIXED_AMOUNT-固定满减，预留扩展） */
  private String discountType;

  /** 抵扣金额（单位：元，与 product.price 单位一致；INT 元，计算时换算 BigDecimal） */
  private Integer amount;

  /** 使用门槛（单位：元；订单商品总额满该值才可用，0-无门槛） */
  private Integer minSpend;

  /** 有效天数（发放时按 发放时间 + valid_days 计算过期时间） */
  private Integer validDays;

  /** 备注说明（发放场景与规则文案） */
  private String remark;

  /** 状态（ACTIVE-启用，可被注册/订阅自动发放；INACTIVE-停用，不再自动发放，存量持券不受影响；V8 新增列） */
  private String status;

  /** 创建时间（数据库自动填充） */
  private LocalDateTime createTime;
}
