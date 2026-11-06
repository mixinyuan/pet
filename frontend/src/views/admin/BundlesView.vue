<script setup lang="ts">
/**
 * 场景套餐管理页（/admin/bundles）：分页列表（关键字/上架状态筛选）+ 新增/编辑弹窗
 * （含商品明细子表编辑器）+ 上下架开关（乐观更新失败回滚）+ 删除二次确认。
 * 交互对齐旧版 admin-console 套餐模块：列表缩略图取第一件商品图、
 * 「N 种 · 共 M 件」汇总列、编辑先拉详情再回填。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { deleteAdminBundle, fetchAdminBundles, updateAdminBundleStatus } from '@/api/admin/bundle'
import type { AdminBundleVO, AdminStatus } from '@/types/admin'
import BundleEditDialog from './bundles/BundleEditDialog.vue'

/** 缩略图占位色块调色板（与旧版 THUMB_COLORS 一致） */
const THUMB_COLORS = [
  '#7f9cf5', '#68d391', '#f6ad55', '#f687b3', '#63b3ed', '#f6e05e',
  '#b794f4', '#4fd1c5', '#fc8181', '#9ae6b4', '#fbd38d', '#90cdf4',
  '#ecc94b', '#fed7d7',
]

/** 与旧版一致的每页条数（后端 size clamp 到 [1,100]） */
const PAGE_SIZE = 10

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminBundleVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({
  keyword: '',
  /** ACTIVE/INACTIVE/all */
  status: 'all' as 'all' | AdminStatus,
})

const filtersApplied = computed(() => filters.keyword.trim() !== '' || filters.status !== 'all')
const emptyText = computed(() =>
  filtersApplied.value ? '未找到匹配的套餐' : '暂无套餐，点击「新增套餐」创建',
)

async function load() {
  const params: { page: number; size: number; keyword?: string; status?: AdminStatus } = {
    page: page.value,
    size: PAGE_SIZE,
  }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.status !== 'all') params.status = filters.status
  loading.value = true
  try {
    const data = await fetchAdminBundles(params)
    rows.value = data.records
    total.value = data.total
  } catch {
    // 错误提示由拦截器统一弹出，这里只做收尾
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)

/** 搜索/筛选变化：重置到第 1 页（与旧版一致） */
function search() {
  page.value = 1
  load()
}

/* ---------------- 展示辅助 ---------------- */

function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function thumbColor(id: number) {
  return THUMB_COLORS[Math.abs(id) % THUMB_COLORS.length]
}

/* ---------------- 上下架开关（乐观更新，失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

async function handleStatusChange(row: AdminBundleVO) {
  const next = row.status // v-model 已写入目标状态
  switchingId.value = row.id
  try {
    await updateAdminBundleStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '套餐已上架' : '套餐已下架')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 失败回滚
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除（二次确认） ---------------- */

async function handleDelete(row: AdminBundleVO) {
  const confirmed = await ElMessageBox.confirm(
    '删除后 C 端套餐区立即不再展示。',
    `删除套餐「${row.name}」？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminBundle(row.id)
    ElMessage.success('套餐已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1 // 当前页删空则回退一页
    load()
  } catch {
    // 后端拒绝时拦截器已弹出报错文案
  }
}

/* ---------------- 新增/编辑弹窗（明细编辑器在子组件） ---------------- */

const dialogVisible = ref(false)
/** 当前编辑的套餐（null 表示新增） */
const editingBundle = ref<AdminBundleVO | null>(null)

function openCreate() {
  editingBundle.value = null
  dialogVisible.value = true
}

/** 编辑：传行数据给弹窗，弹窗内部再拉详情回填（与旧版一致） */
function openEdit(row: AdminBundleVO) {
  editingBundle.value = row
  dialogVisible.value = true
}

/** 弹窗保存成功后刷新列表（列表按 sort_order 升序，保持当前页刷新） */
function handleSaved() {
  load()
}
</script>

<template>
  <el-card shadow="never" class="bundles-card">
    <div class="page-head">
      <h2>场景套餐</h2>
      <p>多商品打包组合，C 端首页套餐区展示；编辑不影响历史数据</p>
    </div>

    <!-- 工具栏：新增 / 状态筛选 / 关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增套餐</el-button>
      <el-select v-model="filters.status" style="width: 130px" @change="search">
        <el-option label="全部状态" value="all" />
        <el-option label="上架中" value="ACTIVE" />
        <el-option label="已下架" value="INACTIVE" />
      </el-select>
      <div class="spacer" />
      <el-input
        v-model="filters.keyword"
        placeholder="套餐名称"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="排序" width="70" align="center">
        <template #default="{ row }">{{ row.sortOrder ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="套餐" min-width="260">
        <template #default="{ row }">
          <div class="prod-cell">
            <!-- 套餐缩略图取第一件商品的图（套餐 VO 本身无 imageUrl 字段） -->
            <el-image
              v-if="row.items && row.items.length"
              :src="row.items[0].imageUrl"
              fit="cover"
              loading="lazy"
              class="prod-thumb"
            >
              <template #error>
                <div class="prod-thumb-fb" :style="{ background: thumbColor(row.id) }">
                  {{ String(row.name || '·').slice(0, 1) }}
                </div>
              </template>
            </el-image>
            <div v-else class="prod-thumb">
              <div class="prod-thumb-fb" :style="{ background: thumbColor(row.id) }">
                {{ String(row.name || '·').slice(0, 1) }}
              </div>
            </div>
            <div class="prod-meta">
              <div class="prod-name" :title="row.name">{{ row.name }}</div>
              <div class="prod-desc" :title="row.description || ''">{{ row.description || '—' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="折扣文案" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.discountLabel" size="small" type="warning">{{ row.discountLabel }}</el-tag>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column label="现价" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.price) }}</template>
      </el-table-column>
      <el-table-column label="原价" width="100" align="right">
        <template #default="{ row }">
          <span class="origin-price">{{ formatMoney(row.originalPrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="明细" min-width="120">
        <template #default="{ row }">
          {{ row.itemCount || 0 }} 种 · 共 {{ row.totalQuantity || 0 }} 件
        </template>
      </el-table-column>
      <el-table-column label="上架状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="ACTIVE"
            inactive-value="INACTIVE"
            :disabled="switchingId === row.id"
            inline-prompt
            active-text="上架"
            inactive-text="下架"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      class="pager"
      background
      layout="total, prev, pager, next, jumper"
      :total="total"
      :page-size="PAGE_SIZE"
      @current-change="load"
    />

    <BundleEditDialog v-model="dialogVisible" :bundle="editingBundle" @saved="handleSaved" />
  </el-card>
</template>

<style scoped>
.page-head {
  margin-bottom: 14px;
}

.page-head h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
}

.page-head p {
  font-size: 12.5px;
  color: #9ca3af;
  margin: 0;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.spacer {
  flex: 1;
}

.prod-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex: none;
  overflow: hidden;
}

.prod-thumb-fb {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.prod-meta {
  min-width: 0;
}

.prod-name {
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prod-desc {
  font-size: 12px;
  color: #9ca3af;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.origin-price {
  color: #c0c4cc;
  text-decoration: line-through;
}

.dim {
  color: #c0c4cc;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
