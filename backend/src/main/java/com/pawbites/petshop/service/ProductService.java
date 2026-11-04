package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Product;

import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.product.ProductVO;
import java.util.List;

/**
 * 商品服务
 * <p>提供商品的分类/关键字/分页列表查询、详情查询，以及批量装载分类 code 与标签名的视图装配。</p>
 */
public interface ProductService extends IService<Product> {

  /**
   * 分页查询商品列表
   * <p>仅返回有效商品；分类过滤经 product_category_rel 关联实现，关键字匹配名称/口味/宠物类型（like），
   * 结果按评分降序排列，页码从 1 起、每页数量上限 100。</p>
   *
   * @param page         页码（从 1 开始，小于 1 时按 1 处理）
   * @param size         每页数量（上限 100）
   * @param categoryCode 分类 code（null、"all" 或空白表示不过滤）
   * @param keyword      搜索关键字（null 或空白表示不过滤）
   * @return 商品视图分页结果
   */
  PageResult<ProductVO> listProducts(long page, long size, String categoryCode, String keyword);

  /**
   * 查询商品详情
   * <p>附带商品关联的分类 code 与标签名。</p>
   *
   * @param id 商品 id
   * @return 商品视图
   * @throws BusinessException 商品不存在或已下架（404）
   */
  ProductVO getProduct(Long id);

  /**
   * 批量装配商品视图
   * <p>一次性批量装载各商品关联的分类 code 与标签名，供列表与详情复用。</p>
   *
   * @param products 商品实体列表
   * @return 商品视图列表
   */
  List<ProductVO> enrichProducts(List<Product> products);
}
