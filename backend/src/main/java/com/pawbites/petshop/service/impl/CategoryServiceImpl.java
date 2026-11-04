package com.pawbites.petshop.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.CategoryService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.mapper.CategoryMapper;
import com.pawbites.petshop.vo.category.CategoryVO;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

  @Override
  public List<CategoryVO> listActiveCategories() {
    return baseMapper.selectList(Wrappers.<Category>lambdaQuery()
            .eq(Category::getStatus, StatusConstants.ACTIVE)
            .orderByAsc(Category::getSortOrder)
            .orderByAsc(Category::getId))
        .stream()
        .map(category -> new CategoryVO(category.getId(), category.getCode(), category.getName(), category.getDescription()))
        .toList();
  }
}
