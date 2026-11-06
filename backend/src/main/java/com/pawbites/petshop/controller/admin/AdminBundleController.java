package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.admin.BundleSaveRequest;
import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.admin.AdminBundleService;
import com.pawbites.petshop.vo.admin.AdminBundleVO;

/**
 * 管理端场景套餐控制器
 * <p>路由前缀 /api/admin/bundles，提供后台套餐的分页查询（不过滤上架状态）、详情（含明细子表
 * 与汇总字段）、新增、编辑（明细整体替换）、上下架切换与删除接口；由 SecurityConfig 统一约束
 * /api/admin/** 仅 ADMIN 角色可访问；类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/bundles")
public class AdminBundleController {

  /** 管理端套餐服务 */
  @Resource
  private AdminBundleService adminBundleService;

  /**
   * 分页查询套餐（GET /api/admin/bundles）
   * <p>后台视角不过滤上架状态（下架套餐同样可见），按 sort_order、id 升序；
   * 关键字匹配套餐名称；records 每条含明细子表与 itemCount（明细行数）/totalQuantity（总件数）汇总。</p>
   *
   * @param page    页码，从 1 开始，默认 1
   * @param size    每页条数，默认 10，clamp 到 [1, 100]
   * @param keyword 套餐名关键字模糊搜索，可选，最长 64 字符
   * @param status  上架状态筛选，可选（ACTIVE/INACTIVE，空表示全部）
   * @return 套餐分页结果（records 含明细与汇总字段）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  @GetMapping
  public ApiResponse<PageResult<AdminBundleVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword,
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.success(adminBundleService.pageBundles(page, size, keyword, status));
  }

  /**
   * 查询套餐详情（GET /api/admin/bundles/{id}）
   * <p>与列表单条同构（含下架套餐），供编辑弹窗回填明细子表。</p>
   *
   * @param id 套餐 id
   * @return 套餐详情（含明细子表与汇总字段）
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminBundleVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminBundleService.getBundle(id));
  }

  /**
   * 新增套餐（POST /api/admin/bundles）
   * <p>事务内写 bundle 主表与 bundle_item 明细；要求原价 ≥ 现价、明细 ≥ 1 行、
   * 同一商品不可重复、引用商品存在且未删除（允许下架商品入套餐）。</p>
   *
   * @param request 套餐表单（字段校验见 {@link BundleSaveRequest}）
   * @return 新增后的套餐详情
   * @throws com.pawbites.petshop.exception.BusinessException 价格规则不符/明细无效（400）时抛出
   */
  @PostMapping
  public ApiResponse<AdminBundleVO> create(@Valid @RequestBody BundleSaveRequest request) {
    return ApiResponse.success(adminBundleService.createBundle(request));
  }

  /**
   * 编辑套餐（PUT /api/admin/bundles/{id}）
   * <p>整体替换语义：主表字段全量更新，bundle_item 明细先物理删除旧行再重插（事务内）；
   * 套餐与订单无关联（C 端暂不可购套餐），编辑不影响历史数据。</p>
   *
   * @param id      套餐 id
   * @param request 套餐表单（与新增同一份校验）
   * @return 编辑后的套餐详情
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）、
   *                                                          价格规则不符/明细无效（400）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminBundleVO> update(@PathVariable Long id, @Valid @RequestBody BundleSaveRequest request) {
    return ApiResponse.success(adminBundleService.updateBundle(id, request));
  }

  /**
   * 切换上架状态（PUT /api/admin/bundles/{id}/status）
   * <p>只更新状态列，下架套餐 C 端立即不可见（C 端仅返回 ACTIVE 套餐）。</p>
   *
   * @param id      套餐 id
   * @param request 状态请求（status：ACTIVE 上架/INACTIVE 下架）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminBundleService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除套餐（DELETE /api/admin/bundles/{id}）
   * <p>bundle 主表逻辑删除即可；bundle_item 明细行保留不管，查询侧按 deleted=0 正确过滤。</p>
   *
   * @param id 套餐 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminBundleService.deleteBundle(id);
    return ApiResponse.success();
  }
}
