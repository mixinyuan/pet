package com.pawbites.petshop.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

  @Update("""
      UPDATE product
      SET stock = stock - #{quantity}, updated_at = NOW()
      WHERE id = #{productId}
        AND stock >= #{quantity}
        AND status = 'ACTIVE'
        AND deleted = 0
      """)
  int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  @Update("""
      UPDATE product
      SET stock = stock + #{quantity}, updated_at = NOW()
      WHERE id = #{productId}
        AND deleted = 0
      """)
  int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
