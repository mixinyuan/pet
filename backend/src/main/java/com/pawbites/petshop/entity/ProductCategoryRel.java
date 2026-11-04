package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品-分类关联实体
 * <p>对应数据表 product_category_rel，维护商品与分类的多对多关联关系。</p>
 */
@Data
@TableName("product_category_rel")
@EqualsAndHashCode(callSuper = true)
public class ProductCategoryRel extends BaseEntity {

  /** 商品 ID（外键关联 product.id） */
  private Long productId;
  /** 分类 ID（外键关联 category.id） */
  private Long categoryId;
}
