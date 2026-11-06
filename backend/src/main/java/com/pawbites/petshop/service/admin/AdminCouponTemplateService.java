package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.CouponTemplateSaveRequest;
import com.pawbites.petshop.entity.Coupon;
import com.pawbites.petshop.vo.admin.AdminCouponTemplateVO;
import java.util.List;

/**
 * 管理端优惠券模板服务
 * <p>提供后台券模板维护：全量列表（附已发放/已使用统计）、新增/编辑（名称全局唯一）、
 * 启用/停用切换与删除（已发放数 > 0 时拒绝删除，仅支持停用）。
 * 发放规则（注册/订阅自动发）不变，停用后对应模板不再被自动发放。</p>
 */
public interface AdminCouponTemplateService extends IService<Coupon> {

  /**
   * 查询全部券模板（后台视角，含停用模板）
   * <p>不分页（模板量小），按 id 倒序（最新模板在前）；每个模板附 issuedCount
   * （该模板 user_coupon 记录总数）与 usedCount（其中 status=1 已核销数）。</p>
   *
   * @return 券模板列表（含已发放/已使用统计）
   */
  List<AdminCouponTemplateVO> listTemplates();

  /**
   * 查询单个券模板（含统计，供新增/编辑回显）
   *
   * @param id 模板 id
   * @return 券模板视图（含已发放/已使用统计）
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）时抛出
   */
  AdminCouponTemplateVO getTemplate(Long id);

  /**
   * 新增券模板
   * <p>名称全局唯一，重复报 409；优惠类型固定 FIXED_AMOUNT；状态默认 ACTIVE。</p>
   *
   * @param request 模板表单
   * @return 新增后的模板视图（统计为 0）
   * @throws com.pawbites.petshop.exception.BusinessException 名称重复（409）时抛出
   */
  AdminCouponTemplateVO createTemplate(CouponTemplateSaveRequest request);

  /**
   * 编辑券模板
   * <p>仅更新名称/面额/门槛/有效天数/备注五列；注意 user_coupon 未快照金额/门槛，
   * 名称、金额、门槛在展示与核销时随模板实时读取，编辑会影响存量持券实例。</p>
   *
   * @param id      模板 id
   * @param request 模板表单（与新增同一份校验）
   * @return 编辑后的模板视图（含最新统计）
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）、名称重复（409）时抛出
   */
  AdminCouponTemplateVO updateTemplate(Long id, CouponTemplateSaveRequest request);

  /**
   * 切换模板状态
   * <p>INACTIVE（停用）后注册/订阅不再自动发放该模板，存量持券照常展示与核销；
   * ACTIVE（启用）恢复自动发放。</p>
   *
   * @param id     模板 id
   * @param status 目标状态（ACTIVE/INACTIVE）
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除券模板（物理删除）
   * <p>已发放数（user_coupon 引用数）大于 0 时拒绝删除（400，文案含已发放数，
   * 提示仅支持停用，防审计断链且避免持券记录外键悬空）。</p>
   *
   * @param id 模板 id
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）、已发放（400）时抛出
   */
  void deleteTemplate(Long id);
}
