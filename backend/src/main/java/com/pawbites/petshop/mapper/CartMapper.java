package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pawbites.petshop.entity.Cart;

/**
 * 购物车表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 cart 表的基础 CRUD，
 * 并包含下单流程使用的行级排他锁查询。</p>
 */
public interface CartMapper extends BaseMapper<Cart> {

  /**
   * 按主键查询购物车并对该行加排他锁（SELECT ... FOR UPDATE）
   * <p>必须在事务内调用，否则锁立即释放；用于下单流程对购物车串行化，
   * 防止并发下单重复读取/扣减同一购物车。查询条件包含 deleted = 0，
   * 购物车不存在或已被逻辑删除时返回 {@code null}。</p>
   *
   * @param cartId 购物车 id
   * @return 加锁的购物车实体；未查到时返回 {@code null}
   */
  @Select("""
      SELECT id, user_id, guest_id, created_at, updated_at, deleted
      FROM cart
      WHERE id = #{cartId} AND deleted = 0
      FOR UPDATE
      """)
  Cart selectByIdForUpdate(@Param("cartId") Long cartId);
}
