package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端商品视图
 * <p>后台商品列表/详情共用；与 C 端 ProductVO 的差异：带 status（后台需查看下架商品）、
 * categories 为分类 code 列表（后台表单以 code 回填多选框）、tags 为标签 id 列表（后台以 id 维护关联）。</p>
 */
public record AdminProductVO(
    /** 商品 ID */
    Long id,
    /** 商品 SKU 编码（全局唯一） */
    String sku,
    /** 商品名称 */
    String name,
    /** 适用宠物类型（猫咪适用/狗狗适用/猫狗通用/小型犬适用） */
    String petType,
    /** 口味/风味 */
    String flavor,
    /** 售价（单位：元） */
    BigDecimal price,
    /** 评分（0.0-5.0，人工维护） */
    BigDecimal rating,
    /** 库存数量 */
    Integer stock,
    /** 上架状态（ACTIVE 上架/INACTIVE 下架，后台不过滤下架商品） */
    String status,
    /** 商品主图 URL */
    String imageUrl,
    /** 商品描述 */
    String description,
    /** 所属分类 code 列表（不含已删除分类） */
    List<String> categories,
    /** 关联标签 id 列表（不含已删除标签） */
    List<Long> tags
) {
}
