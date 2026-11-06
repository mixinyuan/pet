package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Cart;

import com.pawbites.petshop.dto.cart.AddCartItemRequest;
import com.pawbites.petshop.dto.cart.UpdateCartItemRequest;
import com.pawbites.petshop.vo.cart.CartVO;

/**
 * 购物车服务
 * <p>管理登录用户与游客两类身份的购物车：定位/创建购物车、加购、改量、删除与清空。</p>
 */
public interface CartService extends IService<Cart> {

  /**
   * 查询购物车视图
   * <p>查询为只读操作、不创建购物车；购物车不存在时直接返回空购物车视图
   * （items 为空、count 为 0、totalAmount 为 0）。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @return 购物车视图（条目列表、总件数、总金额）
   */
  CartVO getCart(Long userId, String guestId);

  /**
   * 将单品或套餐加入购物车
   * <p>单品：商品不存在、已下架或库存不足时抛出业务异常；
   * 套餐：套餐不存在、已下架、组成商品下架或组成商品库存不足时抛出业务异常。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @param request  加购请求（productId 与 bundleId 恰好传其一，加数量）
   * @return 最新的购物车视图
   * @throws BusinessException 商品/套餐不存在或已下架（404）、数量非法或库存不足
   */
  CartVO addItem(Long userId, String guestId, AddCartItemRequest request);

  /**
   * 修改购物车中指定条目的数量
   * <p>购物车为空、条目不在购物车中、对应单品/套餐已下架或库存不足时抛出业务异常。</p>
   *
   * @param userId    登录用户 id（游客身份为 null）
   * @param guestId   游客标识（登录身份为 null）
   * @param productId 关联 id（单品为商品 id，套餐为套餐 id）
   * @param itemType  条目类型（PRODUCT-单品 BUNDLE-套餐）
   * @param request   改量请求（目标数量）
   * @return 最新的购物车视图
   * @throws BusinessException 购物车为空、无该条目、单品/套餐不存在或已下架、数量非法或库存不足
   */
  CartVO updateItem(Long userId, String guestId, Long productId, String itemType, UpdateCartItemRequest request);

  /**
   * 从购物车移除指定条目
   * <p>购物车或对应条目不存在时静默忽略。</p>
   *
   * @param userId    登录用户 id（游客身份为 null）
   * @param guestId   游客标识（登录身份为 null）
   * @param productId 关联 id（单品为商品 id，套餐为套餐 id）
   * @param itemType  条目类型（PRODUCT-单品 BUNDLE-套餐）
   * @return 最新的购物车视图
   */
  CartVO removeItem(Long userId, String guestId, Long productId, String itemType);

  /**
   * 清空购物车全部条目
   * <p>购物车不存在时静默忽略。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   */
  void clearCart(Long userId, String guestId);

  /**
   * 按身份定位购物车（供订单模块跨模块调用）
   * <p>优先按 userId 定位，其次按 guestId；两者均缺失时抛出业务异常。</p>
   *
   * @param userId          登录用户 id（游客身份为 null）
   * @param guestId         游客标识（登录身份为 null）
   * @param createIfMissing true 表示购物车不存在时自动创建
   * @return 购物车实体；createIfMissing 为 false 且购物车不存在时返回 null
   * @throws BusinessException 既无 userId 也无 guestId（401）
   */
  Cart resolveCart(Long userId, String guestId, boolean createIfMissing);

  /**
   * 对购物车行加排他锁（供订单模块跨模块调用）
   * <p>通过 SELECT ... FOR UPDATE 对 cart 行加排他锁，必须在其外层事务内调用，
   * 用于下单流程的串行化：锁持有期间其他事务无法同时锁定/修改同一购物车，防止并发下单超卖。</p>
   * <p>已知边界（设计权衡）：行锁仅串行化对同一购物车的结算；结算事务读取勾选条目之后至删除之前的
   * 窗口内，并发加购到同一条目的数量会随旧条目一并被清理（不计入订单金额），用户表现为该次加购"丢失"。</p>
   *
   * @param cartId 购物车 id
   * @return 加锁后的购物车实体
   * @throws BusinessException 购物车不存在（404）
   */
  Cart lockCart(Long cartId);
}
