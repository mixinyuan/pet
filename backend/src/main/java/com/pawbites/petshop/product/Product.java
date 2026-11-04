package com.pawbites.petshop.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("product")
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

  private String sku;
  private String name;
  private String petType;
  private String flavor;
  private BigDecimal price;
  private BigDecimal rating;
  private Integer stock;
  private String status;
  private String imageUrl;
  private String description;
}
