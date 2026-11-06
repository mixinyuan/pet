<script setup lang="ts">
/**
 * 数据看板：骨架版里唯一接真实数据的模块（GET /api/admin/dashboard）。
 * 统计卡片 + 最近订单 + 热销 TOP5 + 库存预警，迁移时在此基础上加图表。
 */
import { onMounted, ref } from 'vue'

import { fetchAdminDashboard } from '@/api/dashboard'
import type { AdminDashboardVO } from '@/types/api'

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

function formatAmount(value: number | undefined) {
  return `¥${(value ?? 0).toFixed(2)}`
}

function formatDelta(delta: number | undefined) {
  if (!delta) return '与昨日持平'
  return delta > 0 ? `较昨日 +${delta}` : `较昨日 ${delta}`
}
</script>

<template>
  <div v-loading="loading">
    <template v-if="dashboard">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card shadow="never">
            <el-statistic title="今日订单" :value="dashboard.summary.todayOrderCount" />
            <div class="stat-extra">{{ formatDelta(dashboard.summary.todayOrderDelta) }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <el-statistic title="今日销售额" :value="dashboard.summary.todaySalesAmount" :precision="2" prefix="¥" />
            <div class="stat-extra">仅统计有效订单</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <el-statistic title="用户总数" :value="dashboard.summary.userTotal" />
            <div class="stat-extra">其中管理员 {{ dashboard.summary.adminCount }} 人</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <el-statistic title="库存预警" :value="dashboard.summary.lowStockCount" />
            <div class="stat-extra warn">库存低于 20 的商品数</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dash-row">
        <el-col :span="14">
          <el-card shadow="never" header="最近订单">
            <el-table :data="dashboard.recentOrders" size="small">
              <el-table-column prop="orderNo" label="订单号" min-width="170" />
              <el-table-column prop="buyerName" label="下单人" min-width="110" />
              <el-table-column prop="itemCount" label="件数" width="70" />
              <el-table-column label="实付" width="100">
                <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'CREATED' ? 'success' : 'info'" size="small">
                    {{ row.status === 'CREATED' ? '已创建' : '已取消' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card shadow="never" header="热销 TOP5">
            <el-table :data="dashboard.topProducts" size="small">
              <el-table-column prop="productName" label="商品" min-width="140" />
              <el-table-column prop="soldQuantity" label="销量" width="70" />
              <el-table-column label="销售额" width="100">
                <template #default="{ row }">{{ formatAmount(row.salesAmount) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-row class="dash-row">
        <el-col :span="24">
          <el-card shadow="never" header="库存预警（stock < 20，含已下架）">
            <el-table :data="dashboard.lowStockProducts" size="small">
              <el-table-column prop="productName" label="商品" min-width="180" />
              <el-table-column prop="sku" label="SKU" min-width="140" />
              <el-table-column prop="stock" label="库存" width="80" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                    {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!loading" description="看板数据加载失败，请稍后重试" />
  </div>
</template>

<style scoped>
.stat-extra {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.stat-extra.warn {
  color: #d97706;
}

.dash-row {
  margin-top: 16px;
}
</style>
