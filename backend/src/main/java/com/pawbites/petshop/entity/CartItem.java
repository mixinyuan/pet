package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车条目实体
 * <p>对应数据表 cart_item，存储购物车中每条条目的明细；自 V9 起支持单品与套餐两类条目。</p>
 */
@Data
@TableName("cart_item")
@EqualsAndHashCode(callSuper = true)
public class CartItem extends BaseEntity {

  /** 条目类型常量：单品 */
  public static final String TYPE_PRODUCT = "PRODUCT";
  /** 条目类型常量：套餐 */
  public static final String TYPE_BUNDLE = "BUNDLE";

  /** 所属购物车 ID（外键关联 cart.id） */
  private Long cartId;
  /**
   * 关联 ID：PRODUCT 行为商品 id，BUNDLE 行为套餐 id
   * <p>两类 id 分属不同自增序列、可能撞号，解读时必须结合 {@link #itemType}。</p>
   */
  private Long productId;
  /** 条目类型：PRODUCT-单品 BUNDLE-套餐（新建条目时按类型显式赋值，缺省单品） */
  private String itemType = TYPE_PRODUCT;
  /** 加入购物车的数量（默认 1） */
  private Integer quantity;
  /** 是否勾选参与结算（1-勾选，0-未勾选，默认 1） */
  private Boolean checked;
}
