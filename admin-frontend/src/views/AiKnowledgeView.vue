<template>
  <div class="knowledge-page">
    <section class="hero">
      <div><span>KNOWLEDGE BASE</span><h1>AI知识库管理</h1><p>维护挂号时间、取药流程、报告查询、楼层导诊等常见问题，让患者端就医智能助手优先给出标准答案。</p></div>
      <el-button type="primary" size="large" @click="openCreate"><el-icon><Plus /></el-icon>新增知识</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索问题、关键词" clearable :prefix-icon="Search" @keyup.enter="loadList" />
        <el-select v-model="query.category" placeholder="输入或选择分类" clearable filterable allow-create default-first-option>
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="loadList"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>
      <el-table v-loading="loading" :data="list" class="soft-table">
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="category" label="分类" width="120"><template #default="{ row }"><el-tag>{{ row.category || '通用' }}</el-tag></template></el-table-column>
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'info' : 'success'">{{ row.status === 0 ? '停用' : '启用' }}</el-tag></template></el-table-column>
        <el-table-column label="更新时间" width="180"><template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="loadList" /></div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识条目' : '新增知识条目'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="分类" prop="category"><el-select v-model="form.category" filterable allow-create default-first-option style="width:100%"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="问题" prop="question"><el-input v-model="form.question" placeholder="例如：发烧应该去哪个科室？" /></el-form-item>
        <el-form-item label="答案" prop="answer"><el-input v-model="form.answer" type="textarea" :rows="6" placeholder="请输入 AI 可引用的标准回答" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔，例如：发热,退烧,内科" /></el-form-item>
        <el-row :gutter="14"><el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { addKnowledge, deleteKnowledge, getKnowledgeList, updateKnowledge } from '@/api/ai'
import type { AiKnowledge } from '@/types'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const list = ref<AiKnowledge[]>([])
const total = ref(0)
const categories = ['门诊服务', '楼层导诊', '检查检验', '药房服务', '急诊服务', '费用与支付', '住院服务', '交通停车']
const query = reactive({ keyword: '', category: '', page: 1, size: 10 })
const emptyForm = (): AiKnowledge => ({ category: '门诊服务', question: '', answer: '', keywords: '', sort: 0, status: 1 })
const form = reactive<AiKnowledge>(emptyForm())
const formatDate = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '--'
const rules: FormRules = { question: [{ required: true, message: '请输入问题', trigger: 'blur' }], answer: [{ required: true, message: '请输入答案', trigger: 'blur' }], category: [{ required: true, message: '请选择分类', trigger: 'change' }] }

async function loadList() {
  loading.value = true
  try {
    const res = await getKnowledgeList({ keyword: query.keyword || undefined, category: query.category || undefined, page: query.page, size: query.size })
    list.value = res.data.data.content || []
    total.value = res.data.data.totalElements || 0
  } finally { loading.value = false }
}
function resetQuery() { query.keyword = ''; query.category = ''; query.page = 1; loadList() }
function assignForm(data: AiKnowledge) { Object.assign(form, emptyForm(), data) }
function openCreate() { assignForm(emptyForm()); dialogVisible.value = true }
function openEdit(row: AiKnowledge) { assignForm(row); dialogVisible.value = true }
async function save() {
  if (!await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    if (form.id) await updateKnowledge(form)
    else await addKnowledge(form)
    ElMessage.success('知识条目已保存')
    dialogVisible.value = false
    await loadList()
  } finally { saving.value = false }
}
async function remove(row: AiKnowledge) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除“${row.question}”吗？`, '删除知识条目', { type: 'warning' })
  await deleteKnowledge(row.id)
  ElMessage.success('已删除')
  await loadList()
}
onMounted(loadList)
</script>

<style scoped>
.knowledge-page{display:flex;flex-direction:column;gap:18px}.hero{position:relative;overflow:hidden;display:flex;align-items:center;justify-content:space-between;padding:28px 32px;border-radius:22px;background:linear-gradient(125deg,#083f5d,#0b8c82 68%,#2db39b);color:#fff;box-shadow:0 16px 38px rgba(11,74,89,.15)}.hero:after{content:"";position:absolute;right:-76px;top:-134px;width:295px;height:295px;border:1px solid rgba(255,255,255,.13);border-radius:50%;box-shadow:0 0 0 50px rgba(255,255,255,.035)}.hero>div,.hero .el-button{position:relative;z-index:1}.hero span{font-size:10px;font-weight:800;letter-spacing:2px;opacity:.72}.hero h1{margin:8px 0;font-size:30px}.hero p{max-width:680px;color:rgba(255,255,255,.72);font-size:13px;line-height:1.8}.hero .el-button{border:0;background:#fff;color:#0b817b;font-weight:800}.panel{overflow:hidden;border:1px solid #e1ebed;border-radius:20px;background:#fff;box-shadow:0 9px 28px rgba(26,67,81,.06)}.toolbar{display:grid;grid-template-columns:minmax(260px,1fr) 180px 96px 96px;gap:12px;padding:18px 20px;border-bottom:1px solid #edf2f3}.soft-table{width:100%}.soft-table :deep(th.el-table__cell){background:#f5fafb;color:#526b74}.pager{display:flex;justify-content:flex-end;padding:16px 20px}.el-dialog :deep(.el-dialog__header){padding-bottom:10px}.el-textarea :deep(.el-textarea__inner),.el-input :deep(.el-input__wrapper),.el-select :deep(.el-select__wrapper){border-radius:11px}
</style>
