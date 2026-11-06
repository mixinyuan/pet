<script setup lang="ts">
/**
 * 首页 hero 区：纯静态展示（文案与图片迁移自旧 index.html）。
 * 旧 vision.js 是独立的 Node 图片转文字 CLI 工具，与页面视觉效果无关，无需迁移。
 */
</script>

<template>
  <section class="hero">
    <div class="hero-copy">
      <p class="eyebrow">天然原料 · 猫狗都爱</p>
      <h1>给毛孩子的日常奖励，认真一点也更香一点。</h1>
      <p class="hero-text">
        精选冻干、洁齿棒与训练小食，低盐低脂、批次检测，让每一次奖励都吃得安心。
      </p>
      <div class="hero-actions">
        <RouterLink class="primary-action" :to="{ name: 'shop-home', hash: '#products' }">立即选购</RouterLink>
        <RouterLink class="secondary-action" :to="{ name: 'shop-home', hash: '#products' }">查看热销</RouterLink>
      </div>
      <div class="trust-strip" aria-label="服务保障">
        <span>48小时发货</span>
        <span>满99包邮</span>
        <span>7天无忧售后</span>
      </div>
    </div>
    <div class="hero-media" aria-label="宠物零食商品陈列">
      <img
        src="https://images.unsplash.com/photo-1589924691995-400dc9ecc119?auto=format&fit=crop&w=1100&q=82"
        alt="宠物零食与狗狗"
      />
      <div class="hero-badge">
        <strong>今日热卖</strong>
        <span>鸡肉冻干 2件85折</span>
      </div>
    </div>
  </section>
</template>
