package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套餐商品项实体
 * <p>对应数据表 bundle_item，存储套餐内包含的商品及其数量。</p>
 */
@Data
@TableName("bundle_item")
@EqualsAndHashCode(callSuper = true)
public class BundleItem extends BaseEntity {

  /** 所属套餐 ID（外键关联 bundle.id） */
  private Long bundleId;
  /** 商品 ID（外键关联 product.id） */
  private Long productId;
  /** 该商品在套餐中的数量（默认 1） */
  private Integer quantity;
}
