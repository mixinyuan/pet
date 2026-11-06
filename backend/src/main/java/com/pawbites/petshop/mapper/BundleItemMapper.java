package com.pawbites.petshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import com.pawbites.petshop.entity.BundleItem;

/**
 * 套餐商品项表 Mapper
 * <p>继承 MyBatis-Plus BaseMapper，提供 bundle_item 表的基础 CRUD。</p>
 */
public interface BundleItemMapper extends BaseMapper<BundleItem> {

  /**
   * 物理删除指定套餐的全部明细行。
   * <p>明细表为纯关系表，无唯一键，逻辑删除虽不会引发唯一键冲突，但管理端每次保存套餐都会
   * 整体替换明细，若走逻辑删除会让 deleted=1 的死行随编辑次数无限堆积，故此处用原生 SQL
   * 绕过 @TableLogic 做物理删除（沿用 P1 商品关联表的处理方式，明细行无审计价值，物理删除安全）。</p>
   *
   * @param bundleId 套餐 ID
   * @return 删除的明细行数
   */
  @Delete("DELETE FROM bundle_item WHERE bundle_id = #{bundleId}")
  int deleteByBundleIdPhysically(@Param("bundleId") Long bundleId);
}
