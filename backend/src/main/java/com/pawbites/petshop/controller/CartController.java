package com.pawbites.petshop.controller;
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

import com.pawbites.petshop.dto.cart.AddCartItemRequest;
import com.pawbites.petshop.dto.cart.UpdateCartItemRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.CartService;
import com.pawbites.petshop.vo.cart.CartVO;

/**
 * 购物车控制器
 * <p>路由前缀 /api/cart，提供购物车的查询、加购、改数量、删除、清空接口。
 * 登录用户按 userId、游客按 X-Guest-Id 请求头定位购物车。</p>
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

  /** 购物车服务 */
  @Resource
  private CartService cartService;

  /**
   * 查询当前购物车（GET /api/cart）
   * <p>只读查询，不创建购物车；购物车不存在时返回空购物车（无条目、件数 0、总金额 0）。</p>
   *
   * @return 购物车详情（条目、件数、总金额）
   */
  @GetMapping
  public ApiResponse<CartVO> getCart() {
    return ApiResponse.success(cartService.getCart(SecurityUtils.currentUserId(), SecurityUtils.guestId()));
  }

  /**
   * 添加商品到购物车（POST /api/cart/items）
   *
   * @param requestBody 加购请求（productId 商品 id、quantity 加购数量，缺省为 1）
   * @return 更新后的购物车详情
   * @throws BusinessException 商品不存在或已下架（404）、商品数量必须大于 0、库存不足时抛出
   */
  @PostMapping("/items")
  public ApiResponse<CartVO> addItem(@Valid @RequestBody AddCartItemRequest requestBody) {
    return ApiResponse.success(cartService.addItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(), requestBody));
  }

  /**
   * 修改购物车中指定商品的购买数量（PUT /api/cart/items/{productId}）
   *
   * @param productId 商品 id
   * @param requestBody 修改请求（quantity 新数量，必须大于等于 1）
   * @return 更新后的购物车详情
   * @throws BusinessException 购物车中没有该商品或商品库存不足时抛出
   */
  @PutMapping("/items/{productId}")
  public ApiResponse<CartVO> updateItem(
      @PathVariable Long productId,
      @Valid @RequestBody UpdateCartItemRequest requestBody
  ) {
    return ApiResponse.success(cartService.updateItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(), productId, requestBody));
  }

  /**
   * 删除购物车中指定的商品条目（DELETE /api/cart/items/{productId}）
   *
   * @param productId 商品 id
   * @return 更新后的购物车详情
   */
  @DeleteMapping("/items/{productId}")
  public ApiResponse<CartVO> removeItem(@PathVariable Long productId) {
    return ApiResponse.success(cartService.removeItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(), productId));
  }

  /**
   * 清空当前购物车（DELETE /api/cart）
   *
   * @return 成功响应，无数据体
   */
  @DeleteMapping
  public ApiResponse<Void> clear() {
    cartService.clearCart(SecurityUtils.currentUserId(), SecurityUtils.guestId());
    return ApiResponse.success();
  }
}
