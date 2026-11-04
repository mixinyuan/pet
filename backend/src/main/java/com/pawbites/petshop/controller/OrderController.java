package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.OrderService;
import com.pawbites.petshop.vo.order.OrderVO;

/**
 * 订单控制器
 * <p>路由前缀 /api/orders，提供从购物车结算下单、订单列表、订单详情、取消订单接口。
 * 登录用户按 userId、游客按 X-Guest-Id 请求头定位订单。</p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  /** 订单服务 */
  @Resource
  private OrderService orderService;

  /**
   * 从当前购物车结算并创建订单（POST /api/orders）
   * <p>仅结算购物车中勾选（checked）的条目，下单成功后删除这些条目并扣减商品库存。</p>
   *
   * @return 新建的订单详情（订单号、总金额、状态、创建时间、订单条目）
   * @throws BusinessException 购物车为空、部分商品已下架、商品库存不足时抛出
   */
  @PostMapping
  public ApiResponse<OrderVO> create() {
    return ApiResponse.success(orderService.createFromCart(SecurityUtils.currentUserId(), SecurityUtils.guestId()));
  }

  /**
   * 查询当前用户的订单列表（GET /api/orders）
   *
   * @return 订单列表，按订单 id 倒序（最新在前）
   * @throws BusinessException 未登录且未提供游客标识（401）时抛出
   */
  @GetMapping
  public ApiResponse<List<OrderVO>> list() {
    return ApiResponse.success(orderService.listOrders(SecurityUtils.currentUserId(), SecurityUtils.guestId()));
  }

  /**
   * 查询指定订单详情（GET /api/orders/{id}）
   *
   * @param id 订单 id
   * @return 订单详情（订单号、总金额、状态、创建时间、订单条目）
   * @throws BusinessException 未登录且未提供游客标识（401）、订单不存在或不属于当前用户（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<OrderVO> detail(@PathVariable Long id) {
    return ApiResponse.success(orderService.getOrder(SecurityUtils.currentUserId(), SecurityUtils.guestId(), id));
  }

  /**
   * 取消指定订单（PUT /api/orders/{id}/cancel）
   * <p>仅已创建（CREATED）状态的订单可取消，取消后回补商品库存并将状态置为 CANCELLED。</p>
   *
   * @param id 订单 id
   * @return 取消后的订单详情
   * @throws BusinessException 订单不存在或不属于当前用户（404）、当前订单状态不可取消时抛出
   */
  @PutMapping("/{id}/cancel")
  public ApiResponse<OrderVO> cancel(@PathVariable Long id) {
    return ApiResponse.success(orderService.cancelOrder(SecurityUtils.currentUserId(), SecurityUtils.guestId(), id));
  }
}
