package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @Resource
  private OrderService orderService;

  @PostMapping
  public ApiResponse<OrderVO> create(HttpServletRequest request) {
    return ApiResponse.success(orderService.createFromCart(SecurityUtils.currentUserId(), SecurityUtils.guestId(request)));
  }

  @GetMapping
  public ApiResponse<List<OrderVO>> list(HttpServletRequest request) {
    return ApiResponse.success(orderService.listOrders(SecurityUtils.currentUserId(), SecurityUtils.guestId(request)));
  }

  @GetMapping("/{id}")
  public ApiResponse<OrderVO> detail(@PathVariable Long id, HttpServletRequest request) {
    return ApiResponse.success(orderService.getOrder(SecurityUtils.currentUserId(), SecurityUtils.guestId(request), id));
  }

  @PutMapping("/{id}/cancel")
  public ApiResponse<OrderVO> cancel(@PathVariable Long id, HttpServletRequest request) {
    return ApiResponse.success(orderService.cancelOrder(SecurityUtils.currentUserId(), SecurityUtils.guestId(request), id));
  }
}
