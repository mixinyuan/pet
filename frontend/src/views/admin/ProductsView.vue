<script setup lang="ts">
/**
 * 商品管理页：商品列表 / 分类管理 / 标签管理 三个子标签（对齐旧版 admin-console 的三子标签结构）。
 *
 * 父组件持有分类 + 标签两份引用数据（对应旧版 ensureRefData）：
 * - 商品列表的分类筛选下拉、分类/标签名称映射、商品表单的 chips 选择都从这里取；
 * - 切换到分类/标签子标签或任一子标签完成写操作后强制刷新（引用数 productCount 会变化）。
 */
import { onMounted, ref } from 'vue'

import CategoryTab from './products/CategoryTab.vue'
import ProductListTab from './products/ProductListTab.vue'
import TagTab from './products/TagTab.vue'
import { fetchAdminCategories } from '@/api/admin/category'
import { fetchAdminTags } from '@/api/admin/tag'
import type { AdminCategoryVO, AdminTagVO } from '@/types/admin'

const activeTab = ref('products')

const categories = ref<AdminCategoryVO[]>([])
const tags = ref<AdminTagVO[]>([])
const refLoading = ref(false)

/** 拉取分类 + 标签引用数据（失败时保持空数组，错误提示由拦截器统一弹出） */
async function loadRefData() {
  refLoading.value = true
  try {
    const [cats, tagList] = await Promise.all([fetchAdminCategories(), fetchAdminTags()])
    categories.value = cats
    tags.value = tagList
  } catch {
    // 列表回退显示 code / 空白，不阻塞商品列表渲染
  } finally {
    refLoading.value = false
  }
}

onMounted(loadRefData)

/** 切换子标签：分类/标签页每次进入都强制刷新（与旧版 loadCats/loadTags 的 ensureRefData(true) 一致） */
function onTabChange(name: string | number) {
  if (name === 'cats' || name === 'tags') {
    loadRefData()
  }
}

/** 子标签完成写操作后通知刷新引用数据（productCount 变化） */
function handleRefChanged() {
  loadRefData()
}
</script>

<template>
  <el-card shadow="never" class="products-card">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="商品列表" name="products">
        <ProductListTab :categories="categories" :tags="tags" @ref-changed="handleRefChanged" />
      </el-tab-pane>
      <el-tab-pane label="分类管理" name="cats">
        <CategoryTab :categories="categories" :loading="refLoading" @ref-changed="handleRefChanged" />
      </el-tab-pane>
      <el-tab-pane label="标签管理" name="tags">
        <TagTab :tags="tags" :loading="refLoading" @ref-changed="handleRefChanged" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.products-card :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
</style>
