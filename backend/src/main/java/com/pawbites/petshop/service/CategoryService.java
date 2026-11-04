package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Category;

import com.pawbites.petshop.vo.category.CategoryVO;
import java.util.List;

/**
 * 商品分类服务
 * <p>提供有效分类列表查询，供商城导航与商品筛选使用。</p>
 */
public interface CategoryService extends IService<Category> {

  /**
   * 查询全部有效分类
   * <p>按排序值与 id 升序返回。</p>
   *
   * @return 分类视图列表
   */
  List<CategoryVO> listActiveCategories();
}
