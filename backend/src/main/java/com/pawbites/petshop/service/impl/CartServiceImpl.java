package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.CartService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
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

/**
 * 购物车服务实现
 * <p>写操作（加购/改量/删除/清空）均在事务中执行；加购前校验商品有效性与库存
 * （含与已有条目合并后的总量校验），条目落库采用唯一索引 + 原子 upsert，消除“先查后写”竞态；
 * resolveCart 按 userId 优先、guestId 兜底定位购物车，按需创建，并发首建冲突时兜底重查；
 * 查询为只读操作，不创建购物车；视图装配时批量加载商品并过滤已失效商品条目。</p>
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

  /** 购物车条目 Mapper */
  @Resource
  private CartItemMapper cartItemMapper;
  /** 商品 Mapper（加购校验与视图装配） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public CartVO getCart(Long userId, String guestId) {
    // 查询无副作用：购物车不存在时不创建，直接返回空购物车视图
    Cart cart = resolveCart(userId, guestId, false);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO addItem(Long userId, String guestId, AddCartItemRequest request) {
    Product product = requireAvailableProduct(request.productId());
    Cart cart = resolveCart(userId, guestId, true);
    int quantity = request.normalizedQuantity();

    // 先查当前数量，校验合并后的总量不超库存，再交由 upsert 原子落库
    CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cart.getId())
        .eq(CartItem::getProductId, product.getId())
        .last("LIMIT 1"));
    ensureStock(product, quantity + (item == null ? 0 : item.getQuantity()));

    cartItemMapper.upsertItem(cart.getId(), product.getId(), quantity);
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

    Cart cart = selectCartByIdentity(userId, guestId);
    if (cart == null && createIfMissing) {
      cart = new Cart();
      cart.setUserId(userId);
      cart.setGuestId(userId == null ? guestId : null);
      try {
        baseMapper.insert(cart);
      } catch (DuplicateKeyException e) {
        // 并发首建双车时，唯一索引让后插入方失败；重查一次返回已存在的购物车
        cart = selectCartByIdentity(userId, guestId);
      }
    }
    return cart;
  }

  @Override
  public Cart lockCart(Long cartId) {
    if (cartId == null) {
      throw new BusinessException(404, "购物车不存在");
    }
    Cart cart = baseMapper.selectByIdForUpdate(cartId);
    if (cart == null) {
      throw new BusinessException(404, "购物车不存在");
    }
    return cart;
  }

  /** 按身份定位已存在的购物车：userId 优先，guestId 兜底 */
  private Cart selectCartByIdentity(Long userId, String guestId) {
    return baseMapper.selectOne(userId != null
        ? Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, userId).last("LIMIT 1")
        : Wrappers.<Cart>lambdaQuery().eq(Cart::getGuestId, guestId).last("LIMIT 1"));
  }

  /** 组装购物车视图：批量加载条目对应商品，汇总总件数与总金额 */
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

  /** 条目 + 商品 → 购物车条目视图（商品已失效时返回 null 以便过滤） */
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

  /** 校验商品存在且为有效状态，否则抛 404 */
  private Product requireAvailableProduct(Long productId) {
    Product product = productMapper.selectById(productId);
    if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
      throw new BusinessException(404, "商品不存在或已下架");
    }
    return product;
  }

  /** 校验数量为正且不超过商品当前库存 */
  private void ensureStock(Product product, Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new BusinessException("商品数量必须大于 0");
    }
    if (product.getStock() == null || product.getStock() < quantity) {
      throw new BusinessException("商品库存不足");
    }
  }
}
