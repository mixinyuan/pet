package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单条目实体
 * <p>对应数据表 order_item，存储订单中每条条目的快照明细；自 V9 起支持单品与套餐两类条目
 * （套餐行的 productName/price 为套餐名与套餐价快照）。</p>
 */
@Data
@TableName("order_item")
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

  /** 条目类型常量：单品 */
  public static final String TYPE_PRODUCT = "PRODUCT";
  /** 条目类型常量：套餐 */
  public static final String TYPE_BUNDLE = "BUNDLE";

  /** 所属订单 ID（外键关联 orders.id） */
  private Long orderId;
  /**
   * 关联 ID：PRODUCT 行为商品 id，BUNDLE 行为套餐 id
   * <p>两类 id 分属不同自增序列、可能撞号，解读时必须结合 {@link #itemType}。</p>
   */
  private Long productId;
  /** 条目类型：PRODUCT-单品 BUNDLE-套餐（新建条目时按类型显式赋值，缺省单品） */
  private String itemType = TYPE_PRODUCT;
  /** 名称快照（单品为商品名、套餐为套餐名，下单时冻结） */
  private String productName;
  /** 图片 URL 快照（套餐行取首个组成商品图片） */
  private String productImage;
  /** 成交单价（下单时的价格快照，套餐行即套餐价，单位：元） */
  private BigDecimal price;
  /** 购买数量 */
  private Integer quantity;
  /** 小计金额（单价 × 数量，单位：元） */
  private BigDecimal subtotal;
}
