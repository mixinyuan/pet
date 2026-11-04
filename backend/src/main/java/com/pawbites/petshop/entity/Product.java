package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品实体
 * <p>对应数据表 product，存储宠物商品的基础与销售信息。</p>
 */
@Data
@TableName("product")
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

  /** 商品 SKU 编号（全局唯一） */
  private String sku;
  /** 商品名称 */
  private String name;
  /** 适用宠物类型（如猫、狗） */
  private String petType;
  /** 口味/风味 */
  private String flavor;
  /** 售价（单位：元） */
  private BigDecimal price;
  /** 评分（0.0-5.0，默认 5.0） */
  private BigDecimal rating;
  /** 库存数量（默认 0） */
  private Integer stock;
  /** 商品状态（默认 ACTIVE） */
  private String status;
  /** 商品图片 URL */
  private String imageUrl;
  /** 商品描述 */
  private String description;
}
