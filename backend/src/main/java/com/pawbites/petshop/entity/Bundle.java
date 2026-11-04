package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品套餐实体
 * <p>对应数据表 bundle，存储商品组合套餐的促销信息。</p>
 */
@Data
@TableName("bundle")
@EqualsAndHashCode(callSuper = true)
public class Bundle extends BaseEntity {

  /** 套餐名称 */
  private String name;
  /** 套餐描述 */
  private String description;
  /** 套餐售价（单位：元） */
  private BigDecimal price;
  /** 原价（用于展示划线价、凸显优惠力度） */
  private BigDecimal originalPrice;
  /** 折扣标签文案（如"立省 30 元"） */
  private String discountLabel;
  /** 套餐状态（默认 ACTIVE） */
  private String status;
  /** 排序序号（数值越小越靠前，默认 0） */
  private Integer sortOrder;
}
