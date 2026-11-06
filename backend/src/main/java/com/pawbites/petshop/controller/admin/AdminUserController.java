package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.admin.StatusUpdateRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.admin.AdminUserService;
import com.pawbites.petshop.vo.admin.AdminUserDetailVO;
import com.pawbites.petshop.vo.admin.AdminUserVO;

/**
 * 管理端用户控制器
 * <p>路由前缀 /api/admin/users，提供后台用户的只读查阅（关键字/状态分页列表、
 * 详情含订单/收藏/持券/订阅聚合）与账号禁用/启用接口；由 SecurityConfig 统一约束
 * /api/admin/** 仅 ADMIN 角色可访问；类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

  /** 管理端用户服务 */
  @Resource
  private AdminUserService adminUserService;

  /**
   * 分页查询用户（GET /api/admin/users）
   * <p>按用户 id 倒序；关键字匹配用户名/昵称/邮箱（三字段模糊），
   * 状态筛选 ACTIVE/INACTIVE。records 附带汇总：orderCount/totalSpend 仅统计
   * 有效订单（CREATED），couponTotal/couponAvailable 为持券数/可用券数。</p>
   *
   * @param page    页码，从 1 开始，默认 1
   * @param size    每页条数，默认 10，clamp 到 [1, 100]
   * @param keyword 用户名/昵称/邮箱关键字模糊搜索，可选，最长 64 字符
   * @param status  账号状态筛选，可选（ACTIVE/INACTIVE，空表示全部）
   * @return 用户分页结果（records 含订单/消费/持券汇总字段）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  @GetMapping
  public ApiResponse<PageResult<AdminUserVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword,
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.success(adminUserService.pageUsers(page, size, keyword, status));
  }

  /**
   * 查询用户详情（GET /api/admin/users/{id}）
   * <p>聚合视图：基本资料（含角色）+ 全部订单 + 收藏（总数与最近 5 个商品）
   * + 持券列表 + 邮箱订阅状态。</p>
   *
   * @param id 用户 id
   * @return 用户详情（含订单/收藏/持券/订阅聚合）
   * @throws com.pawbites.petshop.exception.BusinessException 用户不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminUserDetailVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminUserService.getUserDetail(id));
  }

  /**
   * 切换账号状态（PUT /api/admin/users/{id}/status）
   * <p>body {status: ACTIVE|INACTIVE}。禁用不允许作用于 role=ADMIN 的账号
   * （400「管理员账号不允许禁用」，防止自锁）；启用无限制。禁用不清数据、不删订单，
   * 该用户已签发的 token 会被 JwtAuthenticationFilter 的 status 回查即时拒绝。</p>
   *
   * @param id      用户 id
   * @param request 状态请求（status：ACTIVE 启用 / INACTIVE 禁用）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 用户不存在（404）、
   *                                                          管理员账号不允许禁用（400）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    adminUserService.updateStatus(id, request.status());
    return ApiResponse.success();
  }
}
