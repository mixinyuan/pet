<script setup lang="ts">
/**
 * 商城首页：迁移旧静态页（index.html + script.js）的全部只读浏览区块。
 *
 * hero / 分类导航（id=categories）/ 商品列表（id=products，分页加载更多）/
 * 场景套餐（id=bundles）/ 品质保证（id=quality）/ 用户评价（id=reviews）；
 * 邮件订阅表单位于 ShopLayout 页脚（SubscribeForm，POST /api/subscriptions）。
 * 加购与收藏交互已由购物车/收藏任务接入（ProductList 内）。
 */
import { ref } from 'vue'

import BundleSection from '@/components/shop/BundleSection.vue'
import CategoryNav from '@/components/shop/CategoryNav.vue'
import ProductList from '@/components/shop/ProductList.vue'
import QualitySection from '@/components/shop/QualitySection.vue'
import ReviewSection from '@/components/shop/ReviewSection.vue'
import ShopHero from '@/components/shop/ShopHero.vue'

/** 当前分类筛选（all 表示全部），分类卡片点击后联动商品列表重载 */
const activeCategory = ref('all')
</script>

<template>
  <div class="home-page">
    <ShopHero />
    <CategoryNav :active="activeCategory" @select="activeCategory = $event" />
    <ProductList :category="activeCategory" />
    <BundleSection />
    <QualitySection />
    <ReviewSection />
  </div>
</template>
