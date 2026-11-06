package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminProductService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.admin.ProductSaveRequest;
import com.pawbites.petshop.entity.Bundle;
import com.pawbites.petshop.entity.BundleItem;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.ProductCategoryRel;
import com.pawbites.petshop.entity.ProductTag;
import com.pawbites.petshop.entity.ProductTagRel;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.BundleItemMapper;
import com.pawbites.petshop.mapper.BundleMapper;
import com.pawbites.petshop.mapper.CategoryMapper;
import com.pawbites.petshop.mapper.ProductCategoryRelMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.ProductTagMapper;
import com.pawbites.petshop.mapper.ProductTagRelMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminProductVO;

/**
 * 管理端商品服务实现
 * <p>pageProducts 与 C 端 listProducts 同款查询套路（分类经关联表解析为商品 id 集合、
 * 关键字通配符经 escapeLike 转义后 like、页码/条数 clamp），差异在于：不过滤上架状态
 * （后台要能看到下架商品）、分类筛选不要求分类启用、关键字匹配名称/SKU/口味、按 id 倒序；
 * assembleVOs 批量装载分类 code 与标签 id（沿用 C 端 enrichProducts 的批量装配方式）。
 * create/update 在事务内写主表 + 两张关联表，编辑时关联整体替换（关联表物理删除后重插，
 * 避免逻辑删除死行堆积）；delete 走主表逻辑删除，被未删除套餐引用时拒绝。</p>
 */
@Service
public class AdminProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements AdminProductService {

  /** 评分缺省值（表单未填时） */
  private static final BigDecimal DEFAULT_RATING = new BigDecimal("5.0");

  /** 分类 Mapper（分类 code 校验与解析） */
  @Resource
  private CategoryMapper categoryMapper;
  /** 商品-分类关联 Mapper（关联整体替换与装配） */
  @Resource
  private ProductCategoryRelMapper productCategoryRelMapper;
  /** 商品标签 Mapper（标签 id 校验与装配） */
  @Resource
  private ProductTagMapper productTagMapper;
  /** 商品-标签关联 Mapper（关联整体替换与装配） */
  @Resource
  private ProductTagRelMapper productTagRelMapper;
  /** 套餐 Mapper（删除前的套餐引用检查） */
  @Resource
  private BundleMapper bundleMapper;
  /** 套餐明细 Mapper（删除前的套餐引用检查） */
  @Resource
  private BundleItemMapper bundleItemMapper;

  @Override
  public PageResult<AdminProductVO> pageProducts(long page, long size, String keyword, String categoryCode, String status) {
    if (StringUtils.hasText(status)
        && !StatusConstants.ACTIVE.equals(status) && !StatusConstants.INACTIVE.equals(status)) {
      throw new BusinessException(400, "状态筛选仅支持 ACTIVE 或 INACTIVE");
    }
    // 页码 clamp 到 [1, 10000]，防止深分页时偏移量溢出或拖垮查询；每页条数 clamp 到 [1, 100]
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(size, 1), 100);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    // 分类筛选：经关联表解析出商品 id 集合（后台不要求分类为启用状态，停用分类同样可筛）
    List<Long> categoryProductIds = null;
    if (StringUtils.hasText(categoryCode) && !"all".equalsIgnoreCase(categoryCode)) {
      Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
          .eq(Category::getCode, categoryCode)
          .last("LIMIT 1"));
      if (category == null) {
        return new PageResult<>(List.of(), 0, safePage, safeSize);
      }
      categoryProductIds = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
              .eq(ProductCategoryRel::getCategoryId, category.getId()))
          .stream()
          .map(ProductCategoryRel::getProductId)
          .distinct()
          .toList();
      if (categoryProductIds.isEmpty()) {
        return new PageResult<>(List.of(), 0, safePage, safeSize);
      }
    }

    // 后台视角不过滤上架状态；关键字匹配名称/SKU/口味三字段；按 id 倒序（最新商品在前）
    IPage<Product> productPage = baseMapper.selectPage(
        Page.of(safePage, safeSize),
        Wrappers.<Product>lambdaQuery()
            .eq(StringUtils.hasText(status), Product::getStatus, status)
            .in(categoryProductIds != null, Product::getId, categoryProductIds)
            .and(StringUtils.hasText(likeKeyword), wrapper -> wrapper
                .like(Product::getName, likeKeyword)
                .or()
                .like(Product::getSku, likeKeyword)
                .or()
                .like(Product::getFlavor, likeKeyword))
            .orderByDesc(Product::getId)
    );

    return new PageResult<>(
        assembleVOs(productPage.getRecords()),
        productPage.getTotal(),
        productPage.getCurrent(),
        productPage.getSize()
    );
  }

  @Override
  public AdminProductVO getProduct(Long id) {
    return assembleVOs(List.of(requireProduct(id))).getFirst();
  }

  @Transactional
  @Override
  public AdminProductVO createProduct(ProductSaveRequest request) {
    Product product = new Product();
    applyEditableFields(product, request);
    ensureSkuUnique(product.getSku(), null);
    // 先完成分类/标签有效性校验再落库，避免写了一半才因关联数据非法而回滚
    List<Category> categories = resolveCategories(request.categories());
    List<ProductTag> tags = resolveTags(request.tags());
    try {
      baseMapper.insert(product);
    } catch (DuplicateKeyException exception) {
      // 预校验之后仍可能撞唯一键（如与已逻辑删除商品的 SKU 冲突——唯一索引不区分 deleted），统一给出明确报错
      throw new BusinessException(409, "SKU 已存在：" + product.getSku());
    }
    insertRels(product.getId(), categories, tags);
    return getProduct(product.getId());
  }

  @Transactional
  @Override
  public AdminProductVO updateProduct(Long id, ProductSaveRequest request) {
    requireProduct(id);
    Product product = new Product();
    applyEditableFields(product, request);
    ensureSkuUnique(product.getSku(), id);
    List<Category> categories = resolveCategories(request.categories());
    List<ProductTag> tags = resolveTags(request.tags());

    // 只 SET 表单覆盖的可编辑列（沿用日志管理更新写法），created_at 不触碰、updated_at 由数据库 ON UPDATE 维护
    baseMapper.update(null, Wrappers.<Product>lambdaUpdate()
        .eq(Product::getId, id)
        .set(Product::getSku, product.getSku())
        .set(Product::getName, product.getName())
        .set(Product::getPetType, product.getPetType())
        .set(Product::getFlavor, product.getFlavor())
        .set(Product::getPrice, product.getPrice())
        .set(Product::getRating, product.getRating())
        .set(Product::getStock, product.getStock())
        .set(Product::getStatus, product.getStatus())
        .set(Product::getImageUrl, product.getImageUrl())
        .set(Product::getDescription, product.getDescription()));

    // 关联整体替换：关联表为纯关系表，物理删除旧关联行后重插（无唯一键冲突风险，也避免逻辑删除死行堆积）
    productCategoryRelMapper.deleteByProductIdPhysically(id);
    productTagRelMapper.deleteByProductIdPhysically(id);
    insertRels(id, categories, tags);
    return getProduct(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireProduct(id);
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<Product>lambdaUpdate()
        .eq(Product::getId, id)
        .set(Product::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteProduct(Long id) {
    requireProduct(id);
    // 套餐引用检查：bundle_item/bundle 均带逻辑删除，MP 自动过滤已删除的套餐及其明细
    List<BundleItem> items = bundleItemMapper.selectList(Wrappers.<BundleItem>lambdaQuery()
        .eq(BundleItem::getProductId, id));
    if (!items.isEmpty()) {
      Set<Long> bundleIds = items.stream().map(BundleItem::getBundleId).collect(Collectors.toSet());
      List<Bundle> bundles = bundleMapper.selectBatchIds(bundleIds);
      if (!bundles.isEmpty()) {
        String names = bundles.stream().map(Bundle::getName).collect(Collectors.joining("、"));
        throw new BusinessException(400,
            "商品已被 " + bundles.size() + " 个套餐引用（" + names + "），请先从套餐中移除后再删除");
      }
    }
    // 主表逻辑删除（deleted=1）；关联行保留不动，C 端按 deleted=0 查询自然过滤
    baseMapper.deleteById(id);
  }

  /** 按 id 查询商品（含下架商品），不存在或已删除时抛 404 */
  private Product requireProduct(Long id) {
    Product product = baseMapper.selectById(id);
    if (product == null) {
      throw new BusinessException(404, "商品不存在");
    }
    return product;
  }

  /** 应用表单可编辑字段（去空白、补默认值：rating 缺省 5.0、status 缺省 ACTIVE） */
  private void applyEditableFields(Product product, ProductSaveRequest request) {
    product.setSku(request.sku().trim());
    product.setName(request.name().trim());
    product.setPetType(request.petType().trim());
    product.setFlavor(request.flavor().trim());
    product.setPrice(request.price());
    product.setRating(request.rating() == null ? DEFAULT_RATING : request.rating());
    product.setStock(request.stock());
    product.setStatus(StringUtils.hasText(request.status()) ? request.status() : StatusConstants.ACTIVE);
    product.setImageUrl(request.imageUrl().trim());
    product.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
  }

  /** SKU 唯一性预校验（excludeId 用于编辑时排除自身），重复抛 409 */
  private void ensureSkuUnique(String sku, Long excludeId) {
    Long count = baseMapper.selectCount(Wrappers.<Product>lambdaQuery()
        .eq(Product::getSku, sku)
        .ne(excludeId != null, Product::getId, excludeId));
    if (count != null && count > 0) {
      throw new BusinessException(409, "SKU 已存在：" + sku);
    }
  }

  /** 校验分类 code 列表（去重、不允许 all、逐个存在），返回分类实体列表 */
  private List<Category> resolveCategories(List<String> codes) {
    List<String> normalized = codes.stream()
        .map(String::trim)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    if (normalized.isEmpty()) {
      throw new BusinessException(400, "至少选择 1 个分类");
    }
    if (normalized.stream().anyMatch("all"::equalsIgnoreCase)) {
      throw new BusinessException(400, "分类不能包含系统内置的 all");
    }
    List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
        .in(Category::getCode, normalized));
    // 数据库排序规则不区分大小写，比对存在性时同样忽略大小写，避免 "Cat"/"cat" 误报不存在
    List<String> missing = normalized.stream()
        .filter(code -> categories.stream().noneMatch(category -> code.equalsIgnoreCase(category.getCode())))
        .toList();
    if (!missing.isEmpty()) {
      throw new BusinessException(400, "分类不存在：" + String.join("、", missing));
    }
    return categories;
  }

  /** 校验标签 id 列表（去重、逐个存在），返回标签实体列表；空列表直接通过（标签可选） */
  private List<ProductTag> resolveTags(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return List.of();
    }
    if (tagIds.stream().anyMatch(Objects::isNull)) {
      throw new BusinessException(400, "标签 id 不能为空");
    }
    List<Long> normalized = tagIds.stream().distinct().toList();
    List<ProductTag> tags = productTagMapper.selectBatchIds(normalized);
    Set<Long> foundIds = tags.stream().map(ProductTag::getId).collect(Collectors.toSet());
    List<Long> missing = normalized.stream().filter(tagId -> !foundIds.contains(tagId)).toList();
    if (!missing.isEmpty()) {
      throw new BusinessException(400,
          "标签不存在：" + missing.stream().map(String::valueOf).collect(Collectors.joining("、")));
    }
    return tags;
  }

  /** 批量插入商品与分类/标签的关联行（created_at/updated_at 由 MetaObjectHandler 自动填充） */
  private void insertRels(Long productId, List<Category> categories, List<ProductTag> tags) {
    for (Category category : categories) {
      ProductCategoryRel rel = new ProductCategoryRel();
      rel.setProductId(productId);
      rel.setCategoryId(category.getId());
      productCategoryRelMapper.insert(rel);
    }
    for (ProductTag tag : tags) {
      ProductTagRel rel = new ProductTagRel();
      rel.setProductId(productId);
      rel.setTagId(tag.getId());
      productTagRelMapper.insert(rel);
    }
  }

  /** 批量装配管理端商品视图（分类 code 列表 + 标签 id 列表，装载方式沿用 C 端 enrichProducts） */
  private List<AdminProductVO> assembleVOs(List<Product> products) {
    if (products == null || products.isEmpty()) {
      return List.of();
    }
    List<Long> productIds = products.stream().map(Product::getId).toList();
    Map<Long, List<String>> categoryMap = loadCategoryCodes(productIds);
    Map<Long, List<Long>> tagMap = loadTagIds(productIds);
    return products.stream()
        .map(product -> new AdminProductVO(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getPetType(),
            product.getFlavor(),
            product.getPrice(),
            product.getRating(),
            product.getStock(),
            product.getStatus(),
            product.getImageUrl(),
            product.getDescription(),
            categoryMap.getOrDefault(product.getId(), List.of()),
            tagMap.getOrDefault(product.getId(), List.of())
        ))
        .toList();
  }

  /** 批量装载商品 → 分类 code 列表映射（经关联表分组，过滤已删除分类），与 C 端同款套路 */
  private Map<Long, List<String>> loadCategoryCodes(List<Long> productIds) {
    List<ProductCategoryRel> rels = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
        .in(ProductCategoryRel::getProductId, productIds));
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<Long> categoryIds = rels.stream().map(ProductCategoryRel::getCategoryId).collect(Collectors.toSet());
    Map<Long, String> categories = categoryMapper.selectBatchIds(categoryIds).stream()
        .collect(Collectors.toMap(Category::getId, Category::getCode));
    return rels.stream()
        .collect(Collectors.groupingBy(
            ProductCategoryRel::getProductId,
            Collectors.mapping(rel -> categories.get(rel.getCategoryId()),
                Collectors.filtering(StringUtils::hasText, Collectors.toList()))
        ));
  }

  /** 批量装载商品 → 标签 id 列表映射（经关联表分组，过滤已删除标签） */
  private Map<Long, List<Long>> loadTagIds(List<Long> productIds) {
    List<ProductTagRel> rels = productTagRelMapper.selectList(Wrappers.<ProductTagRel>lambdaQuery()
        .in(ProductTagRel::getProductId, productIds));
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<Long> tagIds = rels.stream().map(ProductTagRel::getTagId).collect(Collectors.toSet());
    Set<Long> existingIds = productTagMapper.selectBatchIds(tagIds).stream()
        .map(ProductTag::getId)
        .collect(Collectors.toSet());
    return rels.stream()
        .collect(Collectors.groupingBy(
            ProductTagRel::getProductId,
            Collectors.mapping(ProductTagRel::getTagId,
                Collectors.filtering(existingIds::contains, Collectors.toList()))
        ));
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
