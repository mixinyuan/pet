package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("cart_item")
@EqualsAndHashCode(callSuper = true)
public class CartItem extends BaseEntity {

  private Long cartId;
  private Long productId;
  private Integer quantity;
  private Boolean checked;
}
