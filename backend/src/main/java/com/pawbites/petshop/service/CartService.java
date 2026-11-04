package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Cart;

import com.pawbites.petshop.dto.cart.AddCartItemRequest;
import com.pawbites.petshop.dto.cart.UpdateCartItemRequest;
import com.pawbites.petshop.vo.cart.CartVO;

public interface CartService extends IService<Cart> {

  CartVO getCart(Long userId, String guestId);

  CartVO addItem(Long userId, String guestId, AddCartItemRequest request);

  CartVO updateItem(Long userId, String guestId, Long productId, UpdateCartItemRequest request);

  CartVO removeItem(Long userId, String guestId, Long productId);

  void clearCart(Long userId, String guestId);

  Cart resolveCart(Long userId, String guestId, boolean createIfMissing);
}
