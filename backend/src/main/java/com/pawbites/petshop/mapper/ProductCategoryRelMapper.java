package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import com.pawbites.petshop.entity.ProductCategoryRel;

/**
 * 商品-分类关联表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 product_category_rel 表的基础 CRUD。</p>
 */
public interface ProductCategoryRelMapper extends BaseMapper<ProductCategoryRel> {

  /**
   * 物理删除指定商品的全部分类关联行。
   * <p>关联表为纯关系表，无唯一键，逻辑删除虽不会引发唯一键冲突，但管理端每次保存商品都会
   * 整体替换关联，若走逻辑删除会让 deleted=1 的死行随编辑次数无限堆积，故此处用原生 SQL
   * 绕过 @TableLogic 做物理删除（关联行无审计价值，物理删除安全）。</p>
   *
   * @param productId 商品 ID
   * @return 删除的关联行数
   */
  @Delete("DELETE FROM product_category_rel WHERE product_id = #{productId}")
  int deleteByProductIdPhysically(@Param("productId") Long productId);
}
