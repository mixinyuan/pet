package com.pawbites.petshop.service.impl;
import com.pawbites.petshop.service.CategoryService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.mapper.CategoryMapper;
import com.pawbites.petshop.vo.category.CategoryVO;

@Service
public class CategoryServiceImpl implements CategoryService {

  private final CategoryMapper categoryMapper;

  public CategoryServiceImpl(CategoryMapper categoryMapper) {
    this.categoryMapper = categoryMapper;
  }

  @Override
  public List<CategoryVO> listActiveCategories() {
    return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
            .eq(Category::getStatus, StatusConstants.ACTIVE)
            .orderByAsc(Category::getSortOrder)
            .orderByAsc(Category::getId))
        .stream()
        .map(category -> new CategoryVO(category.getId(), category.getCode(), category.getName(), category.getDescription()))
        .toList();
  }
}
