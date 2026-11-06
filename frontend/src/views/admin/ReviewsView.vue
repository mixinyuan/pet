<script setup lang="ts">
/**
 * 评价管理页（/admin/reviews）：C 端评价由后台统一维护，本页提供全量视角
 * （隐藏评价同样可见）的分页列表（关键字/展示状态筛选）+ 新增/编辑/展示隐藏切换/删除。
 * 交互对齐旧版 admin-console-modules 评价模块：
 * - 编辑直接用列表行回填（列表行与详情 VO 同构），商品下拉拉前 100 个商品（已下架带标记）；
 * - 展示状态 Switch 乐观翻转，失败回滚（报错文案由拦截器弹出）；
 * - 删除为逻辑删除，二次确认文案保留旧版语义。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createAdminReview,
  deleteAdminReview,
  fetchAdminReviews,
  updateAdminReview,
  updateAdminReviewStatus,
} from '@/api/admin/review'
import { fetchAdminProducts } from '@/api/admin/product'
import type { AdminProductVO, AdminReviewVO, AdminStatus, ReviewSavePayload } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

/** 与旧版一致的每页条数（后端 size clamp 到 [1,100]） */
const PAGE_SIZE = 10
/** 弹窗商品下拉一次性拉取条数（后端 size clamp 到 [1,100]） */
const OPTION_SIZE = 100

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminReviewVO[]>([])
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
  filtersApplied.value ? '未找到匹配的评价' : '暂无评价，点击「新增评价」创建',
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
    const data = await fetchAdminReviews(params)
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

/** 删除末页最后一条后回退一页，避免停留在空页 */
function stepBackPage() {
  if (rows.value.length === 1 && page.value > 1) page.value -= 1
}

/* ---------------- 展示/隐藏切换（乐观更新失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

/** Switch 触发：v-model 已翻转；失败回滚（后端文案由拦截器弹出） */
async function handleStatusChange(row: AdminReviewVO) {
  const next = row.status
  switchingId.value = row.id
  try {
    await updateAdminReviewStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '评价已展示' : '评价已隐藏，C 端将不再展示')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除（逻辑删除，二次确认） ---------------- */

async function handleDelete(row: AdminReviewVO) {
  const confirmed = await ElMessageBox.confirm(
    '逻辑删除，删除后 C 端立即不再展示。',
    `删除这条评价？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminReview(row.id)
    ElMessage.success('评价已删除')
    stepBackPage()
    load()
  } catch {
    // 报错由拦截器弹出
  }
}

/* ---------------- 新增/编辑弹窗 ---------------- */

interface ReviewFormState {
  buyerName: string
  petName: string
  petBreed: string
  avatarUrl: string
  content: string
  rating: number
  sortOrder: number
  /** null 表示通用（不关联商品） */
  productId: number | null
  status: AdminStatus
}

function blankForm(): ReviewFormState {
  return {
    buyerName: '',
    petName: '',
    petBreed: '',
    avatarUrl: '',
    content: '',
    rating: 4.8, // 与旧版一致的缺省评分
    sortOrder: 0,
    productId: null,
    status: 'ACTIVE',
  }
}

const dialogVisible = ref(false)
const saving = ref(false)
/** 本次弹窗编辑的评价 id（null 表示新增） */
const editingId = ref<number | null>(null)
const form = reactive<ReviewFormState>(blankForm())
const formRef = ref<FormInstance>()
/** 商品下拉数据源（前 100 个，已下架带标记） */
const products = ref<AdminProductVO[]>([])

/** 评分：0.0-5.0 一位小数（对齐后端 @DecimalMin/@DecimalMax/@Digits） */
const validateRating = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback(new Error('请填写评分'))
  if (!/^\d(\.\d)?$/.test(String(value)) || Number(value) > 5) {
    return callback(new Error('评分须为 0.0 – 5.0 的一位小数'))
  }
  callback()
}

const rules: FormRules = {
  buyerName: [
    { required: true, message: '请填写评价人姓名', trigger: 'blur' },
    { max: 64, message: '评价人姓名长度不能超过 64 字符', trigger: 'blur' },
  ],
  petName: [{ max: 64, message: '宠物名长度不能超过 64 字符', trigger: 'blur' }],
  petBreed: [{ max: 64, message: '宠物品种长度不能超过 64 字符', trigger: 'blur' }],
  avatarUrl: [{ max: 500, message: '头像 URL 长度不能超过 500 字符', trigger: 'blur' }],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    { max: 1000, message: '评价内容长度不能超过 1000 字符', trigger: 'blur' },
  ],
  rating: [{ required: true, validator: validateRating, trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请填写排序号', trigger: 'blur' }],
}

/** 打开弹窗：先拉商品下拉数据源（失败则关闭弹窗，与旧版一致），再用行数据回填 */
async function openDialog(row: AdminReviewVO | null) {
  editingId.value = row ? row.id : null
  Object.assign(form, blankForm())
  dialogVisible.value = true
  try {
    const data = await fetchAdminProducts({ page: 1, size: OPTION_SIZE })
    products.value = data.records
  } catch {
    products.value = []
    dialogVisible.value = false
    return
  }
  if (!row) return
  // 列表行与详情 VO 同构，直接回填（与旧版一致）
  Object.assign(form, {
    buyerName: row.buyerName ?? '',
    petName: row.petName ?? '',
    petBreed: row.petBreed ?? '',
    avatarUrl: row.avatarUrl ?? '',
    content: row.content ?? '',
    rating: row.rating ?? 4.8,
    sortOrder: row.sortOrder ?? 0,
    productId: row.productId ?? null,
    status: row.status,
  })
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload: ReviewSavePayload = {
    buyerName: form.buyerName.trim(),
    petName: form.petName.trim() || null,
    petBreed: form.petBreed.trim() || null,
    content: form.content.trim(),
    avatarUrl: form.avatarUrl.trim() || null,
    rating: Number(form.rating),
    sortOrder: Number(form.sortOrder),
    productId: form.productId,
    status: form.status,
  }
  try {
    if (editingId.value) {
      await updateAdminReview(editingId.value, payload)
      ElMessage.success('评价已保存')
    } else {
      await createAdminReview(payload)
      ElMessage.success('评价已创建')
    }
    dialogVisible.value = false
    load() // 保持当前页刷新（与旧版一致）
  } catch {
    // 保存失败（如关联商品不存在 400）时保持弹窗打开供修改，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <div class="page-head">
      <h2>评价管理</h2>
      <p>隐藏后 C 端立即不展示；删除为逻辑删除，C 端评价由后台统一维护</p>
    </div>

    <!-- 工具栏：新增 / 状态筛选 / 关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog(null)">新增评价</el-button>
      <el-select v-model="filters.status" style="width: 120px" @change="search">
        <el-option label="全部状态" value="all" />
        <el-option label="展示中" value="ACTIVE" />
        <el-option label="已隐藏" value="INACTIVE" />
      </el-select>
      <div class="spacer" />
      <el-input
        v-model="filters.keyword"
        placeholder="内容 / 评价人"
        clearable
        maxlength="64"
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="评价人" min-width="150">
        <template #default="{ row }">
          <div class="cell-main">{{ row.buyerName }}</div>
          <div v-if="row.petName || row.petBreed" class="cell-sub">
            {{ row.petName || '—' }} · {{ row.petBreed || '—' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="260">
        <template #default="{ row }">
          <div class="content-clamp" :title="row.content">{{ row.content }}</div>
        </template>
      </el-table-column>
      <el-table-column label="关联商品" min-width="140">
        <template #default="{ row }">
          <span v-if="row.productName">{{ row.productName }}</span>
          <span v-else class="dim">通用</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="90" align="center">
        <template #default="{ row }">
          <template v-if="row.rating != null"><span class="star">★</span> {{ Number(row.rating).toFixed(1) }}</template>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column label="展示状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="ACTIVE"
            inactive-value="INACTIVE"
            :disabled="switchingId === row.id"
            inline-prompt
            active-text="展示"
            inactive-text="隐藏"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="排序号" width="80" align="center">
        <template #default="{ row }">{{ row.sortOrder ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="时间" width="140">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
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

    <!-- 新增/编辑评价弹窗：全量表单（编辑为整体替换语义） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑评价' : '新增评价'"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评价人" prop="buyerName">
              <el-input v-model="form.buyerName" maxlength="64" placeholder="最长 64 字符" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="宠物名" prop="petName">
              <el-input v-model="form.petName" maxlength="64" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="宠物品种" prop="petBreed">
              <el-input v-model="form.petBreed" maxlength="64" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像 URL" prop="avatarUrl">
              <el-input v-model="form.avatarUrl" maxlength="500" placeholder="外链地址，最长 500 字符" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="关联商品">
              <el-select v-model="form.productId" placeholder="通用（不关联商品）" clearable style="width: 100%">
                <el-option
                  v-for="p in products"
                  :key="p.id"
                  :value="p.id"
                  :label="`${p.name}${p.status === 'INACTIVE' ? '（已下架）' : ''}`"
                />
              </el-select>
              <div class="field-hint">仅展示前 {{ OPTION_SIZE }} 个商品；已下架商品带标记</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="展示状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="展示" value="ACTIVE" />
                <el-option label="隐藏" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分" prop="rating">
              <el-input-number
                v-model="form.rating"
                :controls="false"
                :min="0"
                :max="5"
                :step="0.1"
                :precision="1"
                class="w-full"
              />
              <div class="field-hint">0.0 – 5.0，一位小数</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :controls="false" :min="0" :max="999999" :step="1" :precision="0" class="w-full" />
              <div class="field-hint">越小越靠前，0 – 999999</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评价内容" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ editingId ? '保存修改' : '创建评价' }}
        </el-button>
      </template>
    </el-dialog>
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

.dim {
  color: #c0c4cc;
}

.star {
  color: #f7ba2a;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.cell-main {
  font-weight: 500;
  color: #1f2937;
}

.cell-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

/* 内容两行截断（对齐旧版 -webkit-line-clamp:2） */
.content-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.w-full {
  width: 100%;
}

.field-hint {
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
