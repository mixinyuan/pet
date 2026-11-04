package com.pawbites.petshop.service.impl;
import com.pawbites.petshop.service.CartService;
import com.pawbites.petshop.service.OrderService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Cart;
import com.pawbites.petshop.entity.CartItem;
import com.pawbites.petshop.entity.OrderEntity;
import com.pawbites.petshop.entity.OrderItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CartItemMapper;
import com.pawbites.petshop.mapper.OrderItemMapper;
import com.pawbites.petshop.mapper.OrderMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.vo.order.OrderItemVO;
import com.pawbites.petshop.vo.order.OrderVO;

@Service
public class OrderServiceImpl implements OrderService {

  private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final CartItemMapper cartItemMapper;
  private final ProductMapper productMapper;
  private final CartService cartService;

  public OrderServiceImpl(
      OrderMapper orderMapper,
      OrderItemMapper orderItemMapper,
      CartItemMapper cartItemMapper,
      ProductMapper productMapper,
      CartService cartService
  ) {
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.cartItemMapper = cartItemMapper;
    this.productMapper = productMapper;
    this.cartService = cartService;
  }

  @Transactional
  @Override
  public OrderVO createFromCart(Long userId, String guestId) {
    Cart cart = cartService.resolveCart(userId, guestId, false);
    if (cart == null) {
      throw new BusinessException("购物车为空");
    }

    List<CartItem> cartItems = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .eq(CartItem::getChecked, true));
    if (cartItems.isEmpty()) {
      throw new BusinessException("购物车为空");
    }

    Map<Long, Product> productMap = productMapper.selectBatchIds(
            cartItems.stream().map(CartItem::getProductId).distinct().toList()
        )
        .stream()
        .collect(Collectors.toMap(Product::getId, product -> product));

    BigDecimal totalAmount = BigDecimal.ZERO;
    for (CartItem cartItem : cartItems) {
      Product product = requireOrderableProduct(productMap.get(cartItem.getProductId()));
      if (productMapper.deductStock(product.getId(), cartItem.getQuantity()) == 0) {
        throw new BusinessException(product.getName() + "库存不足");
      }
      totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
    }

    OrderEntity order = new OrderEntity();
    order.setOrderNo(generateOrderNo());
    order.setUserId(userId);
    order.setGuestId(userId == null ? guestId : null);
    order.setStatus(StatusConstants.CREATED);
    order.setTotalAmount(totalAmount);
    orderMapper.insert(order);

    for (CartItem cartItem : cartItems) {
      Product product = productMap.get(cartItem.getProductId());
      OrderItem orderItem = new OrderItem();
      orderItem.setOrderId(order.getId());
      orderItem.setProductId(product.getId());
      orderItem.setProductName(product.getName());
      orderItem.setProductImage(product.getImageUrl());
      orderItem.setPrice(product.getPrice());
      orderItem.setQuantity(cartItem.getQuantity());
      orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
      orderItemMapper.insert(orderItem);
    }

    cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .eq(CartItem::getChecked, true));
    return toOrderVO(order);
  }

  @Override
  public List<OrderVO> listOrders(Long userId, String guestId) {
    requireOwner(userId, guestId);
    List<OrderEntity> orders = orderMapper.selectList(userId != null
        ? Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, userId).orderByDesc(OrderEntity::getId)
        : Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getGuestId, guestId).orderByDesc(OrderEntity::getId));
    return orders.stream().map(this::toOrderVO).toList();
  }

  @Override
  public OrderVO getOrder(Long userId, String guestId, Long orderId) {
    requireOwner(userId, guestId);
    OrderEntity order = orderMapper.selectById(orderId);
    if (order == null || !isOwner(order, userId, guestId)) {
      throw new BusinessException(404, "订单不存在");
    }
    return toOrderVO(order);
  }

  @Transactional
  @Override
  public OrderVO cancelOrder(Long userId, String guestId, Long orderId) {
    requireOwner(userId, guestId);
    OrderEntity order = orderMapper.selectById(orderId);
    if (order == null || !isOwner(order, userId, guestId)) {
      throw new BusinessException(404, "订单不存在");
    }
    if (!StatusConstants.CREATED.equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可取消");
    }

    List<OrderItem> items = listOrderItems(order.getId());
    for (OrderItem item : items) {
      productMapper.restoreStock(item.getProductId(), item.getQuantity());
    }
    order.setStatus(StatusConstants.CANCELLED);
    orderMapper.updateById(order);
    return toOrderVO(order);
  }

  private OrderVO toOrderVO(OrderEntity order) {
    List<OrderItemVO> items = listOrderItems(order.getId()).stream()
        .map(item -> new OrderItemVO(
            item.getProductId(),
            item.getProductName(),
            item.getProductImage(),
            item.getPrice(),
            item.getQuantity(),
            item.getSubtotal()
        ))
        .toList();
    return new OrderVO(
        order.getId(),
        order.getOrderNo(),
        order.getTotalAmount(),
        order.getStatus(),
        order.getCreatedAt(),
        items
    );
  }

  private List<OrderItem> listOrderItems(Long orderId) {
    return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
        .eq(OrderItem::getOrderId, orderId)
        .orderByAsc(OrderItem::getId));
  }

  private Product requireOrderableProduct(Product product) {
    if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
      throw new BusinessException("部分商品已下架");
    }
    return product;
  }

  private void requireOwner(Long userId, String guestId) {
    if (userId == null && !StringUtils.hasText(guestId)) {
      throw new BusinessException(401, "请先登录或提供游客标识");
    }
  }

  private boolean isOwner(OrderEntity order, Long userId, String guestId) {
    if (userId != null) {
      return userId.equals(order.getUserId());
    }
    return StringUtils.hasText(guestId) && guestId.equals(order.getGuestId());
  }

  private String generateOrderNo() {
    return "PB" + LocalDateTime.now().format(ORDER_NO_TIME) + ThreadLocalRandom.current().nextInt(100000, 1000000);
  }
}
