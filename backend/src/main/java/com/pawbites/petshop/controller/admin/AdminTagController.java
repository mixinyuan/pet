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

import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.dto.admin.TagCreateRequest;
import com.pawbites.petshop.dto.admin.TagUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.admin.AdminTagService;
import com.pawbites.petshop.vo.admin.AdminTagVO;

/**
 * 管理端商品标签控制器
 * <p>路由前缀 /api/admin/tags，提供后台标签的列表（含停用、带引用商品数）、新增、改名、
 * 启用/停用与删除接口；由 SecurityConfig 统一约束 /api/admin/** 仅 ADMIN 角色可访问。
 * C 端商品 VO 的标签展示逻辑不受影响。</p>
 */
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

  /** 管理端标签服务 */
  @Resource
  private AdminTagService adminTagService;

  /**
   * 查询全部标签（GET /api/admin/tags）
   * <p>含停用标签，按 id 升序，每项带引用商品数。</p>
   *
   * @return 管理端标签视图列表
   */
  @GetMapping
  public ApiResponse<List<AdminTagVO>> list() {
    return ApiResponse.success(adminTagService.listTags());
  }

  /**
   * 新增标签（POST /api/admin/tags）
   * <p>名称全局唯一，新建默认启用（ACTIVE）。</p>
   *
   * @param request 标签表单（name 必填）
   * @return 新增后的标签视图
   * @throws com.pawbites.petshop.exception.BusinessException 名称重复（409）时抛出
   */
  @PostMapping
  public ApiResponse<AdminTagVO> create(@Valid @RequestBody TagCreateRequest request) {
    return ApiResponse.success(adminTagService.createTag(request));
  }

  /**
   * 标签改名（PUT /api/admin/tags/{id}）
   * <p>仅改名称（需保持全局唯一）；状态切换走 /status 接口。</p>
   *
   * @param id      标签 id
   * @param request 标签编辑表单（name 必填）
   * @return 编辑后的标签视图
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）、名称重复（409）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminTagVO> update(@PathVariable Long id, @Valid @RequestBody TagUpdateRequest request) {
    return ApiResponse.success(adminTagService.updateTag(id, request));
  }

  /**
   * 切换标签状态（PUT /api/admin/tags/{id}/status）
   * <p>取值与 product_tag.status 对齐（ACTIVE 启用/INACTIVE 停用）。</p>
   *
   * @param id      标签 id
   * @param request 状态请求（status：ACTIVE/INACTIVE）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminTagService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除标签（DELETE /api/admin/tags/{id}）
   * <p>被商品引用（文案含引用数）时拒绝，否则逻辑删除。</p>
   *
   * @param id 标签 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）、被商品引用（400）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminTagService.deleteTag(id);
    return ApiResponse.success();
  }
}
