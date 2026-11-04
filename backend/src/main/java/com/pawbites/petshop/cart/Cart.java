package com.pawbites.petshop.cart;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("cart")
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {

  private Long userId;
  private String guestId;
}
