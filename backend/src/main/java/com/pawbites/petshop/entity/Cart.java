package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("cart")
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {

  private Long userId;
  private String guestId;
}
