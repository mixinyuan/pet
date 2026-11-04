package com.pawbites.petshop.bundle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
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
