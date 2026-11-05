package com.pawbites.petshop.controller;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.FavoriteService;
import com.pawbites.petshop.vo.favorite.FavoriteVO;

/**
 * 收藏控制器
 * <p>路由前缀 /api/favorites，提供收藏列表、收藏与取消收藏接口。
 * 全部接口要求登录（SecurityConfig 配置 authenticated），游客不可收藏。</p>
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

  /** 收藏服务 */
  @Resource
  private FavoriteService favoriteService;

  /**
   * 查询当前用户的收藏列表（GET /api/favorites）
   * <p>按收藏时间倒序，联查商品返回卡片信息（id/名称/图片/价格/petType/flavor/评分）。</p>
   *
   * @return 收藏列表
   * @throws BusinessException 未登录（401 请先登录）时抛出
   */
  @GetMapping
  public ApiResponse<List<FavoriteVO>> list() {
    return ApiResponse.success(favoriteService.listFavorites(SecurityUtils.currentUserId()));
  }

  /**
   * 收藏指定商品（POST /api/favorites/{productId}）
   * <p>重复收藏幂等，直接返回成功。</p>
   *
   * @param productId 商品 id
   * @return 无数据的成功响应
   * @throws BusinessException 未登录（401）、商品不存在（404）时抛出
   */
  @PostMapping("/{productId}")
  public ApiResponse<Void> add(@PathVariable Long productId) {
    favoriteService.addFavorite(SecurityUtils.currentUserId(), productId);
    return ApiResponse.success();
  }

  /**
   * 取消收藏指定商品（DELETE /api/favorites/{productId}）
   * <p>未收藏时同样返回成功（幂等）。</p>
   *
   * @param productId 商品 id
   * @return 无数据的成功响应
   * @throws BusinessException 未登录（401）时抛出
   */
  @DeleteMapping("/{productId}")
  public ApiResponse<Void> remove(@PathVariable Long productId) {
    favoriteService.removeFavorite(SecurityUtils.currentUserId(), productId);
    return ApiResponse.success();
  }
}
