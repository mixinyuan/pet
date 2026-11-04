package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

/**
 * 订单服务实现
 * <p>createFromCart 在单个事务中完成：校验勾选项与商品可售状态 → deductStock 原子扣减库存防超卖
 * （条件更新影响行数为 0 即抛异常回滚）→ 保存订单与条目（条目保存商品名称/图片/价格快照，不受后续改价影响）→
 * 清空已勾选的购物车条目；cancelOrder 校验 CREATED 状态后按条目 restoreStock 恢复库存；
 * 列表与详情按 userId/guestId 归属过滤。</p>
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

  /** 订单号中的时间戳格式（yyyyMMddHHmmss） */
  private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  /** 订单条目 Mapper */
  @Resource
  private OrderItemMapper orderItemMapper;
  /** 购物车条目 Mapper（读取勾选项、下单后清理） */
  @Resource
  private CartItemMapper cartItemMapper;
  /** 商品 Mapper（扣减/恢复库存） */
  @Resource
  private ProductMapper productMapper;
  /** 购物车服务（跨模块定位购物车） */
  @Resource
  private CartService cartService;

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
    baseMapper.insert(order);

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
    List<OrderEntity> orders = baseMapper.selectList(userId != null
        ? Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, userId).orderByDesc(OrderEntity::getId)
        : Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getGuestId, guestId).orderByDesc(OrderEntity::getId));
    return orders.stream().map(this::toOrderVO).toList();
  }

  @Override
  public OrderVO getOrder(Long userId, String guestId, Long orderId) {
    requireOwner(userId, guestId);
    OrderEntity order = baseMapper.selectById(orderId);
    if (order == null || !isOwner(order, userId, guestId)) {
      throw new BusinessException(404, "订单不存在");
    }
    return toOrderVO(order);
  }

  @Transactional
  @Override
  public OrderVO cancelOrder(Long userId, String guestId, Long orderId) {
    requireOwner(userId, guestId);
    OrderEntity order = baseMapper.selectById(orderId);
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
    baseMapper.updateById(order);
    return toOrderVO(order);
  }

  /** 组装订单视图：装载订单条目明细 */
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

  /** 查询订单全部条目（按 id 升序） */
  private List<OrderItem> listOrderItems(Long orderId) {
    return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
        .eq(OrderItem::getOrderId, orderId)
        .orderByAsc(OrderItem::getId));
  }

  /** 校验商品存在且为有效状态，否则视为已下架 */
  private Product requireOrderableProduct(Product product) {
    if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
      throw new BusinessException("部分商品已下架");
    }
    return product;
  }

  /** 校验身份参数：userId 与 guestId 至少存在其一 */
  private void requireOwner(Long userId, String guestId) {
    if (userId == null && !StringUtils.hasText(guestId)) {
      throw new BusinessException(401, "请先登录或提供游客标识");
    }
  }

  /** 判断订单是否归属当前身份（userId 优先，其次比对 guestId） */
  private boolean isOwner(OrderEntity order, Long userId, String guestId) {
    if (userId != null) {
      return userId.equals(order.getUserId());
    }
    return StringUtils.hasText(guestId) && guestId.equals(order.getGuestId());
  }

  /** 生成订单号：前缀 PB + 时间戳 + 6 位随机数 */
  private String generateOrderNo() {
    return "PB" + LocalDateTime.now().format(ORDER_NO_TIME) + ThreadLocalRandom.current().nextInt(100000, 1000000);
  }
}
