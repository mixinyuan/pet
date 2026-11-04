package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.pawbites.petshop.entity.Product;

/**
 * 商品表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 product 表的基础 CRUD，
 * 并包含库存的原子扣减与恢复操作。</p>
 */
public interface ProductMapper extends BaseMapper<Product> {

  /**
   * 条件扣减库存（防超卖）
   * <p>单条 UPDATE 语句原子执行：仅当商品在售（status = 'ACTIVE'）、未被逻辑删除
   * 且当前库存充足（stock >= quantity）时，才将库存扣减指定数量并刷新 updated_at。</p>
   *
   * @param productId 商品 ID
   * @param quantity  扣减数量
   * @return 受影响行数：1 表示扣减成功；0 表示商品不存在、已下架、已删除或库存不足
   */
  @Update("""
      UPDATE product
      SET stock = stock - #{quantity}, updated_at = NOW()
      WHERE id = #{productId}
        AND stock >= #{quantity}
        AND status = 'ACTIVE'
        AND deleted = 0
      """)
  int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  /**
   * 恢复库存（取消订单时回补）
   * <p>单条 UPDATE 语句原子执行：为指定商品回加数量并刷新 updated_at。
   * 仅要求商品未被逻辑删除，不校验在售状态，用于订单取消后的库存回滚。</p>
   *
   * @param productId 商品 ID
   * @param quantity  恢复数量
   * @return 受影响行数：1 表示恢复成功；0 表示商品不存在或已被逻辑删除
   */
  @Update("""
      UPDATE product
      SET stock = stock + #{quantity}, updated_at = NOW()
      WHERE id = #{productId}
        AND deleted = 0
      """)
  int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
