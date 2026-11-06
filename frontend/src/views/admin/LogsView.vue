<script setup lang="ts">
/**
 * 日志管理页（/admin/logs，接口 /api/admin/logs）：C 端「AI 自习室」更新日志/通知，
 * 分页列表（标题关键字筛选）+ 新增/编辑/发布状态切换/删除（对齐旧版 admin-console-modules
 * 日志模块与旧 admin.html 的能力；任务简报曾标注「只读」，但后端 AdminLogsController
 * 与旧控制台均提供完整 CRUD，以源码为准迁移全量交互）。
 * 注意契约差异（沿自旧 LogsController）：
 * - 列表参数名是 pageSize，响应为 {total, list}（不是 PageResult{records,total}）；
 * - 发布状态是数字 0/1，PUT /{id}/status body {status:0|1}；
 * - 编辑需先拉详情（列表行不含 content）；新增默认未发布（status=0），成功后回到第 1 页。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createAdminLog,
  deleteAdminLog,
  fetchAdminLog,
  fetchAdminLogs,
  updateAdminLog,
  updateAdminLogStatus,
} from '@/api/admin/log'
import type { UpdateLogVO } from '@/types/admin'

/** 与旧版一致的每页条数（后端 pageSize 上限 100） */
const PAGE_SIZE = 10
/** 日志类型候选（对齐旧版 datalist：可从常用类型中选择或自定义） */
const LOG_TYPES = ['AI自习室通知', '版本更新', '运营公告']

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<UpdateLogVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({ keyword: '' })

const filtersApplied = computed(() => filters.keyword.trim() !== '')
const emptyText = computed(() => (filtersApplied.value ? '未找到匹配的日志' : '暂无日志，点击「新增日志」创建'))

async function load() {
  const params: { page: number; pageSize: number; keyword?: string } = {
    page: page.value,
    pageSize: PAGE_SIZE, // 注意参数名是 pageSize（不是 size）
  }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  loading.value = true
  try {
    const data = await fetchAdminLogs(params)
    rows.value = data.list
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

/** 搜索变化：重置到第 1 页（与旧版一致） */
function search() {
  page.value = 1
  load()
}

/** 序号跨页连续：第 N 页第 i 行 = (N-1)*size + i + 1 */
function rowNumber(index: number) {
  return (page.value - 1) * PAGE_SIZE + index + 1
}

/** 删除末页最后一条后回退一页，避免停留在空页 */
function stepBackPage() {
  if (rows.value.length === 1 && page.value > 1) page.value -= 1
}

/* ---------------- 发布状态切换（乐观更新失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

/** Switch 触发：v-model 已翻转；发布状态是数字 0/1，失败回滚 */
async function handleStatusChange(row: UpdateLogVO) {
  const next = Number(row.publishStatus)
  switchingId.value = row.id
  try {
    await updateAdminLogStatus(row.id, next)
    ElMessage.success(next === 1 ? '已发布' : '已下线')
  } catch {
    row.publishStatus = next === 1 ? 0 : 1
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除（物理删除，二次确认） ---------------- */

async function handleDelete(row: UpdateLogVO) {
  const confirmed = await ElMessageBox.confirm('物理删除，不可恢复。', `删除这条日志？`, {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger',
  }).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminLog(row.id)
    ElMessage.success('删除成功')
    stepBackPage()
    load()
  } catch {
    // 报错由拦截器弹出
  }
}

/* ---------------- 新增/编辑弹窗（编辑先拉详情） ---------------- */

interface LogFormState {
  title: string
  type: string
  content: string
}

function blankForm(): LogFormState {
  return { title: '', type: LOG_TYPES[0], content: '' }
}

const dialogVisible = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
/** 本次弹窗编辑的日志 id（null 表示新增） */
const editingId = ref<number | null>(null)
const form = reactive<LogFormState>(blankForm())
const formRef = ref<FormInstance>()

const rules: FormRules = {
  title: [
    { required: true, message: '请填写标题', trigger: 'blur' },
    { max: 128, message: '标题长度不能超过 128 字符', trigger: 'blur' },
  ],
  type: [{ max: 64, message: '类型长度不能超过 64 字符', trigger: 'blur' }],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }],
}

/** 打开新增弹窗（类型默认第一个候选，与旧版一致） */
function openCreate() {
  editingId.value = null
  Object.assign(form, blankForm())
  dialogVisible.value = true
}

/** 打开编辑弹窗：先拉详情回填 content（列表行不含正文，与旧版一致） */
async function openEdit(row: UpdateLogVO) {
  editingId.value = row.id
  Object.assign(form, blankForm())
  dialogVisible.value = true
  detailLoading.value = true
  try {
    const detail = await fetchAdminLog(row.id)
    Object.assign(form, {
      title: detail.title ?? '',
      type: detail.type || LOG_TYPES[0],
      content: detail.content ?? '',
    })
  } catch {
    dialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      // 编辑仅提交 title/type/content（发布状态走独立 /status 接口）
      await updateAdminLog(editingId.value, {
        title: form.title.trim(),
        type: form.type.trim(),
        content: form.content.trim(),
      })
      ElMessage.success('日志已保存')
      dialogVisible.value = false
      load() // 保持当前页刷新（与旧版一致）
    } else {
      // 新增默认未发布（与旧日志页一致）；新日志按时间倒序排在最前，回到第 1 页
      await createAdminLog({
        title: form.title.trim(),
        type: form.type.trim() || LOG_TYPES[0],
        content: form.content.trim(),
        status: 0,
      })
      ElMessage.success('日志已创建（默认未发布）')
      dialogVisible.value = false
      page.value = 1
      load()
    }
  } catch {
    // 保存失败时保持弹窗打开供修改，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <div class="page-head">
      <h2>更新日志 / 通知</h2>
      <p>C 端「AI 自习室」更新日志；发布状态 Switch 即时上下线</p>
    </div>

    <!-- 工具栏：新增 / 标题关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增日志</el-button>
      <div class="spacer" />
      <el-input
        v-model="filters.keyword"
        placeholder="标题关键字"
        clearable
        maxlength="64"
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="序号" width="70" align="center">
        <template #default="{ $index }">{{ rowNumber($index) }}</template>
      </el-table-column>
      <el-table-column label="标题" min-width="280">
        <template #default="{ row }">
          <span class="ellipsis" :title="row.title">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" min-width="140">
        <template #default="{ row }">{{ row.type || '—' }}</template>
      </el-table-column>
      <el-table-column label="时间" width="140">
        <!-- 服务端已格式化为 yyyy-MM-dd HH:mm 字符串，直接展示 -->
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="发布状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.publishStatus"
            :active-value="1"
            :inactive-value="0"
            :disabled="switchingId === row.id"
            inline-prompt
            active-text="已发布"
            inactive-text="未发布"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="点击量" width="80" align="center">
        <template #default="{ row }">{{ Number(row.viewCount || 0) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
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

    <!-- 新增/编辑日志弹窗：类型可选常用候选或自定义（等价旧版 datalist） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑日志' : '新增日志'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="detailLoading">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="128" placeholder="最长 128 字符" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="t in LOG_TYPES" :key="t" :value="t" :label="t" />
          </el-select>
          <div class="field-hint">可从常用类型中选择或自定义</div>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
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

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.ellipsis {
  display: block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-hint {
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
