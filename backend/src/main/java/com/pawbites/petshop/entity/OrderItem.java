package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单商品项实体
 * <p>对应数据表 order_item，存储订单中每条商品的快照明细。</p>
 */
@Data
@TableName("order_item")
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

  /** 所属订单 ID（外键关联 orders.id） */
  private Long orderId;
  /** 商品 ID（外键关联 product.id） */
  private Long productId;
  /** 商品名称（下单时的名称快照） */
  private String productName;
  /** 商品图片 URL（下单时的图片快照） */
  private String productImage;
  /** 成交单价（下单时的价格快照，单位：元） */
  private BigDecimal price;
  /** 购买数量 */
  private Integer quantity;
  /** 小计金额（单价 × 数量，单位：元） */
  private BigDecimal subtotal;
}
