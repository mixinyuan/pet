package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.pawbites.petshop.dto.admin.CouponTemplateSaveRequest;
import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.admin.AdminCouponTemplateService;
import com.pawbites.petshop.vo.admin.AdminCouponTemplateVO;

/**
 * 管理端优惠券模板控制器
 * <p>路由前缀 /api/admin/coupon-templates，提供后台券模板的全量列表（附已发放/已使用统计）、
 * 新增、编辑、启用/停用切换与删除接口；由 SecurityConfig 统一约束 /api/admin/**
 * 仅 ADMIN 角色可访问；发放规则（注册/订阅自动发）不变，停用后不再自动发放。</p>
 */
@RestController
@RequestMapping("/api/admin/coupon-templates")
public class AdminCouponTemplateController {

  /** 管理端优惠券模板服务 */
  @Resource
  private AdminCouponTemplateService adminCouponTemplateService;

  /**
   * 查询全部券模板（GET /api/admin/coupon-templates）
   * <p>不分页（模板量小），按 id 倒序，含停用模板；每个模板附 issuedCount（已发放数）
   * 与 usedCount（已使用数，从 user_coupon 统计），使用率可由前端按两者相除计算。</p>
   *
   * @return 券模板列表（含已发放/已使用统计）
   */
  @GetMapping
  public ApiResponse<List<AdminCouponTemplateVO>> list() {
    return ApiResponse.success(adminCouponTemplateService.listTemplates());
  }

  /**
   * 查询单个券模板（GET /api/admin/coupon-templates/{id}）
   * <p>与列表单条同构（含停用模板与统计），供编辑弹窗回填。</p>
   *
   * @param id 模板 id
   * @return 模板详情（含已发放/已使用统计）
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminCouponTemplateVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminCouponTemplateService.getTemplate(id));
  }

  /**
   * 新增券模板（POST /api/admin/coupon-templates）
   * <p>名称全局唯一，重复报「券名称已存在：xxx」（409）；优惠类型固定 FIXED_AMOUNT，
   * 状态默认 ACTIVE（可被注册/订阅自动发放）。</p>
   *
   * @param request 模板表单（字段校验见 {@link CouponTemplateSaveRequest}）
   * @return 新增后的模板详情（统计为 0）
   * @throws com.pawbites.petshop.exception.BusinessException 名称重复（409）时抛出
   */
  @PostMapping
  public ApiResponse<AdminCouponTemplateVO> create(@Valid @RequestBody CouponTemplateSaveRequest request) {
    return ApiResponse.success(adminCouponTemplateService.createTemplate(request));
  }

  /**
   * 编辑券模板（PUT /api/admin/coupon-templates/{id}）
   * <p>仅更新名称/面额/门槛/有效天数/备注。注意：user_coupon 未快照金额/门槛/名称，
   * 持券展示与核销随模板实时读取，编辑会影响存量持券实例（前端编辑弹窗需提示）。</p>
   *
   * @param id      模板 id
   * @param request 模板表单（与新增同一份校验）
   * @return 编辑后的模板详情（含最新统计）
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）、名称重复（409）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminCouponTemplateVO> update(@PathVariable Long id,
      @Valid @RequestBody CouponTemplateSaveRequest request) {
    return ApiResponse.success(adminCouponTemplateService.updateTemplate(id, request));
  }

  /**
   * 切换模板状态（PUT /api/admin/coupon-templates/{id}/status）
   * <p>INACTIVE（停用）后注册/订阅不再自动发放该模板，存量持券照常展示与核销。</p>
   *
   * @param id      模板 id
   * @param request 状态请求（status：ACTIVE 启用/INACTIVE 停用）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminCouponTemplateService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除券模板（DELETE /api/admin/coupon-templates/{id}）
   * <p>物理删除；已发放数 &gt; 0 时拒绝（400，文案含已发放数，提示仅支持停用，
   * 防审计断链并避免持券记录外键悬空）。</p>
   *
   * @param id 模板 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 模板不存在（404）、已发放（400）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminCouponTemplateService.deleteTemplate(id);
    return ApiResponse.success();
  }
}
