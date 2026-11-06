package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.ProductSaveRequest;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminProductVO;

/**
 * 管理端商品服务
 * <p>提供后台商品维护：不过滤上架状态的分页查询（含下架商品）、详情、新增/编辑
 * （事务内写主表 + 两张关联表，编辑时整体替换关联）、上下架切换与删除（逻辑删除主表，
 * 被套餐引用时拒绝）。</p>
 */
public interface AdminProductService extends IService<Product> {

  /**
   * 分页查询商品（后台视角，不过滤上架状态）
   * <p>关键字匹配名称/SKU/口味（like，通配符转义），分类过滤经 product_category_rel 关联实现
   * （不要求分类为启用状态，后台可按停用分类筛选），结果按 id 倒序。</p>
   *
   * @param page         页码（从 1 开始，小于 1 时按 1 处理，大于 10000 时按 10000 处理）
   * @param size         每页数量（clamp 到 [1, 100]）
   * @param keyword      关键字（null 或空白表示不过滤，匹配名称/SKU/口味）
   * @param categoryCode 分类 code（null、"all" 或空白表示不过滤）
   * @param status       上架状态筛选（ACTIVE/INACTIVE，null 或空白表示不过滤）
   * @return 管理端商品视图分页结果（含分类 code 列表与标签 id 列表）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  PageResult<AdminProductVO> pageProducts(long page, long size, String keyword, String categoryCode, String status);

  /**
   * 查询商品详情（后台视角，下架商品也可查看）
   *
   * @param id 商品 id
   * @return 管理端商品视图
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）时抛出
   */
  AdminProductVO getProduct(Long id);

  /**
   * 新增商品
   * <p>事务内写 product 主表与 product_category_rel、product_tag_rel 两张关联表；
   * SKU 全局唯一，重复时报「SKU 已存在：xxx」。</p>
   *
   * @param request 商品表单
   * @return 新增后的商品视图
   * @throws com.pawbites.petshop.exception.BusinessException SKU 重复（409）、分类/标签无效（400）时抛出
   */
  AdminProductVO createProduct(ProductSaveRequest request);

  /**
   * 编辑商品
   * <p>整体替换语义：主表字段全量更新，两张关联表先物理删除旧关联行再插入新关联（事务内）。</p>
   *
   * @param id      商品 id
   * @param request 商品表单
   * @return 编辑后的商品视图
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）、SKU 重复（409）、
   *                                                          分类/标签无效（400）时抛出
   */
  AdminProductVO updateProduct(Long id, ProductSaveRequest request);

  /**
   * 切换商品上架状态
   *
   * @param id     商品 id
   * @param status 目标状态（ACTIVE/INACTIVE）
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除商品（逻辑删除主表）
   * <p>商品被任何未删除套餐的明细引用时拒绝删除（报错文案含套餐名）；
   * 订单条目为快照不受影响，不做订单引用校验。</p>
   *
   * @param id 商品 id
   * @throws com.pawbites.petshop.exception.BusinessException 商品不存在（404）、被套餐引用（400）时抛出
   */
  void deleteProduct(Long id);
}
