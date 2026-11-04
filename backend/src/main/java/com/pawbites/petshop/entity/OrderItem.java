package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("order_item")
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

  private Long orderId;
  private Long productId;
  private String productName;
  private String productImage;
  private BigDecimal price;
  private Integer quantity;
  private BigDecimal subtotal;
}
