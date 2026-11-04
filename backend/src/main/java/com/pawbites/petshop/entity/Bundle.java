package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("bundle")
@EqualsAndHashCode(callSuper = true)
public class Bundle extends BaseEntity {

  private String name;
  private String description;
  private BigDecimal price;
  private BigDecimal originalPrice;
  private String discountLabel;
  private String status;
  private Integer sortOrder;
}
