package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品标签实体
 * <p>对应数据表 product_tag，存储商品标签字典（如"新品尝鲜"、"热销"）。</p>
 */
@Data
@TableName("product_tag")
@EqualsAndHashCode(callSuper = true)
public class ProductTag extends BaseEntity {

  /** 标签名称（全局唯一） */
  private String name;
  /** 标签状态（默认 ACTIVE） */
  private String status;
}
