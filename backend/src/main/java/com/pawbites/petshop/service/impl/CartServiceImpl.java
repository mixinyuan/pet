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
import com.pawbites.petshop.entity.Bundle;
import com.pawbites.petshop.entity.BundleItem;
import com.pawbites.petshop.entity.Cart;
import com.pawbites.petshop.entity.CartItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.BundleItemMapper;
import com.pawbites.petshop.mapper.BundleMapper;
import com.pawbites.petshop.mapper.CartItemMapper;
import com.pawbites.petshop.mapper.CartMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.vo.cart.CartItemVO;
import com.pawbites.petshop.vo.cart.CartVO;

/**
 * 购物车服务实现
 * <p>写操作（加购/改量/删除/清空）均在事务中执行；加购前校验单品/套餐有效性与库存
 * （单品按自身库存、套餐按组成商品库存逐项校验，含与已有条目合并后的总量校验），
 * 条目落库采用唯一索引 + 原子 upsert，消除“先查后写”竞态；
 * resolveCart 按 userId 优先、guestId 兜底定位购物车，按需创建，并发首建冲突时兜底重查；
 * 查询为只读操作，不创建购物车；视图装配时批量加载单品与套餐及其组成商品，
 * 过滤已失效（下架/删除）条目。</p>
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

  /** 购物车条目 Mapper */
  @Resource
  private CartItemMapper cartItemMapper;
  /** 商品 Mapper（加购校验与视图装配） */
  @Resource
  private ProductMapper productMapper;
  /** 套餐 Mapper（套餐条目校验与视图装配） */
  @Resource
  private BundleMapper bundleMapper;
  /** 套餐组成 Mapper（套餐条目的组成商品装载） */
  @Resource
  private BundleItemMapper bundleItemMapper;

  @Override
  public CartVO getCart(Long userId, String guestId) {
    // 查询无副作用：购物车不存在时不创建，直接返回空购物车视图
    Cart cart = resolveCart(userId, guestId, false);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO addItem(Long userId, String guestId, AddCartItemRequest request) {
    // 单品与套餐恰好传其一：都传或都不传直接拒绝，避免语义歧义
    if (request.bundleId() != null) {
      if (request.productId() != null) {
        throw new BusinessException("商品与套餐不能同时加购");
      }
      return addBundleItem(userId, guestId, request.bundleId(), request.normalizedQuantity());
    }
    if (request.productId() == null) {
      throw new BusinessException("缺少商品或套餐标识");
    }

    Product product = requireAvailableProduct(request.productId());
    Cart cart = resolveCart(userId, guestId, true);
    int quantity = request.normalizedQuantity();

    // 先查当前数量，校验合并后的总量不超库存，再交由 upsert 原子落库
    CartItem item = selectItem(cart.getId(), product.getId(), CartItem.TYPE_PRODUCT);
    ensureStock(product, quantity + (item == null ? 0 : item.getQuantity()));

    cartItemMapper.upsertItem(cart.getId(), product.getId(), CartItem.TYPE_PRODUCT, quantity);
    return buildCartVO(cart);
  }

  /** 套餐加购：校验套餐有效与各组成商品库存（合并已有数量），按套餐条目 upsert 落库 */
  private CartVO addBundleItem(Long userId, String guestId, Long bundleId, int quantity) {
    Bundle bundle = requireAvailableBundle(bundleId);
    List<BundleItem> components = bundleComponents(bundle.getId());
    Cart cart = resolveCart(userId, guestId, true);

    CartItem item = selectItem(cart.getId(), bundle.getId(), CartItem.TYPE_BUNDLE);
    ensureBundleStock(components, quantity + (item == null ? 0 : item.getQuantity()));

    cartItemMapper.upsertItem(cart.getId(), bundle.getId(), CartItem.TYPE_BUNDLE, quantity);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO updateItem(Long userId, String guestId, Long productId, String itemType, UpdateCartItemRequest request) {
    String type = normalizeItemType(itemType);
    Cart cart = resolveCart(userId, guestId, false);
    if (cart == null) {
      throw new BusinessException("购物车为空");
    }
    CartItem item = selectItem(cart.getId(), productId, type);
    if (item == null) {
      throw new BusinessException("购物车中没有该" + labelOfType(type));
    }

    if (CartItem.TYPE_BUNDLE.equals(type)) {
      // 套餐改量：按目标数量校验各组成商品库存
      requireAvailableBundle(productId);
      ensureBundleStock(bundleComponents(productId), request.quantity());
    } else {
      Product product = requireAvailableProduct(productId);
      ensureStock(product, request.quantity());
    }

    item.setQuantity(request.quantity());
    cartItemMapper.updateById(item);
    return buildCartVO(cart);
  }

  @Transactional
  @Override
  public CartVO removeItem(Long userId, String guestId, Long productId, String itemType) {
    String type = normalizeItemType(itemType);
    Cart cart = resolveCart(userId, guestId, false);
    if (cart != null) {
      cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
          .eq(CartItem::getCartId, cart.getId())
          .eq(CartItem::getProductId, productId)
          .eq(CartItem::getItemType, type));
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

  /** 按购物车 + 关联 id + 条目类型定位唯一条目 */
  private CartItem selectItem(Long cartId, Long refId, String itemType) {
    return cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
        .eq(CartItem::getCartId, cartId)
        .eq(CartItem::getProductId, refId)
        .eq(CartItem::getItemType, itemType)
        .last("LIMIT 1"));
  }

  /** 校验条目类型入参，非法值按单品处理（历史调用方不传 type 时保持兼容） */
  private String normalizeItemType(String itemType) {
    return CartItem.TYPE_BUNDLE.equals(itemType) ? CartItem.TYPE_BUNDLE : CartItem.TYPE_PRODUCT;
  }

  /** 条目类型的中文标签（报错文案用） */
  private String labelOfType(String itemType) {
    return CartItem.TYPE_BUNDLE.equals(itemType) ? "套餐" : "商品";
  }

  /**
   * 组装购物车视图：批量加载条目关联的单品与套餐（套餐另装载组成商品），
   * 汇总总件数与总金额；已失效条目（商品/套餐下架或删除、组成商品缺失）过滤不展示。
   */
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

    Map<Long, Product> productMap = loadProductMap(items, CartItem.TYPE_PRODUCT);

    // 套餐条目：批量装载套餐与组成商品（组成商品用于图片展示，套餐价即条目单价）
    List<CartItem> bundleItems = items.stream()
        .filter(item -> CartItem.TYPE_BUNDLE.equals(item.getItemType()))
        .toList();
    Map<Long, Bundle> bundleMap = bundleItems.isEmpty() ? Map.of()
        : bundleMapper.selectBatchIds(bundleItems.stream().map(CartItem::getProductId).distinct().toList())
            .stream().collect(Collectors.toMap(Bundle::getId, bundle -> bundle));
    Map<Long, List<BundleItem>> componentMap = loadComponentMap(
        bundleItems.stream().map(CartItem::getProductId).distinct().toList());
    Map<Long, Product> componentProductMap = productMapper.selectBatchIds(
            componentMap.values().stream().flatMap(List::stream)
                .map(BundleItem::getProductId).distinct().toList())
        .stream().collect(Collectors.toMap(Product::getId, product -> product));

    List<CartItemVO> itemVOs = items.stream()
        .map(item -> CartItem.TYPE_BUNDLE.equals(item.getItemType())
            ? toBundleItemVO(item, bundleMap.get(item.getProductId()), componentMap, componentProductMap)
            : toCartItemVO(item, productMap.get(item.getProductId())))
        .filter(item -> item != null)
        .toList();
    int count = itemVOs.stream().mapToInt(CartItemVO::quantity).sum();
    BigDecimal total = itemVOs.stream()
        .map(CartItemVO::subtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CartVO(itemVOs, count, total);
  }

  /** 批量装载指定类型条目关联的商品（逻辑删除自动过滤） */
  private Map<Long, Product> loadProductMap(List<CartItem> items, String itemType) {
    List<Long> productIds = items.stream()
        .filter(item -> itemType.equals(item.getItemType()))
        .map(CartItem::getProductId)
        .distinct()
        .toList();
    if (productIds.isEmpty()) {
      return Map.of();
    }
    return productMapper.selectBatchIds(productIds).stream()
        .collect(Collectors.toMap(Product::getId, product -> product));
  }

  /** 批量装载套餐 → 组成条目映射 */
  private Map<Long, List<BundleItem>> loadComponentMap(List<Long> bundleIds) {
    if (bundleIds.isEmpty()) {
      return Map.of();
    }
    return bundleItemMapper.selectList(Wrappers.<BundleItem>lambdaQuery()
            .in(BundleItem::getBundleId, bundleIds))
        .stream()
        .collect(Collectors.groupingBy(BundleItem::getBundleId));
  }

  /** 单品条目 + 商品 → 购物车条目视图（商品已失效时返回 null 以便过滤） */
  private CartItemVO toCartItemVO(CartItem item, Product product) {
    if (product == null) {
      return null;
    }
    BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemVO(
        CartItem.TYPE_PRODUCT,
        product.getId(),
        product.getName(),
        product.getImageUrl(),
        product.getPrice(),
        item.getQuantity(),
        subtotal,
        item.getChecked()
    );
  }

  /** 套餐条目 + 套餐 → 购物车条目视图（套餐已失效/删除时返回 null 以便过滤；图片取首个组成商品） */
  private CartItemVO toBundleItemVO(CartItem item, Bundle bundle,
      Map<Long, List<BundleItem>> componentMap, Map<Long, Product> componentProductMap) {
    if (bundle == null || !StatusConstants.ACTIVE.equals(bundle.getStatus())) {
      return null;
    }
    String image = componentMap.getOrDefault(bundle.getId(), List.of()).stream()
        .map(BundleItem::getProductId)
        .map(componentProductMap::get)
        .filter(product -> product != null)
        .findFirst()
        .map(Product::getImageUrl)
        .orElse(null);
    BigDecimal subtotal = bundle.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemVO(
        CartItem.TYPE_BUNDLE,
        bundle.getId(),
        bundle.getName(),
        image,
        bundle.getPrice(),
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

  /** 校验套餐存在且为上架状态，否则抛 404 */
  private Bundle requireAvailableBundle(Long bundleId) {
    Bundle bundle = bundleMapper.selectById(bundleId);
    if (bundle == null || !StatusConstants.ACTIVE.equals(bundle.getStatus())) {
      throw new BusinessException(404, "套餐不存在或已下架");
    }
    return bundle;
  }

  /** 加载套餐组成条目（按 id 升序，保证“首个组成商品”稳定） */
  private List<BundleItem> bundleComponents(Long bundleId) {
    return bundleItemMapper.selectList(Wrappers.<BundleItem>lambdaQuery()
        .eq(BundleItem::getBundleId, bundleId)
        .orderByAsc(BundleItem::getId));
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

  /**
   * 校验套餐各组成商品库存：每份套餐消耗 comp.quantity 件对应商品，
   * totalQuantity 份共需 totalQuantity × comp.quantity 件；任一不足即抛异常。
   */
  private void ensureBundleStock(List<BundleItem> components, int totalQuantity) {
    if (totalQuantity <= 0) {
      throw new BusinessException("套餐数量必须大于 0");
    }
    if (components.isEmpty()) {
      throw new BusinessException("套餐内没有商品，无法购买");
    }
    Map<Long, Product> productMap = productMapper.selectBatchIds(
            components.stream().map(BundleItem::getProductId).distinct().toList())
        .stream().collect(Collectors.toMap(Product::getId, product -> product));
    for (BundleItem component : components) {
      Product product = productMap.get(component.getProductId());
      if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
        throw new BusinessException("套餐内商品已下架：" + (product == null ? "#" + component.getProductId() : product.getName()));
      }
      int required = component.getQuantity() * totalQuantity;
      if (product.getStock() == null || product.getStock() < required) {
        throw new BusinessException("套餐内商品库存不足：" + product.getName());
      }
    }
  }
}
