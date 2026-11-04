package com.pawbites.petshop.category;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawbites.petshop.common.config.StatusConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

  private final CategoryMapper categoryMapper;

  public CategoryService(CategoryMapper categoryMapper) {
    this.categoryMapper = categoryMapper;
  }

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
