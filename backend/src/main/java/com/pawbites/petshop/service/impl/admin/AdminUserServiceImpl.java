package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminUserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Coupon;
import com.pawbites.petshop.entity.OrderEntity;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.Subscription;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.entity.UserCoupon;
import com.pawbites.petshop.entity.UserFavorite;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CouponMapper;
import com.pawbites.petshop.mapper.OrderMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.SubscriptionMapper;
import com.pawbites.petshop.mapper.UserCouponMapper;
import com.pawbites.petshop.mapper.UserFavoriteMapper;
import com.pawbites.petshop.mapper.UserMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminUserCouponVO;
import com.pawbites.petshop.vo.admin.AdminUserDetailVO;
import com.pawbites.petshop.vo.admin.AdminUserFavoriteProductVO;
import com.pawbites.petshop.vo.admin.AdminUserFavoriteVO;
import com.pawbites.petshop.vo.admin.AdminUserOrderVO;
import com.pawbites.petshop.vo.admin.AdminUserSubscriptionVO;
import com.pawbites.petshop.vo.admin.AdminUserVO;

/**
 * 管理端用户服务实现
 * <p>pageUsers 沿用管理端商品/套餐同款查询套路（页码/条数 clamp、状态取值校验、
 * like 通配符经 escapeLike 转义），关键字以「用户名 or 昵称 or 邮箱」嵌套 or 匹配；
 * 列表四个汇总字段批量装配：一次 in 查询当页用户的 CREATED 订单后内存统计
 * orderCount/totalSpend，一次 in 查询当页用户全部持券后内存统计
 * couponTotal/couponAvailable（可用 = 未使用且未过期，与 C 端口径一致）。
 * getUserDetail 聚合订单（全部、id 倒序）、收藏（总数 + 最近 5 个，联查商品快照）、
 * 持券（联查模板，status 懒判定展示态 0/1/2）、订阅（按邮箱匹配 ACTIVE 记录）。
 * updateStatus 只更新状态列；禁用不允许作用于 ADMIN 账号（防止自锁），
 * 禁用后旧 token 由 JwtAuthenticationFilter 的 status 回查即时拒绝（P0 已实现）。</p>
 */
@Service
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User> implements AdminUserService {

  /** 用户详情收藏区块展示的最近收藏商品数量上限 */
  private static final int FAVORITE_PREVIEW_LIMIT = 5;

  /** 订单 Mapper（列表消费汇总与详情订单记录装配） */
  @Resource
  private OrderMapper orderMapper;
  /** 用户持券 Mapper（列表持券汇总与详情持券列表装配） */
  @Resource
  private UserCouponMapper userCouponMapper;
  /** 券模板 Mapper（详情持券列表装配券名） */
  @Resource
  private CouponMapper couponMapper;
  /** 用户收藏 Mapper（详情收藏区块装配） */
  @Resource
  private UserFavoriteMapper userFavoriteMapper;
  /** 商品 Mapper（详情收藏商品卡片联查） */
  @Resource
  private ProductMapper productMapper;
  /** 邮件订阅 Mapper（详情订阅状态按邮箱匹配） */
  @Resource
  private SubscriptionMapper subscriptionMapper;

  @Override
  public PageResult<AdminUserVO> pageUsers(long page, long size, String keyword, String status) {
    if (StringUtils.hasText(status)
        && !StatusConstants.ACTIVE.equals(status) && !StatusConstants.INACTIVE.equals(status)) {
      throw new BusinessException(400, "状态筛选仅支持 ACTIVE 或 INACTIVE");
    }
    // 页码 clamp 到 [1, 10000]（与商品/套餐侧一致），防止深分页偏移量溢出或拖垮查询；每页条数 clamp 到 [1, 100]
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(size, 1), 100);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    // 关键字三字段 or 匹配（用户名/昵称/邮箱），避免 and 连接导致必须同时命中
    LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
    if (StringUtils.hasText(status)) {
      query.eq(User::getStatus, status);
    }
    if (likeKeyword != null) {
      query.and(nested -> nested
          .like(User::getUsername, likeKeyword)
          .or()
          .like(User::getNickname, likeKeyword)
          .or()
          .like(User::getEmail, likeKeyword));
    }
    query.orderByDesc(User::getId);

    IPage<User> userPage = baseMapper.selectPage(Page.of(safePage, safeSize), query);

    List<User> users = userPage.getRecords();
    List<Long> userIds = users.stream().map(User::getId).toList();
    Map<Long, List<OrderEntity>> createdOrdersByUserId = loadCreatedOrdersGrouped(userIds);
    Map<Long, List<UserCoupon>> couponsByUserId = loadCouponsGrouped(userIds);
    LocalDateTime now = LocalDateTime.now();

    return new PageResult<>(
        users.stream()
            .map(user -> toVO(user, createdOrdersByUserId.getOrDefault(user.getId(), List.of()),
                couponsByUserId.getOrDefault(user.getId(), List.of()), now))
            .toList(),
        userPage.getTotal(),
        userPage.getCurrent(),
        userPage.getSize()
    );
  }

  @Override
  public AdminUserDetailVO getUserDetail(Long id) {
    User user = requireUser(id);

    // 订单记录：该用户全部订单（含已取消），按订单 id 倒序
    List<AdminUserOrderVO> orders = orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
            .eq(OrderEntity::getUserId, id)
            .orderByDesc(OrderEntity::getId))
        .stream()
        .map(order -> new AdminUserOrderVO(
            order.getOrderNo(), order.getTotalAmount(), order.getStatus(), order.getCreatedAt()))
        .toList();

    // 收藏：总数 + 最近 5 个（按收藏 id 倒序，与收藏时间同序），联查商品快照
    Long favoriteTotal = userFavoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
        .eq(UserFavorite::getUserId, id));
    List<UserFavorite> recentFavorites = userFavoriteMapper.selectList(Wrappers.<UserFavorite>lambdaQuery()
        .eq(UserFavorite::getUserId, id)
        .orderByDesc(UserFavorite::getId)
        .last("LIMIT " + FAVORITE_PREVIEW_LIMIT));
    List<AdminUserFavoriteProductVO> favoriteProducts = assembleFavoriteProducts(recentFavorites);

    // 持券：联查模板取券名，status 懒判定展示态（0/1/2，与 C 端我的券口径一致），按持券 id 倒序
    List<UserCoupon> userCoupons = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
        .eq(UserCoupon::getUserId, id)
        .orderByDesc(UserCoupon::getId));
    List<AdminUserCouponVO> coupons = assembleCoupons(userCoupons);

    // 订阅：按用户邮箱匹配 ACTIVE 订阅记录（订阅表带逻辑删除，MP 自动过滤）
    boolean subscribed = StringUtils.hasText(user.getEmail())
        && subscriptionMapper.selectCount(Wrappers.<Subscription>lambdaQuery()
            .eq(Subscription::getEmail, user.getEmail())
            .eq(Subscription::getStatus, StatusConstants.ACTIVE)) > 0;

    return new AdminUserDetailVO(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getEmail(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getRole(),
        orders,
        new AdminUserFavoriteVO(favoriteTotal == null ? 0 : favoriteTotal, favoriteProducts),
        coupons,
        new AdminUserSubscriptionVO(subscribed)
    );
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    User user = requireUser(id);
    // 禁用不允许作用于管理员账号（防止自锁）；启用无限制
    if (StatusConstants.INACTIVE.equals(status) && StatusConstants.ROLE_ADMIN.equals(user.getRole())) {
      throw new BusinessException(400, "管理员账号不允许禁用");
    }
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护。
    // 禁用后该用户已签发的 token 由 JwtAuthenticationFilter 的 status 回查即时拒绝（P0 已实现，无需额外处理）
    baseMapper.update(null, Wrappers.<User>lambdaUpdate()
        .eq(User::getId, id)
        .set(User::getStatus, status));
  }

  /** 组装列表行：有效订单数/实付总额（仅 CREATED）+ 持券数/可用券数（未使用且未过期） */
  private AdminUserVO toVO(User user, List<OrderEntity> createdOrders, List<UserCoupon> userCoupons,
      LocalDateTime now) {
    BigDecimal totalSpend = createdOrders.stream()
        .map(OrderEntity::getTotalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    long couponAvailable = userCoupons.stream()
        .filter(coupon -> isCouponAvailable(coupon, now))
        .count();
    return new AdminUserVO(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getEmail(),
        user.getStatus(),
        user.getCreatedAt(),
        createdOrders.size(),
        totalSpend,
        userCoupons.size(),
        couponAvailable
    );
  }

  /** 判定持券可用：未使用且未过期（与 C 端我的券 available 口径一致） */
  private boolean isCouponAvailable(UserCoupon userCoupon, LocalDateTime now) {
    Integer status = userCoupon.getStatus();
    return UserCoupon.STATUS_UNUSED == (status == null ? UserCoupon.STATUS_UNUSED : status)
        && userCoupon.getExpiresAt() != null
        && userCoupon.getExpiresAt().isAfter(now);
  }

  /** 批量装载当页用户的有效订单（status=CREATED）→ 按用户分组映射（orderCount/totalSpend 统计口径） */
  private Map<Long, List<OrderEntity>> loadCreatedOrdersGrouped(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
            .in(OrderEntity::getUserId, userIds)
            .eq(OrderEntity::getStatus, StatusConstants.CREATED))
        .stream()
        .collect(Collectors.groupingBy(OrderEntity::getUserId));
  }

  /** 批量装载当页用户的全部持券 → 按用户分组映射（couponTotal/couponAvailable 统计口径） */
  private Map<Long, List<UserCoupon>> loadCouponsGrouped(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
            .in(UserCoupon::getUserId, userIds))
        .stream()
        .collect(Collectors.groupingBy(UserCoupon::getUserId));
  }

  /** 联查商品装配收藏商品卡片（商品已被逻辑删除的收藏行跳过，不回显脏数据） */
  private List<AdminUserFavoriteProductVO> assembleFavoriteProducts(List<UserFavorite> favorites) {
    if (favorites.isEmpty()) {
      return List.of();
    }
    Map<Long, Product> productMap = productMapper.selectBatchIds(
            favorites.stream().map(UserFavorite::getProductId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
    return favorites.stream()
        .map(favorite -> {
          Product product = productMap.get(favorite.getProductId());
          return product == null ? null : new AdminUserFavoriteProductVO(
              product.getId(), product.getName(), product.getImageUrl(), product.getPrice());
        })
        .filter(Objects::nonNull)
        .toList();
  }

  /** 联查券模板装配持券列表：券名取模板，status 做懒判定展示态换算（0/1/2，过期不回写数据库） */
  private List<AdminUserCouponVO> assembleCoupons(List<UserCoupon> userCoupons) {
    if (userCoupons.isEmpty()) {
      return List.of();
    }
    Map<Long, Coupon> templateMap = couponMapper.selectBatchIds(
            userCoupons.stream().map(UserCoupon::getCouponId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(Coupon::getId, Function.identity()));
    LocalDateTime now = LocalDateTime.now();
    return userCoupons.stream()
        // 模板被物理删除的持券记录跳过（当前无删除路径，防御性处理）
        .filter(userCoupon -> templateMap.containsKey(userCoupon.getCouponId()))
        .map(userCoupon -> {
          Coupon template = templateMap.get(userCoupon.getCouponId());
          int displayStatus = userCoupon.getStatus() == null ? UserCoupon.STATUS_UNUSED : userCoupon.getStatus();
          boolean notExpired = userCoupon.getExpiresAt() != null && userCoupon.getExpiresAt().isAfter(now);
          if (displayStatus == UserCoupon.STATUS_UNUSED && !notExpired) {
            // 数据库不回写过期状态，仅输出展示态 2
            displayStatus = UserCoupon.STATUS_EXPIRED;
          }
          return new AdminUserCouponVO(template.getName(), displayStatus, userCoupon.getExpiresAt());
        })
        .toList();
  }

  /** 按 id 查询用户，不存在或已删除时抛 404 */
  private User requireUser(Long id) {
    User user = baseMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }
    return user;
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
