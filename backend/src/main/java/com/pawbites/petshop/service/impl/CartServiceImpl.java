package com.pawbites.petshop.service.impl;
import com.pawbites.petshop.service.CartService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.cart.AddCartItemRequest;
import com.pawbites.petshop.dto.cart.UpdateCartItemRequest;
import com.pawbites.petshop.entity.Cart;
import com.pawbites.petshop.entity.CartItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CartItemMapper;
import com.pawbites.petshop.mapper.CartMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.vo.cart.CartItemVO;
import com.pawbites.petshop.vo.cart.CartVO;

@Service
public class CartServiceImpl implements CartService {

  private final CartMapper cartMapper;
  private final CartItemMapper cartItemMapper;
  private final ProductMapper productMapper;

  public CartServiceImpl(CartMapper cartMapper, CartItemMapper cartItemMapper, ProductMapper productMapper) {
    this.cartMapper = cartMapper;
    this.cartItemMapper = cartItemMapper;
    this.productMapper = productMapper;
  }

  @Override
  public CartVO getCart(Long userId, String guestId) {
    Cart cart = resolveCart(userId, guestId, true);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO addItem(Long userId, String guestId, AddCartItemRequest request) {
    Product product = requireAvailableProduct(request.productId());
    Cart cart = resolveCart(userId, guestId, true);
    int quantity = request.normalizedQuantity();

    CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .eq(CartItem::getProductId, product.getId())
        .last("LIMIT 1"));
    int nextQuantity = quantity + (item == null ? 0 : item.getQuantity());
    ensureStock(product, nextQuantity);

    if (item == null) {
      item = new CartItem();
      item.setCartId(cart.getId());
      item.setProductId(product.getId());
      item.setQuantity(quantity);
      item.setChecked(true);
      cartItemMapper.insert(item);
    } else {
      item.setQuantity(nextQuantity);
      cartItemMapper.updateById(item);
    }
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO updateItem(Long userId, String guestId, Long productId, UpdateCartItemRequest request) {
    Product product = requireAvailableProduct(productId);
    ensureStock(product, request.quantity());

    Cart cart = resolveCart(userId, guestId, false);
    if (cart == null) {
      throw new BusinessException("购物车为空");
    }
    CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .eq(CartItem::getProductId, productId)
        .last("LIMIT 1"));
    if (item == null) {
      throw new BusinessException("购物车中没有该商品");
    }

    item.setQuantity(request.quantity());
    cartItemMapper.updateById(item);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO removeItem(Long userId, String guestId, Long productId) {
    Cart cart = resolveCart(userId, guestId, false);
    if (cart != null) {
      cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
          .eq(CartItem::getCartId, cart.getId())
          .eq(CartItem::getProductId, productId));
    }
    return getCart(userId, guestId);
  }

  @Transactional
  @Override
  public void clearCart(Long userId, String guestId) {
    Cart cart = resolveCart(userId, guestId, false);
    if (cart != null) {
      cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getCartId, cart.getId()));
    }
  }

  @Override
  public Cart resolveCart(Long userId, String guestId, boolean createIfMissing) {
    if (userId == null && !StringUtils.hasText(guestId)) {
      throw new BusinessException(401, "请先登录或提供游客标识");
    }

    Cart cart = cartMapper.selectOne(userId != null
        ? Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, userId).last("LIMIT 1")
        : Wrappers.<Cart>lambdaQuery().eq(Cart::getGuestId, guestId).last("LIMIT 1"));
    if (cart == null && createIfMissing) {
      cart = new Cart();
      cart.setUserId(userId);
      cart.setGuestId(userId == null ? guestId : null);
      cartMapper.insert(cart);
    }
    return cart;
  }

  private CartVO buildCartVO(Cart cart) {
    if (cart == null) {
      return new CartVO(List.of(), 0, BigDecimal.ZERO);
    }

    List<CartItem> items = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .orderByDesc(CartItem::getId));
    if (items.isEmpty()) {
      return new CartVO(List.of(), 0, BigDecimal.ZERO);
    }

    Map<Long, Product> productMap = productMapper.selectBatchIds(
            items.stream().map(CartItem::getProductId).distinct().toList()
        )
        .stream()
        .collect(Collectors.toMap(Product::getId, product -> product));

    List<CartItemVO> itemVOs = items.stream()
        .map(item -> toCartItemVO(item, productMap.get(item.getProductId())))
        .filter(item -> item != null)
        .toList();
    int count = itemVOs.stream().mapToInt(CartItemVO::quantity).sum();
    BigDecimal total = itemVOs.stream()
        .map(CartItemVO::subtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CartVO(itemVOs, count, total);
  }

  private CartItemVO toCartItemVO(CartItem item, Product product) {
    if (product == null) {
      return null;
    }
    BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemVO(
        product.getId(),
        product.getName(),
        product.getImageUrl(),
        product.getPrice(),
        item.getQuantity(),
        subtotal,
        item.getChecked()
    );
  }

  private Product requireAvailableProduct(Long productId) {
    Product product = productMapper.selectById(productId);
    if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
      throw new BusinessException(404, "商品不存在或已下架");
    }
    return product;
  }

  private void ensureStock(Product product, Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new BusinessException("商品数量必须大于 0");
    }
    if (product.getStock() == null || product.getStock() < quantity) {
      throw new BusinessException("商品库存不足");
    }
  }
}
