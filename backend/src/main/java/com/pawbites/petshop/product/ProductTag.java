package com.pawbites.petshop.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("product_tag")
@EqualsAndHashCode(callSuper = true)
public class ProductTag extends BaseEntity {

  private String name;
  private String status;
}
