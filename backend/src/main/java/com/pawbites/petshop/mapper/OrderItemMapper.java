package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pawbites.petshop.entity.OrderItem;

/**
 * 订单商品项表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 order_item 表的基础 CRUD，
 * 并包含管理端看板热销榜使用的按商品聚合销量/销售额查询。</p>
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

  /**
   * 按商品聚合未取消订单条目的销量与销售额（管理端看板热销榜聚合）
   * <p>GROUP BY / SUM / ORDER BY / LIMIT 全部下推数据库执行，只返回聚合行：
   * JOIN orders 过滤 status = 'CREATED'（排除已取消订单的条目，已取消订单
   * 的库存与券均已回滚，不应计入销量）；仅统计单品条目（item_type = 'PRODUCT'），
   * 套餐行的 product_id 是套餐 id、与商品 id 可能撞号，混入会污染商品聚合。
   * 按销量降序、销售额降序（销量并列时销售额高者靠前）取前 N 行。
   * 聚合不 JOIN 商品表——商品已逻辑删除的条目仍保留计数（报表口径），
   * 商品信息由调用方按 id 批量补齐并对查不到的行跳过。</p>
   *
   * @param limit 返回的聚合行数上限（看板取 5，外层留有余量多取几行）
   * @return 商品销量聚合行列表（已按销量、销售额降序）
   */
  @Select("""
      SELECT oi.product_id          AS product_id,
             SUM(oi.quantity)       AS sold_quantity,
             SUM(oi.subtotal)       AS sales_amount
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
         AND o.status = 'CREATED'
         AND o.deleted = 0
      WHERE oi.deleted = 0
        AND oi.item_type = 'PRODUCT'
      GROUP BY oi.product_id
      ORDER BY sold_quantity DESC, sales_amount DESC
      LIMIT #{limit}
      """)
  List<ProductSalesStats> aggregateProductSales(@Param("limit") int limit);

  /** 商品销量聚合行（aggregateProductSales 的结果投影，非表实体） */
  @Data
  class ProductSalesStats {

    /** 商品 ID */
    private Long productId;
    /** 累计销量（未取消订单条目 quantity 之和，SUM 返回值经 JDBC 转为整数） */
    private Long soldQuantity;
    /** 累计销售额（未取消订单条目 subtotal 之和，单位：元） */
    private BigDecimal salesAmount;
  }
}
