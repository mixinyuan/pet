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

import com.pawbites.petshop.dto.admin.ReviewSaveRequest;
import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.admin.AdminReviewService;
import com.pawbites.petshop.vo.admin.AdminReviewVO;

/**
 * 管理端评价控制器
 * <p>路由前缀 /api/admin/reviews，提供后台评价的分页查询（不过滤展示状态）、
 * 单条查看、新增、编辑、展示/隐藏切换与删除接口；由 SecurityConfig 统一约束
 * /api/admin/** 仅 ADMIN 角色可访问；类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

  /** 管理端评价服务 */
  @Resource
  private AdminReviewService adminReviewService;

  /**
   * 分页查询评价（GET /api/admin/reviews）
   * <p>后台视角不过滤展示状态（隐藏评价同样可见），按排序号升序（同 C 端展示序）；
   * 关键字匹配评价内容或评价人姓名；records 含全部评价字段与关联商品名 productName
   * （未关联或商品已删除为 null）。</p>
   *
   * @param page      页码，从 1 开始，默认 1
   * @param size      每页条数，默认 10，clamp 到 [1, 100]
   * @param keyword   内容/评价人关键字模糊搜索，可选，最长 64 字符
   * @param status    展示状态筛选，可选（ACTIVE/INACTIVE，空表示全部）
   * @param productId 关联商品 id 筛选，可选
   * @return 评价分页结果（records 含关联商品名）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  @GetMapping
  public ApiResponse<PageResult<AdminReviewVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long productId
  ) {
    return ApiResponse.success(adminReviewService.pageReviews(page, size, keyword, status, productId));
  }

  /**
   * 查询单条评价（GET /api/admin/reviews/{id}）
   * <p>与列表单条同构（含隐藏评价），供编辑弹窗回填。</p>
   *
   * @param id 评价 id
   * @return 评价详情（含关联商品名）
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminReviewVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminReviewService.getReview(id));
  }

  /**
   * 新增评价（POST /api/admin/reviews）
   * <p>评价由后台统一维护（C 端无发布入口）；关联商品可空，非空时须存在且未删除。</p>
   *
   * @param request 评价表单（字段校验见 {@link ReviewSaveRequest}）
   * @return 新增后的评价详情
   * @throws com.pawbites.petshop.exception.BusinessException 关联商品不存在（400）时抛出
   */
  @PostMapping
  public ApiResponse<AdminReviewVO> create(@Valid @RequestBody ReviewSaveRequest request) {
    return ApiResponse.success(adminReviewService.createReview(request));
  }

  /**
   * 编辑评价（PUT /api/admin/reviews/{id}）
   * <p>整体替换语义：表单覆盖的可编辑列全量更新（含关联商品与随动同步的冗余商品名）。</p>
   *
   * @param id      评价 id
   * @param request 评价表单（与新增同一份校验）
   * @return 编辑后的评价详情
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）、
   *                                                          关联商品不存在（400）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<AdminReviewVO> update(@PathVariable Long id, @Valid @RequestBody ReviewSaveRequest request) {
    return ApiResponse.success(adminReviewService.updateReview(id, request));
  }

  /**
   * 切换展示状态（PUT /api/admin/reviews/{id}/status）
   * <p>INACTIVE（隐藏）后 C 端评价列表立即不展示；ACTIVE（展示）恢复。</p>
   *
   * @param id      评价 id
   * @param request 状态请求（status：ACTIVE 展示/INACTIVE 隐藏）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminReviewService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除评价（DELETE /api/admin/reviews/{id}）
   * <p>逻辑删除（deleted=1），不可在 C 端展示。</p>
   *
   * @param id 评价 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    adminReviewService.deleteReview(id);
    return ApiResponse.success();
  }
}
