package com.pawbites.petshop.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("product_tag_rel")
@EqualsAndHashCode(callSuper = true)
public class ProductTagRel extends BaseEntity {

  private Long productId;
  private Long tagId;
}
