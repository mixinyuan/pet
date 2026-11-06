package com.pawbites.petshop.vo.admin;

import java.time.LocalDateTime;

/**
 * 管理端优惠券模板视图
 * <p>后台券模板列表/新增回显/编辑回显共用；除模板自身字段外，
 * 附 issuedCount（已发放数）/usedCount（已使用数，均从 user_coupon 统计；
 * 使用率可由前端按 usedCount/issuedCount 计算）。</p>
 */
public record AdminCouponTemplateVO(
    /** 模板 ID */
    Long id,
    /** 券名称（自动发券按名称「新人专享券」定位模板） */
    String name,
    /** 优惠类型（FIXED_AMOUNT-固定满减，本期唯一取值） */
    String discountType,
    /** 抵扣面额（单位：元） */
    Integer amount,
    /** 使用门槛（单位：元；0 表示无门槛） */
    Integer minSpend,
    /** 有效天数（发放时按 发放时间 + valid_days 计算过期时间） */
    Integer validDays,
    /** 备注说明 */
    String remark,
    /** 状态（ACTIVE 启用，可被注册/订阅自动发放；INACTIVE 停用，不再自动发放） */
    String status,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 已发放数（user_coupon 中该模板的持券记录总数） */
    Long issuedCount,
    /** 已使用数（user_coupon 中该模板 status=1 已核销的记录数） */
    Long usedCount
) {
}
