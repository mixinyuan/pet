package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.admin.CategoryCreateRequest;
import com.pawbites.petshop.dto.admin.CategoryUpdateRequest;
import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.admin.AdminCategoryService;
import com.pawbites.petshop.vo.admin.AdminCategoryVO;

/**
 * 管理端分类控制器
 * <p>路由前缀 /api/admin/categories，提供后台商品分类的列表（含停用、带引用商品数）、新增、
 * 编辑（编码不可改）、启用/停用与删除接口；由 SecurityConfig 统一约束 /api/admin/**
 * 仅 ADMIN 角色可访问。C 端 GET /api/categories 只回 ACTIVE 分类的逻辑不受影响。</p>
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

  /** 管理端分类服务 */
  @Resource
  private AdminCategoryService adminCategoryService;

  /**
   * 查询全部分类（GET /api/admin/categories）
   * <p>含停用分类，按 sort_order、id 升序，每项带引用商品数。</p>
   *
   * @return 管理端分类视图列表
   */
  @GetMapping
  public ApiResponse<List<AdminCategoryVO>> list() {
    return ApiResponse.success(adminCategoryService.listCategories());
  }

  /**
   * 查询单个分类（GET /api/admin/categories/{id}）
   *
   * @param id 分类 id
   * @return 管理端分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminCategoryVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminCategoryService.getCategory(id));
  }

  /**
   * 新增分类（POST /api/admin/categories）
   * <p>编码必填（小写字母、唯一、不允许 all），状态默认 ACTIVE。</p>
   *
   * @param request 分类表单
   * @return 新增后的分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 编码为 all（400）、编码重复（409）时抛出
   */
  @PostMapping
  public ApiResponse<AdminCategoryVO> create(@Valid @RequestBody CategoryCreateRequest request) {
    return ApiResponse.success(adminCategoryService.createCategory(request));
  }

  /**
   * 编辑分类（PUT /api/admin/categories/{id}）
   * <p>仅可改 name/description/sortOrder/status；编码不允许修改；状态切换也可走 /status 接口。</p>
   *
   * @param id      分类 id
   * @param request 分类编辑表单（字段可选，null 表示不变）
   * @return 编辑后的分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）、编码被修改（400）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminCategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
    return ApiResponse.success(adminCategoryService.updateCategory(id, request));
  }

  /**
   * 切换分类状态（PUT /api/admin/categories/{id}/status）
   *
   * @param id      分类 id
   * @param request 状态请求（status：ACTIVE/INACTIVE）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminCategoryService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除分类（DELETE /api/admin/categories/{id}）
   * <p>系统内置 all 或被商品引用（文案含引用数）时拒绝，否则逻辑删除。</p>
   *
   * @param id 分类 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）、系统内置或被引用（400）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminCategoryService.deleteCategory(id);
    return ApiResponse.success();
  }
}
