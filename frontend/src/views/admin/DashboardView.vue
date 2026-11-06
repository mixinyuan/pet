<script setup lang="ts">
/**
 * 数据看板（GET /api/admin/dashboard）：对齐 docs/prototype/admin-prototype.html 的原型布局与交互。
 *
 * - 四张统计卡（彩色图标方块 + 数值 + 说明行）；
 * - 最近订单表：行可点击 → 跳订单管理并按订单号定位（query: q + locate）；
 * - 双列榜单：热销 TOP5（名次徽标 + 缩略图 + 销量/销售额）、
 *   库存预警（缩略图 + 仅剩件数 + 「去补库存」→ 跳商品管理并直接拉起编辑弹窗，query: edit）。
 * 视觉规格（间距/配色/字号）取自原型 pa-stat/pa-rank/pa-duo 系列样式。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart, User, Wallet, Warning } from '@element-plus/icons-vue'

import { fetchAdminDashboard } from '@/api/dashboard'
import type { AdminDashboardLowStockVO, AdminDashboardRecentOrderVO, AdminDashboardVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

/** 缩略图占位色块调色板（与管理台其余模块一致） */
const THUMB_COLORS = [
  '#7f9cf5', '#68d391', '#f6ad55', '#f687b3', '#63b3ed', '#f6e05e',
  '#b794f4', '#4fd1c5', '#fc8181', '#9ae6b4', '#fbd38d', '#90cdf4',
  '#ecc94b', '#fed7d7',
]

const router = useRouter()
const dashboard = ref<AdminDashboardVO | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    dashboard.value = await fetchAdminDashboard()
  } catch {
    // 错误提示由拦截器统一弹出
  } finally {
    loading.value = false
  }
})

/* ---------------- 展示辅助 ---------------- */

function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function thumbColor(seed: number) {
  return THUMB_COLORS[Math.abs(Number.isFinite(seed) ? seed : 0) % THUMB_COLORS.length]
}

/* ---------------- 原型交互：订单定位 / 去补库存 ---------------- */

/** 点击最近订单行：跳订单管理并按订单号定位（locate 标记触发落地提示） */
function gotoOrder(row: AdminDashboardRecentOrderVO) {
  router.push({ path: '/admin/orders', query: { q: row.orderNo, locate: '1' } })
}

/** 点击「去补库存」：跳商品管理并直接拉起该商品的编辑弹窗 */
function restock(row: AdminDashboardLowStockVO) {
  router.push({ path: '/admin/products', query: { edit: String(row.productId) } })
}
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <template v-if="dashboard">
      <!-- 统计卡：图标色 #409EFF/#67C23A/#909399/#E6A23C，与原型 stat() 一致 -->
      <div class="stat-grid">
        <div class="stat">
          <span class="stat-ico" style="background: #409eff"><el-icon><ShoppingCart /></el-icon></span>
          <div>
            <div class="stat-label">今日订单</div>
            <div class="stat-value">{{ dashboard.summary.todayOrderCount }}</div>
            <div class="stat-delta">
              <template v-if="dashboard.summary.todayOrderDelta !== 0">
                <b :class="dashboard.summary.todayOrderDelta > 0 ? 'up' : 'down'">
                  {{ dashboard.summary.todayOrderDelta > 0 ? '+' : '' }}{{ dashboard.summary.todayOrderDelta }}
                </b>
                较昨日
              </template>
              <template v-else>与昨日持平</template>
            </div>
          </div>
        </div>
        <div class="stat">
          <span class="stat-ico" style="background: #67c23a"><el-icon><Wallet /></el-icon></span>
          <div>
            <div class="stat-label">今日销售额</div>
            <div class="stat-value">{{ formatMoney(dashboard.summary.todaySalesAmount) }}</div>
            <div class="stat-delta">仅统计有效（已创建）订单</div>
          </div>
        </div>
        <div class="stat">
          <span class="stat-ico" style="background: #909399"><el-icon><User /></el-icon></span>
          <div>
            <div class="stat-label">注册用户</div>
            <div class="stat-value">{{ dashboard.summary.userTotal }}</div>
            <div class="stat-delta">含管理员 {{ dashboard.summary.adminCount }} 名</div>
          </div>
        </div>
        <div class="stat">
          <span class="stat-ico" style="background: #e6a23c"><el-icon><Warning /></el-icon></span>
          <div>
            <div class="stat-label">库存预警</div>
            <div class="stat-value">{{ dashboard.summary.lowStockCount }}</div>
            <div class="stat-delta">库存低于 20 件</div>
          </div>
        </div>
      </div>

      <!-- 最近订单：行可点击跳转订单管理定位 -->
      <el-card shadow="never" class="dash-card">
        <div class="card-hd">
          <h2>最近订单</h2>
          <p>点击行可跳转订单管理并定位该订单</p>
        </div>
        <el-table
          :data="dashboard.recentOrders"
          size="small"
          row-class-name="click-row"
          empty-text="暂无订单"
          @row-click="gotoOrder"
        >
          <el-table-column label="订单号" min-width="180">
            <template #default="{ row }">
              <span class="mono">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">{{ row.ownerName || '—' }}</template>
          </el-table-column>
          <el-table-column label="件数" width="70" align="right">
            <template #default="{ row }">{{ row.itemCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="实付" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <!-- 原型 p-blue/p-gray 药丸：已创建蓝、已取消灰 -->
              <el-tag v-if="row.status === 'CREATED'" size="small">已创建</el-tag>
              <el-tag v-else size="small" type="info">已取消</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 双列榜单：热销 TOP5 / 库存预警 -->
      <div class="duo">
        <el-card shadow="never" class="rank-card">
          <div class="card-hd">
            <h2>热销 TOP 5</h2>
            <p>按有效订单销量聚合</p>
          </div>
          <template v-if="dashboard.topProducts.length">
            <div v-for="(t, i) in dashboard.topProducts" :key="t.productId" class="rank">
              <span class="rank-no" :class="`top${i + 1}`">{{ i + 1 }}</span>
              <el-image :src="t.imageUrl || undefined" fit="cover" class="rank-thumb">
                <template #error>
                  <div class="rank-thumb-fb" :style="{ background: thumbColor(t.productId) }">
                    {{ t.productName.slice(0, 1) }}
                  </div>
                </template>
              </el-image>
              <span class="rank-name" :title="t.productName">{{ t.productName }}</span>
              <span class="rank-dim">销 {{ t.soldQuantity }} 件</span>
              <b class="rank-amt">{{ formatMoney(t.salesAmount) }}</b>
            </div>
          </template>
          <div v-else class="rank-empty">暂无销量数据</div>
        </el-card>

        <el-card shadow="never" class="rank-card">
          <div class="card-hd">
            <h2>库存预警</h2>
            <p>库存 &lt; 20 件，点击可去补库存</p>
          </div>
          <template v-if="dashboard.lowStockProducts.length">
            <div v-for="p in dashboard.lowStockProducts" :key="p.productId" class="rank">
              <!-- 库存预警 VO 无图片字段，统一色块占位（与旧版一致） -->
              <div class="rank-thumb-block" :style="{ background: thumbColor(p.productId) }">
                {{ (p.name || '·').slice(0, 1) }}
              </div>
              <span class="rank-name" :title="p.name">{{ p.name }}</span>
              <span class="lowstock">仅剩 {{ p.stock }} 件</span>
              <el-button link type="primary" class="restock-btn" @click="restock(p)">去补库存</el-button>
            </div>
          </template>
          <div v-else class="rank-empty">暂无预警</div>
        </el-card>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="看板数据加载失败，请稍后重试" />
  </div>
</template>

<style scoped>
/* ---------- 统计卡（原型 pa-stat 系列） ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.stat-ico {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.stat-label {
  font-size: 12.5px;
  color: #9ca3af;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.25;
}

.stat-delta {
  font-size: 11.5px;
  color: #9ca3af;
}

.stat-delta b {
  font-weight: 500;
}

.up {
  color: #67c23a;
}

.down {
  color: #f56c6c;
}

/* ---------- 卡片头部（原型 pa-card-hd） ---------- */
.dash-card {
  margin-top: 14px;
}

.card-hd {
  margin-bottom: 10px;
}

.card-hd h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
}

.card-hd p {
  font-size: 12.5px;
  color: #9ca3af;
  margin: 0;
}

/* ---------- 可点击订单行 ---------- */
.dashboard :deep(.click-row) {
  cursor: pointer;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

/* ---------- 双列榜单（原型 pa-duo / pa-rank） ---------- */
.duo {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.rank {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px dashed #e5e7eb;
  font-size: 13px;
}

.rank:last-child {
  border-bottom: 0;
}

.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.rank-no.top1 {
  background: #fdf6ec;
  color: #e6a23c;
}

.rank-no.top2 {
  background: #f4f4f5;
  color: #909399;
}

.rank-no.top3 {
  background: #fdf6ec;
  color: #c78a5a;
}

.rank-thumb {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  flex: none;
  overflow: hidden;
}

.rank-thumb-fb {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

/* 低库存色块：直接作为 flex 子项，必须定宽且不许收缩
   （不能复用 rank-thumb-fb 的 width:100%，否则会撑满整行把商品名挤成 0 宽） */
.rank-thumb-block {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.rank-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-dim {
  color: #9ca3af;
  flex: none;
}

.rank-amt {
  flex: none;
  font-variant-numeric: tabular-nums;
}

.lowstock {
  flex: none;
  color: #e6a23c;
  font-size: 12.5px;
}

.restock-btn {
  flex: none;
  padding: 0;
}

.rank-empty {
  font-size: 13px;
  color: #c0c4cc;
  padding: 8px 0;
}

/* 窄屏时双列榜单退化为单列 */
@media (max-width: 900px) {
  .duo {
    grid-template-columns: 1fr;
  }
}
</style>
