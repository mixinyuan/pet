package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.pawbites.petshop.service.admin.AdminDashboardService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.OrderEntity;
import com.pawbites.petshop.entity.OrderItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.mapper.OrderItemMapper;
import com.pawbites.petshop.mapper.OrderMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.UserMapper;
import com.pawbites.petshop.vo.admin.AdminDashboardLowStockVO;
import com.pawbites.petshop.vo.admin.AdminDashboardRecentOrderVO;
import com.pawbites.petshop.vo.admin.AdminDashboardSummaryVO;
import com.pawbites.petshop.vo.admin.AdminDashboardTopProductVO;
import com.pawbites.petshop.vo.admin.AdminDashboardVO;

/**
 * 管理端数据看板服务实现
 * <p>聚合逻辑尽量下推 SQL：计数走 MyBatis-Plus selectCount（逻辑删除自动过滤），
 * 今日销售额走 OrderMapper 的 SUM 标量查询，热销榜的 GROUP BY/SUM/ORDER BY/LIMIT
 * 走 OrderItemMapper 的聚合查询，均只取回聚合结果不拉全表进内存。
 * 日期口径沿用订单列表的闭区间约定：日历日折算为 [00:00:00, 23:59:59]，
 * 「今日」按服务器本地日期（LocalDate.now()）计算。
 * 订单类指标只统计 CREATED 状态（已取消订单不计入今日订单数/销售额），
 * 与订单列表的「有效订单」口径一致；最近订单简表、下单人展示名（ownerName）
 * 与商品件数汇总（itemCount）的装配方式对齐 AdminOrderServiceImpl
 * （in 批量装载消除 N+1，游客单展示「游客 + guestId 前 6 位」）。
 * 看板为纯只读聚合，本类不含任何写操作，也因此不挂靠单一实体的 IService。</p>
 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

  /** 游客展示名截取的 guestId 前缀长度（与订单列表口径一致） */
  private static final int GUEST_NAME_PREFIX_LENGTH = 6;

  /** 最近订单简表条数 */
  private static final int RECENT_ORDER_LIMIT = 10;

  /** 热销榜单展示条数 */
  private static final int TOP_PRODUCT_LIMIT = 5;

  /** 热销榜聚合拉取条数：比展示条数多留余量，商品已逻辑删除的行跳过后仍能取满 TOP5 */
  private static final int TOP_PRODUCT_FETCH_LIMIT = 10;

  /** 库存预警线：库存低于该值视为预警（业务梳理文档 Q6，写死常量） */
  private static final int LOW_STOCK_THRESHOLD = 20;

  /** 订单 Mapper（今日订单数计数与今日实付总额聚合） */
  @Resource
  private OrderMapper orderMapper;
  /** 订单条目 Mapper（热销榜按商品聚合销量/销售额） */
  @Resource
  private OrderItemMapper orderItemMapper;
  /** 商品 Mapper（库存预警计数/列表与热销榜商品信息补齐） */
  @Resource
  private ProductMapper productMapper;
  /** 用户 Mapper（用户总数与管理员人数计数、最近订单下单人信息装配） */
  @Resource
  private UserMapper userMapper;

  @Override
  public AdminDashboardVO getDashboard() {
    LocalDate today = LocalDate.now();
    return new AdminDashboardVO(
        buildSummary(today),
        buildRecentOrders(),
        buildTopProducts(),
        buildLowStockProducts());
  }

  /** 组装统计卡片：今日/昨日订单数与今日销售额（仅 CREATED）+ 用户/管理员/库存预警计数 */
  private AdminDashboardSummaryVO buildSummary(LocalDate today) {
    long todayOrderCount = countCreatedOrdersOf(today);
    long yesterdayOrderCount = countCreatedOrdersOf(today.minusDays(1));
    BigDecimal todaySalesAmount = orderMapper.sumCreatedAmountBetween(
        today.atStartOfDay(), today.atTime(LocalTime.of(23, 59, 59)));
    long userTotal = userMapper.selectCount(Wrappers.lambdaQuery());
    long adminCount = userMapper.selectCount(Wrappers.<User>lambdaQuery()
        .eq(User::getRole, StatusConstants.ROLE_ADMIN));
    long lowStockCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
        .lt(Product::getStock, LOW_STOCK_THRESHOLD));
    return new AdminDashboardSummaryVO(
        todayOrderCount,
        todaySalesAmount,
        (int) (todayOrderCount - yesterdayOrderCount),
        userTotal,
        adminCount,
        lowStockCount);
  }

  /** 组装最近订单简表：最近 10 条订单（含已取消）按 id 倒序，批量装载下单人与商品件数 */
  private List<AdminDashboardRecentOrderVO> buildRecentOrders() {
    List<OrderEntity> orders = orderMapper.selectPage(
        Page.of(1, RECENT_ORDER_LIMIT),
        Wrappers.<OrderEntity>lambdaQuery().orderByDesc(OrderEntity::getId)
    ).getRecords();
    if (orders.isEmpty()) {
      return List.of();
    }
    Map<Long, User> userMap = loadUsers(orders);
    Map<Long, Integer> itemCountMap = loadItemCountMap(orders);
    return orders.stream()
        .map(order -> new AdminDashboardRecentOrderVO(
            order.getId(),
            order.getOrderNo(),
            resolveOwnerName(order, userMap.get(order.getUserId())),
            itemCountMap.getOrDefault(order.getId(), 0),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt()))
        .toList();
  }

  /**
   * 组装热销 TOP5：聚合行多取几行（TOP_PRODUCT_FETCH_LIMIT）留余量，
   * 商品已逻辑删除的聚合行保留计数但跳过展示（selectBatchIds 自动过滤 deleted=1，
   * 查不到商品信息的行直接丢弃），仍能取满 5 个可展示商品。
   */
  private List<AdminDashboardTopProductVO> buildTopProducts() {
    List<OrderItemMapper.ProductSalesStats> stats =
        orderItemMapper.aggregateProductSales(TOP_PRODUCT_FETCH_LIMIT);
    if (stats.isEmpty()) {
      return List.of();
    }
    Map<Long, Product> productMap = productMapper.selectBatchIds(
            stats.stream().map(OrderItemMapper.ProductSalesStats::getProductId).toList()).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
    return stats.stream()
        .filter(stat -> productMap.containsKey(stat.getProductId()))
        .limit(TOP_PRODUCT_LIMIT)
        .map(stat -> {
          Product product = productMap.get(stat.getProductId());
          return new AdminDashboardTopProductVO(
              product.getId(),
              product.getName(),
              product.getImageUrl(),
              stat.getSoldQuantity() == null ? 0 : stat.getSoldQuantity().intValue(),
              stat.getSalesAmount());
        })
        .toList();
  }

  /** 组装库存预警列表：未删除且库存低于预警线 20 的商品（含已下架），按库存升序、id 升序 */
  private List<AdminDashboardLowStockVO> buildLowStockProducts() {
    return productMapper.selectList(Wrappers.<Product>lambdaQuery()
            .lt(Product::getStock, LOW_STOCK_THRESHOLD)
            .orderByAsc(Product::getStock)
            .orderByAsc(Product::getId))
        .stream()
        .map(product -> new AdminDashboardLowStockVO(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getStock(),
            product.getStatus()))
        .toList();
  }

  /** 统计某日 CREATED 订单数：日历日折算为 [00:00:00, 23:59:59] 闭区间（与订单列表范围查询口径一致） */
  private long countCreatedOrdersOf(LocalDate day) {
    Long count = orderMapper.selectCount(Wrappers.<OrderEntity>lambdaQuery()
        .eq(OrderEntity::getStatus, StatusConstants.CREATED)
        .ge(OrderEntity::getCreatedAt, day.atStartOfDay())
        .le(OrderEntity::getCreatedAt, day.atTime(LocalTime.of(23, 59, 59))));
    return count == null ? 0L : count;
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
    List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
        .select(OrderItem::getOrderId, OrderItem::getQuantity)
        .in(OrderItem::getOrderId, orders.stream().map(OrderEntity::getId).toList()));
    Map<Long, Integer> itemCountMap = new HashMap<>();
    items.forEach(item -> itemCountMap.merge(item.getOrderId(), item.getQuantity(), Integer::sum));
    return itemCountMap;
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
}
