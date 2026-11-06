<script setup lang="ts">
/**
 * 商品列表子标签：分页/筛选（关键字·分类·上架状态）+ 上下架开关 + 新增/编辑弹窗 + 逻辑删除。
 * 交互对齐旧版 admin-console 商品列表：筛选条件与查询参数一致（page/size/keyword/category/status，
 * all 不传）；编辑先拉详情再回填表单；分类/标签以 chips（checkbox 按钮组）选择，引用数据来自父组件。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createAdminProduct,
  deleteAdminProduct,
  fetchAdminProduct,
  fetchAdminProducts,
  updateAdminProduct,
  updateAdminProductStatus,
} from '@/api/admin/product'
import type { AdminCategoryVO, AdminProductVO, AdminProductQuery, AdminStatus, AdminTagVO, ProductSavePayload } from '@/types/admin'

/** 缩略图加载失败时的占位色块调色板（与旧版 THUMB_COLORS 一致） */
const THUMB_COLORS = [
  '#7f9cf5', '#68d391', '#f6ad55', '#f687b3', '#63b3ed', '#f6e05e',
  '#b794f4', '#4fd1c5', '#fc8181', '#9ae6b4', '#fbd38d', '#90cdf4',
  '#ecc94b', '#fed7d7',
]

/** 与旧版一致的每页条数（后端 size clamp 到 [1,100]） */
const PAGE_SIZE = 10

/**
 * 图片 URL 输入提示。放 script 常量再绑定，而不写成模板静态属性：
 * 静态属性串里的「//」会让 vue-tsc 生成代码时误吞行尾，产生 v-model 的伪类型报错。
 */
const IMAGE_URL_PLACEHOLDER = '必填，需以 http:// 或 https:// 开头'

const props = defineProps<{
  /** 全部分类（含停用），供筛选下拉与名称映射 */
  categories: AdminCategoryVO[]
  /** 全部标签（含停用），供名称映射与表单选择 */
  tags: AdminTagVO[]
}>()

const emit = defineEmits<{ (e: 'refChanged'): void }>()

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminProductVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({
  keyword: '',
  /** 分类 code，all 表示不过滤（不传给后端） */
  category: 'all',
  /** ACTIVE/INACTIVE/all */
  status: 'all' as 'all' | AdminStatus,
})

/** 分类筛选下拉（对齐旧版：剔除系统内置 all） */
const categoryOptions = computed(() => props.categories.filter((c) => c.code !== 'all'))

const filtersApplied = computed(
  () => filters.keyword.trim() !== '' || filters.category !== 'all' || filters.status !== 'all',
)
const emptyText = computed(() =>
  filtersApplied.value ? '未找到匹配的商品' : '暂无商品，点击左上角「新增商品」创建',
)

async function load() {
  const params: AdminProductQuery = { page: page.value, size: PAGE_SIZE }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.category !== 'all') params.category = filters.category
  if (filters.status !== 'all') params.status = filters.status
  loading.value = true
  try {
    const data = await fetchAdminProducts(params)
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

const route = useRoute()
const router = useRouter()

onMounted(async () => {
  await load()
  // 看板「去补库存」跳转落地：直接拉起该商品的编辑弹窗（处理后清除 query，避免刷新重复打开）
  const editId = Number(route.query.edit)
  if (route.query.edit !== undefined && Number.isFinite(editId) && editId > 0) {
    openEdit(editId)
    router.replace({ query: {} })
  }
})

/** 搜索/筛选变化：重置到第 1 页（与旧版一致） */
function search() {
  page.value = 1
  load()
}

/** 一键清空全部筛选条件并重新查询 */
function resetFilters() {
  filters.keyword = ''
  filters.category = 'all'
  filters.status = 'all'
  search()
}

/* ---------------- 名称映射与展示 ---------------- */

/** 分类 code → 名称（未命中回退 code 本身） */
function catName(code: string) {
  return props.categories.find((c) => c.code === code)?.name ?? code
}

/** 标签 id → 名称（未命中返回空串，渲染时过滤） */
function tagName(id: number) {
  return props.tags.find((t) => t.id === id)?.name ?? ''
}

/** 价格展示：¥xx.xx（与旧版 money() 一致） */
function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function thumbColor(id: number) {
  return THUMB_COLORS[Math.abs(id) % THUMB_COLORS.length]
}

/* ---------------- 上下架开关（乐观更新，失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

async function handleStatusChange(row: AdminProductVO) {
  const next = row.status // v-model 已写入目标状态
  switchingId.value = row.id
  try {
    await updateAdminProductStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '商品已上架，C 端将恢复展示' : '商品已下架，C 端将不再展示')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 失败回滚
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除（二次确认；被套餐引用时后端拒绝） ---------------- */

async function handleDelete(row: AdminProductVO) {
  const confirmed = await ElMessageBox.confirm(
    '逻辑删除，历史订单快照不受影响；商品被套餐引用时后端将拒绝删除。',
    `删除商品「${row.name}」？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminProduct(row.id)
    ElMessage.success('商品已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1 // 当前页删空则回退一页
    emit('refChanged') // 分类/标签引用数（productCount）可能变化
    load()
  } catch {
    // 后端拒绝（被套餐引用）时拦截器已弹出报错文案
  }
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const PET_TYPES = ['猫咪适用', '狗狗适用', '猫狗通用', '小型犬适用']

interface ProductForm {
  sku: string
  name: string
  petType: string
  flavor: string
  price: number | null
  stock: number | null
  rating: number | null
  imageUrl: string
  description: string
  status: AdminStatus
  categories: string[]
  tags: number[]
}

function blankForm(): ProductForm {
  return {
    sku: '',
    name: '',
    petType: PET_TYPES[0],
    flavor: '',
    price: null,
    stock: null,
    rating: 5.0,
    imageUrl: '',
    description: '',
    status: 'ACTIVE',
    categories: [],
    tags: [],
  }
}

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ProductForm>(blankForm())

/** 价格：> 0，最多 8 位整数、2 位小数（对齐后端 @DecimalMin/@Digits） */
const validatePrice = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback(new Error('请填写价格'))
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(value)) || Number(value) <= 0) {
    return callback(new Error('价格必须大于 0，最多 8 位整数、2 位小数'))
  }
  callback()
}

/** 库存：≥ 0 的整数 */
const validateStock = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback(new Error('请填写库存'))
  if (!/^\d+$/.test(String(value)) || Number(value) < 0) {
    return callback(new Error('库存须为 ≥ 0 的整数'))
  }
  callback()
}

/** 评分：可空（缺省 5.0），填写时 0.0–5.0 一位小数 */
const validateRating = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback() // 留空按缺省 5.0 提交
  if (!/^\d(\.\d)?$/.test(String(value)) || Number(value) > 5) {
    return callback(new Error('评分须为 0.0 – 5.0 的一位小数'))
  }
  callback()
}

/** 图片 URL：http/https 开头 */
const validateImageUrl = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) return callback(new Error('请填写图片 URL'))
  if (!/^https?:\/\/\S+$/.test(value.trim())) {
    return callback(new Error('图片 URL 需以 http:// 或 https:// 开头'))
  }
  callback()
}

/** 分类：至少 1 个（不含内置 all，选项已过滤） */
const validateCategories = (_rule: unknown, value: string[], callback: (error?: Error) => void) => {
  if (!value || value.length < 1) return callback(new Error('请至少选择 1 个分类'))
  callback()
}

const rules: FormRules = {
  sku: [
    { required: true, message: '请填写 SKU', trigger: 'blur' },
    { max: 64, message: 'SKU 长度不能超过 64 字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请填写商品名称', trigger: 'blur' },
    { max: 100, message: '商品名称长度不能超过 100 字符', trigger: 'blur' },
  ],
  petType: [{ required: true, message: '请选择适用宠物', trigger: 'change' }],
  flavor: [
    { required: true, message: '请填写口味', trigger: 'blur' },
    { max: 50, message: '口味长度不能超过 50 字符', trigger: 'blur' },
  ],
  price: [{ required: true, validator: validatePrice, trigger: 'blur' }],
  stock: [{ required: true, validator: validateStock, trigger: 'blur' }],
  rating: [{ validator: validateRating, trigger: 'blur' }],
  imageUrl: [
    { required: true, validator: validateImageUrl, trigger: 'blur' },
    { max: 500, message: '图片 URL 长度不能超过 500 字符', trigger: 'blur' },
  ],
  categories: [{ required: true, validator: validateCategories, trigger: 'change' }],
  description: [{ max: 500, message: '描述长度不能超过 500 字符', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, blankForm())
  dialogVisible.value = true
}

/** 编辑：先拉详情再回填（与旧版一致；失败时不打开弹窗，报错由拦截器弹出） */
async function openEdit(id: number) {
  try {
    const detail = await fetchAdminProduct(id)
    editingId.value = detail.id
    Object.assign(form, blankForm(), {
      sku: detail.sku,
      name: detail.name,
      petType: detail.petType,
      flavor: detail.flavor,
      price: detail.price,
      stock: detail.stock,
      rating: detail.rating ?? 5.0,
      imageUrl: detail.imageUrl,
      description: detail.description ?? '',
      status: detail.status,
      categories: [...(detail.categories || [])],
      tags: [...(detail.tags || [])],
    })
    dialogVisible.value = true
  } catch {
    // 拦截器已提示「获取商品详情失败」类信息
  }
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload: ProductSavePayload = {
    sku: form.sku.trim().toUpperCase(), // SKU 大写转换由前端负责
    name: form.name.trim(),
    petType: form.petType,
    flavor: form.flavor.trim(),
    price: Number(form.price),
    stock: Number(form.stock),
    rating: form.rating == null ? 5.0 : Number(form.rating),
    imageUrl: form.imageUrl.trim(),
    categories: [...form.categories],
    tags: [...form.tags],
    description: form.description.trim(),
    status: form.status,
  }
  try {
    if (editingId.value) {
      await updateAdminProduct(editingId.value, payload)
      ElMessage.success(`商品「${payload.name}」已保存`)
    } else {
      await createAdminProduct(payload)
      ElMessage.success(`商品「${payload.name}」已创建`)
      page.value = 1 // 列表按 id 倒序，新商品在第 1 页
    }
    dialogVisible.value = false
    emit('refChanged') // 引用数（productCount）可能变化
    load()
  } catch {
    // 保存失败（如 SKU 重复 409）时保持弹窗打开供修改，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <!-- 工具栏：新增 / 分类筛选 / 状态筛选 / 关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      <el-select v-model="filters.category" style="width: 150px" @change="search">
        <el-option label="全部分类" value="all" />
        <el-option v-for="c in categoryOptions" :key="c.code" :label="c.name" :value="c.code" />
      </el-select>
      <el-select v-model="filters.status" style="width: 130px" @change="search">
        <el-option label="全部状态" value="all" />
        <el-option label="上架中" value="ACTIVE" />
        <el-option label="已下架" value="INACTIVE" />
      </el-select>
      <div class="spacer" />
      <el-input
        v-model="filters.keyword"
        placeholder="名称 / SKU / 口味"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="商品" min-width="240">
        <template #default="{ row }">
          <div class="prod-cell">
            <el-image :src="row.imageUrl" fit="cover" loading="lazy" class="prod-thumb">
              <template #error>
                <div class="prod-thumb-fb" :style="{ background: thumbColor(row.id) }">
                  {{ String(row.name || '·').slice(0, 1) }}
                </div>
              </template>
            </el-image>
            <div class="prod-meta">
              <div class="prod-name" :title="row.name">{{ row.name }}</div>
              <div class="prod-sku">{{ row.sku }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" min-width="150">
        <template #default="{ row }">
          <template v-if="row.categories && row.categories.length">
            <el-tag v-for="code in row.categories.slice(0, 2)" :key="code" size="small" class="pill">{{ catName(code) }}</el-tag>
            <el-tag v-if="row.categories.length > 2" size="small" type="info" class="pill">
              +{{ row.categories.length - 2 }}
            </el-tag>
          </template>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="150">
        <template #default="{ row }">
          <template v-if="row.tags && row.tags.length">
            <el-tag v-for="id in row.tags.slice(0, 2)" :key="id" size="small" type="success" class="pill">
              {{ tagName(id) }}
            </el-tag>
            <el-tag v-if="row.tags.length > 2" size="small" type="info" class="pill">+{{ row.tags.length - 2 }}</el-tag>
          </template>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="petType" label="适用宠物" min-width="100" />
      <el-table-column label="价格" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.price) }}</template>
      </el-table-column>
      <el-table-column label="库存" width="80" align="right">
        <template #default="{ row }">
          <span :class="{ 'low-stock': row.stock < 20 }">{{ row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="80">
        <template #default="{ row }">
          <span v-if="row.rating == null" class="dim">—</span>
          <span v-else class="stars">★ {{ row.rating.toFixed(1) }}</span>
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
          <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
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

    <!-- 新增/编辑弹窗：字段与校验对齐 ProductSaveRequest -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑商品' : '新增商品'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SKU 编码" prop="sku">
              <el-input
                v-model="form.sku"
                maxlength="64"
                placeholder="唯一，建议 PB-XXX-XXX，自动转大写"
                @blur="form.sku = form.sku.toUpperCase()"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用宠物" prop="petType">
              <el-select v-model="form.petType" style="width: 100%">
                <el-option v-for="p in PET_TYPES" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="口味" prop="flavor">
              <el-input v-model="form.flavor" maxlength="50" placeholder="如：鸡胸肉" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格（元）" prop="price">
              <el-input-number
                v-model="form.price"
                :controls="false"
                :min="0"
                :max="99999999.99"
                :precision="2"
                placeholder="> 0，最多 8 位整数、2 位小数"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :controls="false"
                :min="0"
                :precision="0"
                placeholder="≥ 0 的整数"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分（缺省 5.0）" prop="rating">
              <el-input-number
                v-model="form.rating"
                :controls="false"
                :min="0"
                :max="5"
                :step="0.1"
                :precision="1"
                placeholder="0.0 – 5.0，一位小数"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上架状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="上架" value="ACTIVE" />
                <el-option label="下架" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片 URL" prop="imageUrl">
              <el-input v-model="form.imageUrl" maxlength="500" :placeholder="IMAGE_URL_PLACEHOLDER" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属分类（多选，至少 1 项）" prop="categories">
              <el-checkbox-group v-if="categoryOptions.length" v-model="form.categories">
                <el-checkbox-button v-for="c in categoryOptions" :key="c.code" :value="c.code">
                  {{ c.name }}
                </el-checkbox-button>
              </el-checkbox-group>
              <span v-else class="dim">暂无分类，请先到「分类管理」创建</span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签（多选，可选）">
              <el-checkbox-group v-if="tags.length" v-model="form.tags">
                <el-checkbox-button v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</el-checkbox-button>
              </el-checkbox-group>
              <span v-else class="dim">暂无标签，可到「标签管理」创建</span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ editingId ? '保存修改' : '创建商品' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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

.prod-sku {
  font-size: 12px;
  color: #9ca3af;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.pill {
  margin-right: 4px;
}

.dim {
  color: #c0c4cc;
}

.low-stock {
  color: #dc2626;
  font-weight: 600;
}

.stars {
  color: #f59e0b;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.w-full {
  width: 100%;
}
</style>
