/**
 * 收藏状态中心（Pinia）：迁移旧 script.js 的 favoriteIds 集合及其同步逻辑
 * （loadFavorites / syncFavoriteButtons / toggleFavorite / resetFavoritesAndCoupons 的收藏部分）。
 *
 * - 登录用户经 GET /api/favorites 初始化已收藏商品 id 集合（游客为空集合）；
 * - 切换走乐观更新：先改本地态再发 POST/DELETE /api/favorites/{id}，失败回滚；
 * - 爱心按钮的选中态由各视图经 isFavorite 派生读取，替代旧页的 DOM 同步。
 */
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'

import { addFavorite, fetchFavorites, removeFavorite } from '@/api/favorite'
import { useAuthStore } from '@/stores/auth'

export const useFavoriteStore = defineStore('favorite', {
  state: () => ({
    /** 已收藏商品 id 集合（登录后经接口初始化，游客为空集合） */
    ids: new Set<number>(),
    /** 是否成功拉取过收藏列表（登录后懒加载一次，失败保持 false 便于重试） */
    loaded: false,
    /** 提交中的商品 id（防止爱心连点重复请求） */
    pendingIds: new Set<number>(),
  }),

  getters: {
    /** 返回判断函数：商品是否已收藏（供模板/组件按商品 id 查询） */
    isFavorite: (state) => (productId: number) => state.ids.has(Number(productId)),
  },

  actions: {
    /**
     * 拉取收藏列表并覆盖本地 id 集合（强制刷新，返回列表供收藏页渲染）。
     * 失败时抛出，由拦截器统一弹错，本地集合保持不变。
     */
    async loadFavorites() {
      const list = await fetchFavorites()
      this.ids = new Set((list || []).map((item) => Number(item.productId)))
      this.loaded = true
      return list
    },

    /**
     * 登录后首次懒加载：游客直接清空；已加载过则跳过。
     * 失败静默降级（错误已由拦截器弹出），爱心按钮保持当前态等待下次重试。
     */
    async ensureFavorites() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.resetFavorites()
        return
      }
      if (this.loaded) return
      try {
        await this.loadFavorites()
      } catch {
        // 首次加载失败不打断浏览，loaded 保持 false，再次进入时重试
      }
    },

    /**
     * 切换收藏（乐观更新，失败回滚），返回切换后的最终收藏态。
     * 游客拦截在视图层（引导登录），这里假定已登录。
     */
    async toggleFavorite(productId: number): Promise<boolean> {
      const id = Number(productId)
      if (this.pendingIds.has(id)) return this.ids.has(id)

      const willFavorite = !this.ids.has(id)
      if (willFavorite) {
        this.ids.add(id)
      } else {
        this.ids.delete(id)
      }
      this.pendingIds.add(id)

      try {
        if (willFavorite) {
          await addFavorite(id)
        } else {
          await removeFavorite(id)
        }
        ElMessage.success(willFavorite ? '已加入收藏' : '已取消收藏')
        return willFavorite
      } catch {
        // 失败回滚乐观更新（错误提示由拦截器统一弹出）
        if (willFavorite) {
          this.ids.delete(id)
        } else {
          this.ids.add(id)
        }
        return !willFavorite
      } finally {
        this.pendingIds.delete(id)
      }
    },

    /** 重置收藏态（登出/登录失效时调用，对齐旧页 resetFavoritesAndCoupons 的收藏部分） */
    resetFavorites() {
      this.ids = new Set<number>()
      this.loaded = false
      this.pendingIds = new Set<number>()
    },
  },
})
