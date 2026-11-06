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

import com.pawbites.petshop.dto.admin.ProductSaveRequest;
import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.admin.AdminProductService;
import com.pawbites.petshop.vo.admin.AdminProductVO;

/**
 * 管理端商品控制器
 * <p>路由前缀 /api/admin/products，提供后台商品的分页查询（不过滤上架状态）、详情、新增、
 * 编辑（关联整体替换）、上下架切换与删除接口；由 SecurityConfig 统一约束 /api/admin/**
 * 仅 ADMIN 角色可访问；类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

  /** 管理端商品服务 */
  @Resource
  private AdminProductService adminProductService;

  /**
   * 分页查询商品（GET /api/admin/products）
   * <p>后台视角不过滤上架状态（下架商品同样可见），按 id 倒序；
   * 关键字匹配名称/SKU/口味，分类筛选经关联表解析。</p>
   *
   * @param page     页码，从 1 开始，默认 1
   * @param size     每页条数，默认 10，clamp 到 [1, 100]
   * @param keyword  名称/SKU/口味关键字模糊搜索，可选，最长 64 字符
   * @param category 分类 code，可选（all 或空表示不过滤）
   * @param status   上架状态筛选，可选（ACTIVE/INACTIVE，空表示全部）
   * @return 商品分页结果（records 含分类 code 列表与标签 id 列表）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  @GetMapping
  public ApiResponse<PageResult<AdminProductVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.success(adminProductService.pageProducts(page, size, keyword, category, status));
  }

  /**
   * 查询商品详情（GET /api/admin/products/{id}）
   * <p>与列表单条同构（含下架商品），供编辑弹窗回填。</p>
   *
   * @param id 商品 id
   * @return 商品详情（含分类 code 列表与标签 id 列表）
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminProductVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminProductService.getProduct(id));
  }

  /**
   * 新增商品（POST /api/admin/products）
   * <p>事务内写主表与分类/标签两张关联表；SKU 全局唯一，重复报「SKU 已存在：xxx」。</p>
   *
   * @param request 商品表单（字段校验见 {@link ProductSaveRequest}）
   * @return 新增后的商品详情
   * @throws com.pawbites.petshop.exception.BusinessException SKU 重复（409）、分类/标签无效（400）时抛出
   */
  @PostMapping
  public ApiResponse<AdminProductVO> create(@Valid @RequestBody ProductSaveRequest request) {
    return ApiResponse.success(adminProductService.createProduct(request));
  }

  /**
   * 编辑商品（PUT /api/admin/products/{id}）
   * <p>整体替换语义：主表字段全量更新，分类/标签关联先删后插（事务内）。</p>
   *
   * @param id      商品 id
   * @param request 商品表单（与新增同一份校验）
   * @return 编辑后的商品详情
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）、SKU 重复（409）、
   *                                                          分类/标签无效（400）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
    return ApiResponse.success(adminProductService.updateProduct(id, request));
  }

  /**
   * 切换上架状态（PUT /api/admin/products/{id}/status）
   * <p>只更新状态列，下架商品 C 端立即不可见。</p>
   *
   * @param id      商品 id
   * @param request 状态请求（status：ACTIVE 上架/INACTIVE 下架）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminProductService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除商品（DELETE /api/admin/products/{id}）
   * <p>主表逻辑删除；商品被未删除套餐引用时拒绝（报错文案含套餐名）。</p>
   *
   * @param id 商品 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）、被套餐引用（400）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminProductService.deleteProduct(id);
    return ApiResponse.success();
  }
}
