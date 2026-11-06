package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminOrderService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import com.pawbites.petshop.entity.OrderItem;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.entity.UserCoupon;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CouponMapper;
import com.pawbites.petshop.mapper.OrderItemMapper;
import com.pawbites.petshop.mapper.OrderMapper;
import com.pawbites.petshop.mapper.UserCouponMapper;
import com.pawbites.petshop.mapper.UserMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.OrderService;
import com.pawbites.petshop.vo.admin.AdminOrderDetailVO;
import com.pawbites.petshop.vo.admin.AdminOrderItemVO;
import com.pawbites.petshop.vo.admin.AdminOrderUserVO;
import com.pawbites.petshop.vo.admin.AdminOrderVO;

/**
 * 管理端订单服务实现
 * <p>pageOrders 沿用管理端商品/套餐同款查询套路（页码/条数 clamp、状态取值校验、
 * like 通配符经 escapeLike 转义），差异在于：订单号精确匹配用 eq；keyword 先反查
 * users 拿到用户名/昵称命中的用户 id 集，再以「user_id IN (...) OR guest_id 前缀」
 * 的嵌套 or 条件一次查全（游客不可注册，只能按 guestId 前缀匹配）；from/to 按闭区间
 * 折算为 [当天 00:00:00, 当天 23:59:59] 的 created_at 范围（本系统首个时间范围查询）。
 * 列表/详情的下单人、商品件数、用券信息均批量装载（in 查询一次取回后内存分组，消除 N+1）；
 * 用券信息从 user_coupon 按 order_id 反查券模板，取消订单券已返还（order_id 被清空）
 * 时自然为 null。cancelOrder 委托 {@link OrderService#cancelOrderByAdmin} 复用 C 端
 * 同一套取消逻辑（回补库存 + 退券 + 审计写入），本类不做任何取消副作用实现。</p>
 */
@Service
public class AdminOrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements AdminOrderService {

  /** 游客展示名截取的 guestId 前缀长度 */
  private static final int GUEST_NAME_PREFIX_LENGTH = 6;

  /** 订单条目 Mapper（商品件数汇总与详情快照装配） */
  @Resource
  private OrderItemMapper orderItemMapper;
  /** 用户 Mapper（关键字反查下单用户、列表/详情装配下单人信息） */
  @Resource
  private UserMapper userMapper;
  /** 用户持券 Mapper（按 order_id 反查订单用券） */
  @Resource
  private UserCouponMapper userCouponMapper;
  /** 券模板 Mapper（用券信息装配券名/面额） */
  @Resource
  private CouponMapper couponMapper;
  /** 订单服务（管理员取消复用 C 端同一套取消逻辑） */
  @Resource
  private OrderService orderService;

  @Override
  public PageResult<AdminOrderVO> pageOrders(long page, long size, String orderNo, String keyword, String status,
      LocalDate from, LocalDate to) {
    if (StringUtils.hasText(status)
        && !StatusConstants.CREATED.equals(status) && !StatusConstants.CANCELLED.equals(status)) {
      throw new BusinessException(400, "订单状态筛选仅支持 CREATED 或 CANCELLED");
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw new BusinessException(400, "起始日期不能晚于结束日期");
    }
    // 页码 clamp 到 [1, 10000]（与商品/套餐侧一致），防止深分页偏移量溢出或拖垮查询；每页条数 clamp 到 [1, 100]
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(size, 1), 100);
    String trimmedOrderNo = StringUtils.hasText(orderNo) ? orderNo.trim() : null;
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    IPage<OrderEntity> orderPage = baseMapper.selectPage(
        Page.of(safePage, safeSize),
        buildQuery(trimmedOrderNo, likeKeyword, status, from, to)
    );

    List<OrderEntity> orders = orderPage.getRecords();
    Map<Long, User> userMap = loadUsers(orders);
    Map<Long, Integer> itemCountMap = loadItemCountMap(orders);
    Map<Long, UserCoupon> couponByOrderId = loadUsedCouponByOrderId(orders);
    Map<Long, Coupon> couponTemplateMap = loadCouponTemplates(couponByOrderId.values());

    return new PageResult<>(
        orders.stream()
            .map(order -> toRowVO(order,
                userMap.get(order.getUserId()),
                itemCountMap.getOrDefault(order.getId(), 0),
                couponByOrderId.get(order.getId()),
                couponTemplateMap))
            .toList(),
        orderPage.getTotal(),
        orderPage.getCurrent(),
        orderPage.getSize()
    );
  }

  @Override
  public AdminOrderDetailVO getOrderDetail(Long id) {
    return assembleDetail(requireOrder(id));
  }

  @Transactional
  @Override
  public AdminOrderDetailVO cancelOrder(Long id) {
    // 复用 C 端取消链路：cancelIfCreated 抢占（写 cancel_by=ADMIN）→ 回补库存 → 退券，全部在同一事务内
    OrderEntity cancelled = orderService.cancelOrderByAdmin(id);
    return assembleDetail(cancelled);
  }

  /**
   * 组装分页查询条件：订单号 eq、状态 eq、created_at 闭区间范围、
   * keyword 命中（下单用户 id 集合 or 游客 guestId 前缀），结果按 id 倒序。
   */
  private LambdaQueryWrapper<OrderEntity> buildQuery(String orderNo, String likeKeyword, String status,
      LocalDate from, LocalDate to) {
    LambdaQueryWrapper<OrderEntity> query = Wrappers.lambdaQuery();
    if (orderNo != null) {
      query.eq(OrderEntity::getOrderNo, orderNo);
    }
    if (StringUtils.hasText(status)) {
      query.eq(OrderEntity::getStatus, status);
    }
    if (from != null) {
      // 闭区间含起始日全天：从当天 00:00:00 起
      query.ge(OrderEntity::getCreatedAt, from.atStartOfDay());
    }
    if (to != null) {
      // 闭区间含结束日全天：截至当天 23:59:59（DATETIME 列为秒精度，避免用 23:59:59.999… 被四舍五入到次日）
      query.le(OrderEntity::getCreatedAt, to.atTime(LocalTime.of(23, 59, 59)));
    }
    if (likeKeyword != null) {
      // 先反查用户名/昵称命中的用户 id，再与游客单 guestId 前缀匹配做 or 组合
      List<Long> matchedUserIds = userMapper.selectList(Wrappers.<User>lambdaQuery()
              .like(User::getUsername, likeKeyword)
              .or()
              .like(User::getNickname, likeKeyword))
          .stream().map(User::getId).toList();
      query.and(nested -> {
        if (!matchedUserIds.isEmpty()) {
          nested.in(OrderEntity::getUserId, matchedUserIds).or();
        }
        nested.likeRight(OrderEntity::getGuestId, likeKeyword);
      });
    }
    query.orderByDesc(OrderEntity::getId);
    return query;
  }

  /** 组装订单详情：列表行全部字段 + 商品快照列表 + 商品合计 + 下单用户信息 */
  private AdminOrderDetailVO assembleDetail(OrderEntity order) {
    List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
        .eq(OrderItem::getOrderId, order.getId())
        .orderByAsc(OrderItem::getId));
    List<AdminOrderItemVO> itemVOs = items.stream()
        .map(item -> new AdminOrderItemVO(
            item.getProductName(),
            item.getProductImage(),
            item.getPrice(),
            item.getQuantity(),
            item.getSubtotal()
        ))
        .toList();

    User user = order.getUserId() == null ? null : userMapper.selectById(order.getUserId());
    Map<Long, UserCoupon> couponByOrderId = loadUsedCouponByOrderId(List.of(order));
    Map<Long, Coupon> couponTemplateMap = loadCouponTemplates(couponByOrderId.values());
    AdminOrderVO row = toRowVO(order, user,
        items.stream().mapToInt(OrderItem::getQuantity).sum(),
        couponByOrderId.get(order.getId()),
        couponTemplateMap);

    return new AdminOrderDetailVO(
        row.id(),
        row.orderNo(),
        row.isGuest(),
        row.userId(),
        row.guestId(),
        row.ownerName(),
        row.itemCount(),
        row.totalAmount(),
        row.couponName(),
        row.couponAmount(),
        row.status(),
        row.createdAt(),
        row.cancelTime(),
        row.cancelBy(),
        itemVOs,
        items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
        user == null ? null : new AdminOrderUserVO(user.getUsername(), user.getNickname(), user.getEmail())
    );
  }

  /** 组装订单列表行：下单人展示名 + 商品件数 + 用券信息（券名/面额，无券为 null） */
  private AdminOrderVO toRowVO(OrderEntity order, User user, int itemCount, UserCoupon usedCoupon,
      Map<Long, Coupon> couponTemplateMap) {
    Coupon template = usedCoupon == null ? null : couponTemplateMap.get(usedCoupon.getCouponId());
    return new AdminOrderVO(
        order.getId(),
        order.getOrderNo(),
        order.getUserId() == null,
        order.getUserId(),
        order.getGuestId(),
        resolveOwnerName(order, user),
        itemCount,
        order.getTotalAmount(),
        template == null ? null : template.getName(),
        template == null ? null : template.getAmount(),
        order.getStatus(),
        order.getCreatedAt(),
        order.getCancelTime(),
        order.getCancelBy()
    );
  }

  /** 解析下单人展示名：用户单取昵称（缺省回退用户名，用户已注销时提示已注销）；游客单为「游客 + guestId 前 6 位」 */
  private String resolveOwnerName(OrderEntity order, User user) {
    if (order.getUserId() != null) {
      if (user == null) {
        // 用户已被逻辑删除（防御性兜底，正常不会出现）
        return "已注销用户";
      }
      return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
    if (!StringUtils.hasText(order.getGuestId())) {
      return "游客";
    }
    return "游客 " + order.getGuestId().substring(0, Math.min(GUEST_NAME_PREFIX_LENGTH, order.getGuestId().length()));
  }

  /** 批量装载订单涉及的下单用户映射（selectBatchIds 自带 deleted=0 过滤，已注销用户不会出现） */
  private Map<Long, User> loadUsers(List<OrderEntity> orders) {
    List<Long> userIds = orders.stream().map(OrderEntity::getUserId).filter(Objects::nonNull).distinct().toList();
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  /** 批量装载订单 → 商品件数（各条目数量之和）映射：in 查询只取 orderId/quantity 两列后内存求和 */
  private Map<Long, Integer> loadItemCountMap(List<OrderEntity> orders) {
    if (orders.isEmpty()) {
      return Collections.emptyMap();
    }
    List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
        .select(OrderItem::getOrderId, OrderItem::getQuantity)
        .in(OrderItem::getOrderId, orders.stream().map(OrderEntity::getId).toList()));
    Map<Long, Integer> itemCountMap = new HashMap<>();
    items.forEach(item -> itemCountMap.merge(item.getOrderId(), item.getQuantity(), Integer::sum));
    return itemCountMap;
  }

  /**
   * 批量装载订单 → 已核销持券映射（按 user_coupon.order_id 反查，仅取 status=1 的核销记录）。
   * 取消订单返还券时 order_id 会被清空，故已取消订单天然查不到用券信息（couponName/couponAmount 为 null）。
   */
  private Map<Long, UserCoupon> loadUsedCouponByOrderId(List<OrderEntity> orders) {
    if (orders.isEmpty()) {
      return Collections.emptyMap();
    }
    return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
            .in(UserCoupon::getOrderId, orders.stream().map(OrderEntity::getId).toList())
            .eq(UserCoupon::getStatus, UserCoupon.STATUS_USED))
        .stream()
        // 单笔订单最多核销一张券；出现脏数据时保留首行避免 toMap 冲突（防御性处理）
        .collect(Collectors.toMap(UserCoupon::getOrderId, Function.identity(), (first, second) -> first));
  }

  /** 批量装载持券记录 → 券模板映射（券名/面额随模板读取） */
  private Map<Long, Coupon> loadCouponTemplates(Collection<UserCoupon> userCoupons) {
    List<Long> templateIds = userCoupons.stream().map(UserCoupon::getCouponId).distinct().toList();
    if (templateIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return couponMapper.selectBatchIds(templateIds).stream()
        .collect(Collectors.toMap(Coupon::getId, Function.identity()));
  }

  /** 按 id 查询订单（用户单与游客单均可见），不存在或已删除时抛 404 */
  private OrderEntity requireOrder(Long id) {
    OrderEntity order = baseMapper.selectById(id);
    if (order == null) {
      throw new BusinessException(404, "订单不存在");
    }
    return order;
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
