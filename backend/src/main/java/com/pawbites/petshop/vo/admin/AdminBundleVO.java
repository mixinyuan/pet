package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端套餐视图
 * <p>后台套餐列表/详情共用；除主表字段外，携带明细子表（items）与两个汇总字段：
 * itemCount（明细行数，即「N 种」）、totalQuantity（总件数，即「共 M 件」），
 * 供列表列「3 种 · 共 5 件」直接展示；明细行已过滤商品被物理清理/逻辑删除的无效行。</p>
 */
public record AdminBundleVO(
    /** 套餐 ID */
    Long id,
    /** 套餐名称 */
    String name,
    /** 套餐描述 */
    String description,
    /** 套餐现价（单位：元） */
    BigDecimal price,
    /** 原价（划线展示用） */
    BigDecimal originalPrice,
    /** 折扣标签文案（如「省 18%」，可为空） */
    String discountLabel,
    /** 排序序号（数值越小越靠前） */
    Integer sortOrder,
    /** 上架状态（ACTIVE 上架/INACTIVE 下架，后台不过滤下架套餐） */
    String status,
    /** 明细子表（商品摘要 + 数量，按录入顺序） */
    List<AdminBundleItemVO> items,
    /** 汇总：明细行数（商品种数） */
    int itemCount,
    /** 汇总：总件数（各明细数量之和） */
    int totalQuantity
) {
}
