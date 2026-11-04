package com.pawbites.petshop.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("product_category_rel")
@EqualsAndHashCode(callSuper = true)
public class ProductCategoryRel extends BaseEntity {

  private Long productId;
  private Long categoryId;
}
