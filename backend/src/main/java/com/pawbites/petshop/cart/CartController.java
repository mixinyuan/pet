package com.pawbites.petshop.cart;

import com.pawbites.petshop.common.result.ApiResponse;
import com.pawbites.petshop.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public ApiResponse<CartVO> getCart(HttpServletRequest request) {
    return ApiResponse.success(cartService.getCart(SecurityUtils.currentUserId(), SecurityUtils.guestId(request)));
  }

  @PostMapping("/items")
  public ApiResponse<CartVO> addItem(@Valid @RequestBody AddCartItemRequest requestBody, HttpServletRequest request) {
    return ApiResponse.success(cartService.addItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(request), requestBody));
  }

  @PutMapping("/items/{productId}")
  public ApiResponse<CartVO> updateItem(
      @PathVariable Long productId,
      @Valid @RequestBody UpdateCartItemRequest requestBody,
      HttpServletRequest request
  ) {
    return ApiResponse.success(cartService.updateItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(request), productId, requestBody));
  }

  @DeleteMapping("/items/{productId}")
  public ApiResponse<CartVO> removeItem(@PathVariable Long productId, HttpServletRequest request) {
    return ApiResponse.success(cartService.removeItem(SecurityUtils.currentUserId(), SecurityUtils.guestId(request), productId));
  }

  @DeleteMapping
  public ApiResponse<Void> clear(HttpServletRequest request) {
    cartService.clearCart(SecurityUtils.currentUserId(), SecurityUtils.guestId(request));
    return ApiResponse.success();
  }
}
