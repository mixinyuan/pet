package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车商品项实体
 * <p>对应数据表 cart_item，存储购物车中每条商品的明细。</p>
 */
@Data
@TableName("cart_item")
@EqualsAndHashCode(callSuper = true)
public class CartItem extends BaseEntity {

  /** 所属购物车 ID（外键关联 cart.id） */
  private Long cartId;
  /** 商品 ID（外键关联 product.id） */
  private Long productId;
  /** 加入购物车的数量（默认 1） */
  private Integer quantity;
  /** 是否勾选参与结算（1-勾选，0-未勾选，默认 1） */
  private Boolean checked;
}
