package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminBundleService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.admin.BundleItemRequest;
import com.pawbites.petshop.dto.admin.BundleSaveRequest;
import com.pawbites.petshop.entity.Bundle;
import com.pawbites.petshop.entity.BundleItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.BundleItemMapper;
import com.pawbites.petshop.mapper.BundleMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminBundleItemVO;
import com.pawbites.petshop.vo.admin.AdminBundleVO;

/**
 * 管理端套餐服务实现
 * <p>pageBundles 沿用管理端商品同款查询套路（关键字通配符经 escapeLike 转义后 like、
 * 页码/条数 clamp、状态取值校验），差异在于：关键字仅匹配套餐名、按 sort_order,id 升序；
 * assembleVOs 批量装载明细子表与内含商品（沿用 C 端 BundleServiceImpl 的批量装配方式），
 * 商品已被逻辑删除的明细行在装配时过滤（查询侧正确过滤，不回显脏行）。
 * create/update 在事务内写 bundle 主表 + bundle_item 明细，编辑时明细整体替换
 * （关联表物理删除后重插，沿用 P1 关联表处理方式）；delete 走主表逻辑删除，
 * 明细行保留不动，查询侧过滤。套餐允许包含下架商品（C 端有置灰标记），仅要求商品存在且未删除。</p>
 */
@Service
public class AdminBundleServiceImpl extends ServiceImpl<BundleMapper, Bundle> implements AdminBundleService {

  /** 套餐明细 Mapper（明细整体替换与装配） */
  @Resource
  private BundleItemMapper bundleItemMapper;
  /** 商品 Mapper（明细引用商品的存在性校验与装配） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public PageResult<AdminBundleVO> pageBundles(long page, long size, String keyword, String status) {
    if (StringUtils.hasText(status)
        && !StatusConstants.ACTIVE.equals(status) && !StatusConstants.INACTIVE.equals(status)) {
      throw new BusinessException(400, "状态筛选仅支持 ACTIVE 或 INACTIVE");
    }
    // 页码 clamp 到 [1, 10000]，防止深分页时偏移量溢出或拖垮查询；每页条数 clamp 到 [1, 100]
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(size, 1), 100);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    // 后台视角不过滤上架状态；关键字仅匹配套餐名称；按 sort_order、id 升序（与 C 端展示顺序一致）
    IPage<Bundle> bundlePage = baseMapper.selectPage(
        Page.of(safePage, safeSize),
        Wrappers.<Bundle>lambdaQuery()
            .eq(StringUtils.hasText(status), Bundle::getStatus, status)
            .like(StringUtils.hasText(likeKeyword), Bundle::getName, likeKeyword)
            .orderByAsc(Bundle::getSortOrder)
            .orderByAsc(Bundle::getId)
    );

    return new PageResult<>(
        assembleVOs(bundlePage.getRecords()),
        bundlePage.getTotal(),
        bundlePage.getCurrent(),
        bundlePage.getSize()
    );
  }

  @Override
  public AdminBundleVO getBundle(Long id) {
    return assembleVOs(List.of(requireBundle(id))).getFirst();
  }

  @Transactional
  @Override
  public AdminBundleVO createBundle(BundleSaveRequest request) {
    ensurePriceRule(request);
    // 先完成明细有效性校验再落库，避免写了一半才因明细数据非法而回滚
    List<BundleItem> items = resolveItems(null, request.items());
    Bundle bundle = new Bundle();
    applyEditableFields(bundle, request);
    baseMapper.insert(bundle);
    insertItems(bundle.getId(), items);
    return getBundle(bundle.getId());
  }

  @Transactional
  @Override
  public AdminBundleVO updateBundle(Long id, BundleSaveRequest request) {
    requireBundle(id);
    ensurePriceRule(request);
    List<BundleItem> items = resolveItems(id, request.items());
    Bundle bundle = new Bundle();
    applyEditableFields(bundle, request);

    // 只 SET 表单覆盖的可编辑列（沿用商品管理更新写法），created_at 不触碰、updated_at 由数据库 ON UPDATE 维护
    baseMapper.update(null, Wrappers.<Bundle>lambdaUpdate()
        .eq(Bundle::getId, id)
        .set(Bundle::getName, bundle.getName())
        .set(Bundle::getDescription, bundle.getDescription())
        .set(Bundle::getPrice, bundle.getPrice())
        .set(Bundle::getOriginalPrice, bundle.getOriginalPrice())
        .set(Bundle::getDiscountLabel, bundle.getDiscountLabel())
        .set(Bundle::getSortOrder, bundle.getSortOrder())
        .set(Bundle::getStatus, bundle.getStatus()));

    // 明细整体替换：bundle_item 为纯关系表，物理删除该套餐全部旧行后重插（避免逻辑删除死行堆积）
    bundleItemMapper.deleteByBundleIdPhysically(id);
    insertItems(id, items);
    return getBundle(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireBundle(id);
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<Bundle>lambdaUpdate()
        .eq(Bundle::getId, id)
        .set(Bundle::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteBundle(Long id) {
    requireBundle(id);
    // 主表逻辑删除（deleted=1）；bundle_item 明细行保留不动，C 端/后台查询按 deleted=0 自然过滤
    baseMapper.deleteById(id);
  }

  /** 按 id 查询套餐（含下架套餐），不存在或已删除时抛 404 */
  private Bundle requireBundle(Long id) {
    Bundle bundle = baseMapper.selectById(id);
    if (bundle == null) {
      throw new BusinessException(404, "套餐不存在");
    }
    return bundle;
  }

  /** 跨字段价格规则校验：原价须不低于现价，否则报 400「原价应不低于现价」（对齐后台表单报错文案） */
  private void ensurePriceRule(BundleSaveRequest request) {
    if (request.originalPrice().compareTo(request.price()) < 0) {
      throw new BusinessException(400, "原价应不低于现价");
    }
  }

  /** 应用表单可编辑字段（去空白、补默认值：sortOrder 缺省 0、status 缺省 ACTIVE） */
  private void applyEditableFields(Bundle bundle, BundleSaveRequest request) {
    bundle.setName(request.name().trim());
    bundle.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
    bundle.setPrice(request.price());
    bundle.setOriginalPrice(request.originalPrice());
    bundle.setDiscountLabel(StringUtils.hasText(request.discountLabel()) ? request.discountLabel().trim() : null);
    bundle.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    bundle.setStatus(StringUtils.hasText(request.status()) ? request.status() : StatusConstants.ACTIVE);
  }

  /**
   * 校验并转换明细行：引用商品必须存在且未逻辑删除（MP 自动过滤已删除商品，查不到即视为不存在；
   * 不要求上架——允许套餐含下架商品，C 端有置灰标记）；同一商品不可重复（报错文案含商品名）。
   *
   * @param bundleId 归属套餐 id（新增时主表尚未落库、传 null，落库后再回填）
   * @return 待插入的明细实体列表（保持表单行序）
   */
  private List<BundleItem> resolveItems(Long bundleId, List<BundleItemRequest> itemRequests) {
    List<Long> productIds = itemRequests.stream().map(BundleItemRequest::productId).toList();
    Map<Long, Product> productMap = productMapper.selectBatchIds(productIds.stream().distinct().toList())
        .stream()
        .collect(Collectors.toMap(Product::getId, product -> product));

    List<Long> missing = productIds.stream()
        .distinct()
        .filter(productId -> !productMap.containsKey(productId))
        .toList();
    if (!missing.isEmpty()) {
      throw new BusinessException(400,
          "商品不存在：" + missing.stream().map(String::valueOf).collect(Collectors.joining("、")));
    }

    // 同一商品不可重复添加（提示去重），报错文案带商品名（存在性已校验通过，名称必可取到）
    List<Long> duplicated = productIds.stream()
        .collect(Collectors.groupingBy(productId -> productId, Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .toList();
    if (!duplicated.isEmpty()) {
      String names = duplicated.stream()
          .map(productId -> productMap.get(productId).getName())
          .collect(Collectors.joining("、"));
      throw new BusinessException(400, "套餐内商品不可重复：" + names);
    }

    return itemRequests.stream()
        .map(itemRequest -> {
          BundleItem item = new BundleItem();
          item.setBundleId(bundleId);
          item.setProductId(itemRequest.productId());
          item.setQuantity(itemRequest.quantity());
          return item;
        })
        .toList();
  }

  /** 批量插入套餐明细行（created_at/updated_at 由 MetaObjectHandler 自动填充） */
  private void insertItems(Long bundleId, List<BundleItem> items) {
    for (BundleItem item : items) {
      item.setBundleId(bundleId);
      bundleItemMapper.insert(item);
    }
  }

  /** 批量装配管理端套餐视图（明细子表 + itemCount/totalQuantity 汇总，批量装载方式沿用 C 端套餐装配） */
  private List<AdminBundleVO> assembleVOs(List<Bundle> bundles) {
    if (bundles == null || bundles.isEmpty()) {
      return List.of();
    }
    List<Long> bundleIds = bundles.stream().map(Bundle::getId).toList();
    List<BundleItem> items = bundleItemMapper.selectList(Wrappers.<BundleItem>lambdaQuery()
        .in(BundleItem::getBundleId, bundleIds)
        .orderByAsc(BundleItem::getId));
    Map<Long, List<BundleItem>> itemMap = items.isEmpty()
        ? Collections.emptyMap()
        : items.stream().collect(Collectors.groupingBy(BundleItem::getBundleId));
    Map<Long, Product> productMap = loadProducts(items);

    return bundles.stream()
        .map(bundle -> {
          // 过滤商品已逻辑删除的明细行（删除套餐只逻辑删主表，查询侧需正确过滤，不回显脏行）
          List<AdminBundleItemVO> itemVOs = itemMap.getOrDefault(bundle.getId(), List.of()).stream()
              .map(item -> toItemVO(item, productMap.get(item.getProductId())))
              .filter(Objects::nonNull)
              .toList();
          return new AdminBundleVO(
              bundle.getId(),
              bundle.getName(),
              bundle.getDescription(),
              bundle.getPrice(),
              bundle.getOriginalPrice(),
              bundle.getDiscountLabel(),
              bundle.getSortOrder(),
              bundle.getStatus(),
              itemVOs,
              itemVOs.size(),
              itemVOs.stream().mapToInt(AdminBundleItemVO::quantity).sum()
          );
        })
        .toList();
  }

  /** 批量装载明细引用的商品映射（selectBatchIds 自带 deleted=0 过滤，已逻辑删除商品不会出现） */
  private Map<Long, Product> loadProducts(List<BundleItem> items) {
    if (items.isEmpty()) {
      return Collections.emptyMap();
    }
    return productMapper.selectBatchIds(items.stream().map(BundleItem::getProductId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(Product::getId, product -> product));
  }

  /** 明细行 + 商品 → 管理端明细视图（商品已不存在时返回 null 以便过滤） */
  private AdminBundleItemVO toItemVO(BundleItem item, Product product) {
    if (product == null) {
      return null;
    }
    return new AdminBundleItemVO(
        product.getId(),
        product.getName(),
        product.getPrice(),
        product.getImageUrl(),
        item.getQuantity()
    );
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
