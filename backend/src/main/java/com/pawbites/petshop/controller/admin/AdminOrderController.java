package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.admin.AdminOrderService;
import com.pawbites.petshop.vo.admin.AdminOrderDetailVO;
import com.pawbites.petshop.vo.admin.AdminOrderVO;

/**
 * 管理端订单控制器
 * <p>路由前缀 /api/admin/orders，提供后台订单的只读查阅（多条件分页列表、详情含商品快照
 * 与用券信息）与管理员兜底取消；由 SecurityConfig 统一约束 /api/admin/** 仅 ADMIN 角色
 * 可访问；类级 @Validated 开启查询参数校验（如订单号/关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

  /** 管理端订单服务 */
  @Resource
  private AdminOrderService adminOrderService;

  /**
   * 分页查询订单（GET /api/admin/orders）
   * <p>后台视角用户单与游客单均可见，按订单 id 倒序；支持订单号精确搜索、
   * 下单人模糊搜索（用户名/昵称，游客单按 guestId 前缀匹配）、状态筛选与
   * 下单日期闭区间范围（from/to 均含当天，格式 yyyy-MM-dd）。</p>
   *
   * @param page    页码，从 1 开始，默认 1
   * @param size    每页条数，默认 10，clamp 到 [1, 100]
   * @param orderNo 订单号精确查询，可选，最长 40 字符
   * @param keyword 下单人关键字模糊搜索，可选，最长 64 字符
   * @param status  订单状态筛选，可选（CREATED/CANCELLED，空表示全部）
   * @param from    下单日期起（含当天，yyyy-MM-dd），可选
   * @param to      下单日期止（含当天，yyyy-MM-dd），可选
   * @return 订单分页结果（records 含下单人展示名、商品件数、用券信息与取消审计列）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法、from 晚于 to、
   *                                                          日期格式不合法（400）时抛出
   */
  @GetMapping
  public ApiResponse<PageResult<AdminOrderVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size,
      @RequestParam(required = false)
      @Size(max = 40, message = "订单号最长 40 字符")
      String orderNo,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to
  ) {
    return ApiResponse.success(adminOrderService.pageOrders(page, size, orderNo, keyword, status, from, to));
  }

  /**
   * 查询订单详情（GET /api/admin/orders/{id}）
   * <p>在列表行字段之上补充商品快照列表（items）、商品合计（itemsSubtotal）与
   * 下单用户信息（user，游客单为 null）；取消信息（cancelTime/cancelBy）随行返回。</p>
   *
   * @param id 订单 id
   * @return 订单详情（含商品快照与用券信息）
   * @throws com.pawbites.petshop.exception.BusinessException 订单不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<AdminOrderDetailVO> detail(@PathVariable Long id) {
    return ApiResponse.success(adminOrderService.getOrderDetail(id));
  }

  /**
   * 管理员取消订单（POST /api/admin/orders/{id}/cancel）
   * <p>仅 CREATED 状态可取消（否则 400「订单状态不允许取消」），取消不可逆；
   * 复用 C 端同一套取消逻辑：原子回补商品库存 + 返还该订单核销的优惠券，
   * 并写入 cancel_time=当前时间、cancel_by=ADMIN。</p>
   *
   * @param id 订单 id
   * @return 取消后的订单详情（状态已置为 CANCELLED）
   * @throws com.pawbites.petshop.exception.BusinessException 订单不存在（404）、
   *                                                          订单状态不允许取消（400）时抛出
   */
  @PostMapping("/{id}/cancel")
  public ApiResponse<AdminOrderDetailVO> cancel(@PathVariable Long id) {
    return ApiResponse.success(adminOrderService.cancelOrder(id));
  }
}
