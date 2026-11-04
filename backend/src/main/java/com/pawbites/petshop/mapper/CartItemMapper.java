package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.pawbites.petshop.entity.CartItem;

/**
 * 购物车商品项表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 cart_item 表的基础 CRUD，
 * 并包含加购流程使用的原子合并插入。</p>
 */
public interface CartItemMapper extends BaseMapper<CartItem> {

  /**
   * 原子合并插入购物车条目（INSERT ... ON DUPLICATE KEY UPDATE）
   * <p>依赖 uk_cart_item (cart_id, product_id) 唯一索引，单条 SQL 原子完成“无则插入、有则累加数量”，
   * 取代“先查再插入/更新”的两步写法，消除并发加购竞态（重复行/覆盖丢量）。</p>
   * <p>冲突分支同时回置 checked = 1、deleted = 0：逻辑删除的条目仍占用唯一键，
   * 若不回置，被删除（removed/clear 置 deleted=1）后重新加购的商品会合并进不可见行。</p>
   * <p>数量语义：命中活跃行（deleted=0）累加数量；复活逻辑删除行（deleted=1）时以本次数量替换旧值——
   * 死行旧数量对用户不可见，累加会出现“删除后再加数量变多”的错觉，替换才等价于重新加购。
   * 注意 MySQL 的 ON DUPLICATE KEY UPDATE 赋值从左到右求值（后者可见前者新值），
   * 因此 IF 判断必须置于 deleted = 0 回置之前，才能读到行的原始删除状态。</p>
   *
   * @param cartId    购物车 id
   * @param productId 商品 id
   * @param quantity  本次加购数量（正整数）
   * @return 受影响行数：1 表示新插入；2 表示命中唯一键执行了更新
   */
  @Insert("""
      INSERT INTO cart_item (cart_id, product_id, quantity, checked, created_at, updated_at, deleted)
      VALUES (#{cartId}, #{productId}, #{quantity}, 1, NOW(), NOW(), 0)
      ON DUPLICATE KEY UPDATE
        quantity = IF(deleted = 1, #{quantity}, quantity + #{quantity}),
        checked = 1,
        deleted = 0,
        updated_at = NOW()
      """)
  int upsertItem(@Param("cartId") Long cartId, @Param("productId") Long productId, @Param("quantity") int quantity);
}
