<template>
  <div class="ai-page" v-loading="loading && !aiStore.streaming">
    <section class="ai-hero">
      <div>
        <span>AI OPERATION CENTER</span>
        <h1>AI运营中心</h1>
        <p>集中查看 AI 使用情况、运营报告、医疗质检与患者问答日志，让 AI 功能不仅能用，也能被管理、被评估。</p>
      </div>
      <el-button type="primary" size="large" :loading="aiStore.streaming" @click="generateReport"><el-icon><MagicStick /></el-icon>{{ aiStore.streaming ? 'AI正在生成...' : '生成运营报告' }}</el-button>
    </section>


    <section v-if="aiStore.streaming || aiStore.streamText" class="panel stream-panel">
      <div class="panel-head compact">
        <div><span>SSE STREAMING</span><h2>AI流式生成预览</h2></div>
        <el-tag :type="aiStore.streaming ? 'warning' : 'success'">{{ aiStore.streaming ? '生成中' : '生成完成' }}</el-tag>
      </div>
      <pre class="stream-output">{{ streamDisplayText || '正在连接 AI 流式服务...' }}</pre>
    </section>
    <section class="ai-stats">
      <article v-for="item in overviewCards" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.note }}</small>
      </article>
    </section>

    <section class="panel report-panel">
      <div class="panel-head">
        <div><span>REPORTS</span><h2>AI运营报告</h2></div>
        <el-select v-model="reportType" style="width: 150px" @change="loadReports">
          <el-option label="全部报告" value="" />
          <el-option label="日报" value="daily" />
          <el-option label="周报" value="weekly" />
          <el-option label="月报" value="monthly" />
        </el-select>
      </div>
      <el-table :data="reports" class="soft-table">
        <el-table-column prop="reportType" label="类型" width="100"><template #default="{ row }"><el-tag>{{ reportName(row.reportType) }}</el-tag></template></el-table-column>
        <el-table-column prop="summary" label="AI运营摘要" min-width="390"><template #default="{ row }"><p class="report-preview">{{ row.summary || '暂无摘要' }}</p></template></el-table-column>
        <el-table-column prop="suggestions" label="优化建议" min-width="390"><template #default="{ row }"><p class="report-preview">{{ suggestionPreview(row.suggestions) }}</p></template></el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="210" />
        <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openReport(row)">查看完整报告</el-button></template></el-table-column>
      </el-table>
    </section>

    <section class="panel quality-card">
      <div class="panel-head">
        <div><span>QUALITY CHECK</span><h2>医疗质量质检</h2></div>
        <div class="quality-form">
          <el-select v-model="qualityForm.checkType" style="width: 150px"><el-option label="病历质检" value="medical_record" /><el-option label="处方质检" value="prescription" /></el-select>
          <el-input-number v-model="qualityForm.sampleSize" :min="1" :max="100" />
          <el-button type="primary" @click="startCheck"><el-icon><CircleCheck /></el-icon>发起质检</el-button>
        </div>
      </div>
      <div class="quality-list horizontal">
        <div v-for="item in checks" :key="item.id" class="quality-item">
          <div><b>{{ checkName(item.checkType) }}</b><span>{{ formatDate(item.createTime || item.checkDate) }}</span></div>
          <strong>{{ item.avgScore ?? '--' }}分</strong>
          <small>抽检 {{ item.totalCount || 0 }} 份，通过 {{ item.passCount || 0 }} 份</small>
        </div>
        <el-empty v-if="!checks.length" description="暂无质检记录" :image-size="80" />
      </div>
    </section>

    <section class="panel log-panel">
      <div class="panel-head">
        <div><span>CHAT LOGS</span><h2>AI问答日志</h2></div>
        <el-button @click="loadAll"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
      <el-table :data="logs" class="soft-table">
        <el-table-column prop="userType" label="用户类型" width="100" />
        <el-table-column prop="question" label="用户问题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="answer" label="AI回复" min-width="330" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="110">
          <template #default="{ row }"><el-tag :type="row.source === 'knowledge' ? 'success' : 'warning'">{{ row.source === 'knowledge' ? '知识库' : 'AI生成' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="feedback" label="反馈" width="110"><template #default="{ row }"><el-tag :type="row.feedback === 'like' ? 'success' : row.feedback === 'dislike' ? 'danger' : 'info'">{{ row.feedback || '未反馈' }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="优化" width="130" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openKnowledgeDialog(row)">加入知识库</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="knowledgeDialogVisible" title="沉淀为知识库标准回答" width="720px" destroy-on-close>
      <el-alert title="把高质量问答保存到知识库后，患者下次问到相似问题，会优先使用这条标准答案。" type="success" :closable="false" show-icon class="knowledge-tip" />
      <el-form :model="knowledgeForm" label-width="88px">
        <el-form-item label="分类">
          <el-select v-model="knowledgeForm.category" style="width:100%">
            <el-option label="医院导诊" value="hospital_info" />
            <el-option label="平台问答" value="平台问答" />
            <el-option label="检查检验" value="检查检验" />
            <el-option label="用药指导" value="用药指导" />
            <el-option label="健康科普" value="健康科普" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准问题"><el-input v-model="knowledgeForm.question" /></el-form-item>
        <el-form-item label="标准答案"><el-input v-model="knowledgeForm.answer" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="knowledgeForm.keywords" placeholder="多个关键词用英文逗号分隔，例如：取药,药房,处方" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="knowledgeDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="savingKnowledge" @click="saveKnowledgeFromLog">保存到知识库</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="reportDrawerVisible" size="720px" :with-header="false">
      <div v-if="currentReport" class="report-detail">
        <div class="report-detail-head">
          <div><span>AI OPERATION REPORT</span><h2>{{ reportName(currentReport.reportType) }} · AI运营分析报告</h2><p>{{ currentReport.startDate || '--' }} 至 {{ currentReport.endDate || '--' }}</p></div>
          <button @click="reportDrawerVisible=false">×</button>
        </div>
        <section><h3>报告摘要</h3><p>{{ currentReport.summary || '暂无摘要' }}</p></section>
        <section><h3>AI核心指标</h3><div class="metric-grid"><div v-for="item in metricItems(currentReport.keyMetrics)" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div></div></section>
        <section><h3>使用趋势分析</h3><p>{{ currentReport.trendsAnalysis || '暂无趋势分析' }}</p></section>
        <section><h3>风险与异常提醒</h3><ul><li v-for="(item,index) in textList(currentReport.warnings)" :key="index">{{ item }}</li><li v-if="!textList(currentReport.warnings).length">本周期未发现明显异常。</li></ul></section>
        <section class="suggestion-section"><h3>完整优化建议</h3><ol><li v-for="(item,index) in textList(currentReport.suggestions)" :key="index">{{ item }}</li><li v-if="!textList(currentReport.suggestions).length">暂无优化建议。</li></ol></section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { addKnowledge, startQualityCheck } from '@/api/ai'
import { useAiAdminStore } from '@/stores/ai'
import { useMedicalStore } from '@/stores/medical'
import type { AiChatLog, AiKnowledge, AiOperationReport } from '@/types'

const aiStore = useAiAdminStore()
const medicalStore = useMedicalStore()
const { loading, overview, reports, checks, logs } = storeToRefs(aiStore)
const reportType = ref('')
const qualityForm = reactive({ checkType: 'medical_record', sampleSize: 10 })
const knowledgeDialogVisible = ref(false)
const savingKnowledge = ref(false)
const reportDrawerVisible = ref(false)
const currentReport = ref<AiOperationReport>()
const knowledgeForm = reactive<AiKnowledge>({ category: 'hospital_info', question: '', answer: '', keywords: '', sort: 0, status: 1 })


const streamDisplayText = computed(() => prettifyStreamText(aiStore.streamText))

function prettifyStreamText(text: string) {
  if (!text) return ''
  return text
    .replace(/\{\"aiTriageCount\"[\s\S]*?\}/g, value => formatMetricJson(value))
    .replace(/\[\{\"level\"[\s\S]*?\}\]/g, value => formatWarningJson(value))
}

function formatMetricJson(value: string) {
  try {
    const data = JSON.parse(value) as Record<string, string | number>
    const labels: Record<string, string> = {
      aiTriageCount: 'AI分诊调用次数',
      triageAccuracy: '分诊推荐采纳率',
      aiChatCount: 'AI问答次数',
      knowledgeHitRate: '知识库命中率',
      reportInterpretationCount: '检验报告解读次数',
      medicationGuideCount: '用药指导生成次数',
      criticalWarningCount: '危急值预警次数',
      followUpCount: '智能随访任务数',
      qualityCheckCount: 'AI质检批次数'
    }
    return Object.entries(labels)
      .filter(([key]) => data[key] !== undefined)
      .map(([key, label]) => `- ${label}：${data[key]}`)
      .join('\n')
  } catch {
    return value
  }
}

function formatWarningJson(value: string) {
  try {
    const list = JSON.parse(value) as Array<{ content?: string; level?: string }>
    return list.map(item => `- ${item.content || '暂无风险内容'}`).join('\n')
  } catch {
    return value
  }
}
const pick = (keys: string[], fallback = 0) => {
  const metrics = (overview.value.keyMetrics || {}) as Record<string, unknown>
  return keys.map(k => overview.value[k] ?? metrics[k]).find(v => v !== undefined && v !== null) ?? fallback
}
const overviewCards = computed(() => [
  { label: 'AI问答次数', value: pick(['chatCount', 'totalChatCount', 'consultCount', 'aiChatCount']), note: '患者端与医生端累计咨询' },
  { label: '分诊调用', value: pick(['triageCount', 'triageTotal', 'aiTriageCount']), note: '智能分诊推荐次数' },
  { label: '质检记录', value: checks.value.length, note: '最近生成的 AI 质检批次' },
  { label: '知识库命中率', value: pick(['knowledgeHitRate']), note: '标准知识回答命中比例' }
])

const reportName = (type?: string) => ({ daily: '日报', weekly: '周报', monthly: '月报' }[type || ''] || type || '未知')
const checkName = (type?: string) => type === 'prescription' ? '处方质检' : '病历质检'
const formatDate = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '--'
const textList = (value?: string) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map(item => typeof item === 'string' ? item : item.content || JSON.stringify(item))
  } catch { /* plain text */ }
  return value.split(/\n|；|;/).map(item => item.replace(/^\s*\d+[.、]\s*/, '').trim()).filter(Boolean)
}
const suggestionPreview = (value?: string) => textList(value).slice(0, 2).join('；') || '暂无建议'
const metricLabels: Record<string, string> = {
  aiTriageCount: 'AI分诊次数', triageAccuracy: '分诊采纳率', aiChatCount: 'AI问答次数',
  knowledgeHitRate: '知识库命中率', reportInterpretationCount: '报告解读次数',
  medicationGuideCount: '用药指导次数', criticalWarningCount: '危急值预警',
  followUpCount: '智能随访任务', qualityCheckCount: 'AI质检批次'
}
const metricItems = (value?: string) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Object.entries(parsed).map(([key, item]) => ({ label: metricLabels[key] || key, value: String(item) }))
  } catch { return [] }
}
const openReport = (row: AiOperationReport) => { currentReport.value = row; reportDrawerVisible.value = true }

async function loadAll() {
  await aiStore.loadDashboard(reportType.value)
}
async function loadReports() {
  await aiStore.loadReports(reportType.value)
}
async function generateReport() {
  try {
    await aiStore.generateReportStream('daily')
    ElMessage.success('AI运营报告已流式生成')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '流式生成失败')
  }
}
async function startCheck() {
  loading.value = true
  try {
    await startQualityCheck(qualityForm)
    ElMessage.success('AI质检已完成')
    await loadAll()
  } finally { loading.value = false }
}
function openKnowledgeDialog(row: AiChatLog) {
  const question = row.question || ''
  Object.assign(knowledgeForm, {
    category: inferCategory(question),
    question,
    answer: row.answer || '',
    keywords: buildKeywords(question),
    sort: 0,
    status: 1
  })
  knowledgeDialogVisible.value = true
}
function inferCategory(question: string) {
  if (/取药|药房|处方|用药/.test(question)) return '用药指导'
  if (/报告|检验|检查|CT|彩超|抽血/.test(question)) return '检查检验'
  if (/挂号|预约|退号|缴费|楼层|在哪|地址|电话|停车|急诊/.test(question)) return 'hospital_info'
  return '平台问答'
}
function buildKeywords(question: string) {
  return Array.from(new Set(question.replace(/[？?，,。.\s]/g, ',').split(',').filter(Boolean))).slice(0, 8).join(',')
}
async function saveKnowledgeFromLog() {
  if (!knowledgeForm.question || !knowledgeForm.answer) return ElMessage.warning('问题和答案不能为空')
  savingKnowledge.value = true
  try {
    await addKnowledge(knowledgeForm)
    ElMessage.success('已加入知识库，后续相似问题将优先命中这条答案')
    knowledgeDialogVisible.value = false
    await loadAll()
  } finally { savingKnowledge.value = false }
}
onMounted(() => {
  loadAll()
  medicalStore.loadSnapshot().catch(() => {})
})
</script>

<style scoped>
.report-preview{display:-webkit-box;overflow:hidden;margin:0;line-height:1.65;-webkit-line-clamp:2;-webkit-box-orient:vertical}.report-detail{padding:8px 12px 28px}.report-detail-head{display:flex;justify-content:space-between;padding:8px 0 20px;border-bottom:1px solid #e5eeee}.report-detail-head span{color:#0b9185;font-size:10px;font-weight:800;letter-spacing:2px}.report-detail-head h2{margin:7px 0;font-size:24px}.report-detail-head p{color:#8da0a6}.report-detail-head button{width:38px;height:38px;border:0;border-radius:11px;background:#f0f5f5;color:#5f777d;font-size:22px;cursor:pointer}.report-detail section{margin-top:16px;padding:18px;border:1px solid #e2ecee;border-radius:15px;background:#f9fcfc}.report-detail section h3{margin:0 0 11px;font-size:16px}.report-detail section p,.report-detail li{color:#49666f;font-size:14px;line-height:1.9}.report-detail ul,.report-detail ol{margin:0;padding-left:22px}.metric-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.metric-grid div{padding:13px;border-radius:12px;background:#edf8f6}.metric-grid span,.metric-grid strong{display:block}.metric-grid span{color:#789096;font-size:11px}.metric-grid strong{margin-top:6px;color:#087f7c;font-size:18px}.report-detail .suggestion-section{border-color:#cfe9e5;background:#f1faf8}
.ai-page{display:flex;flex-direction:column;gap:18px}.ai-hero{position:relative;overflow:hidden;display:flex;align-items:center;justify-content:space-between;padding:28px 32px;border-radius:22px;background:linear-gradient(125deg,#063a5a,#087f82 66%,#18a58f);color:#fff;box-shadow:0 16px 38px rgba(11,74,89,.15)}.ai-hero:after{content:"";position:absolute;right:-78px;top:-130px;width:290px;height:290px;border:1px solid rgba(255,255,255,.13);border-radius:50%;box-shadow:0 0 0 48px rgba(255,255,255,.035)}.ai-hero>div,.ai-hero .el-button{position:relative;z-index:1}.ai-hero span,.panel-head span{font-size:10px;font-weight:800;letter-spacing:2px;opacity:.72}.ai-hero h1{margin:8px 0;font-size:30px}.ai-hero p{max-width:680px;color:rgba(255,255,255,.72);font-size:13px;line-height:1.8}.ai-hero .el-button{border:0;background:#fff;color:#0b817b;font-weight:800}.ai-stats{display:grid;grid-template-columns:repeat(4,1fr);gap:15px}.ai-stats article,.panel{border:1px solid #e1ebed;border-radius:20px;background:#fff;box-shadow:0 9px 28px rgba(26,67,81,.06)}.ai-stats article{padding:20px}.ai-stats span,.ai-stats strong,.ai-stats small{display:block}.ai-stats span{color:#82949b;font-size:11px}.ai-stats strong{margin:7px 0;color:#123e4f;font-size:28px}.ai-stats small{color:#a3b0b5;font-size:10px}.panel{overflow:hidden}.panel-head{display:flex;align-items:center;justify-content:space-between;padding:20px 22px;border-bottom:1px solid #edf2f3}.panel-head.compact{padding-bottom:14px}.panel-head span{color:#108b80}.panel-head h2{margin-top:5px;font-size:18px}.soft-table{width:100%}.soft-table :deep(th.el-table__cell){background:#f5fafb;color:#526b74}.quality-form{display:flex;align-items:center;gap:10px}.quality-list{padding:20px}.quality-list.horizontal{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.quality-item{position:relative;margin-bottom:0;padding:15px;border-radius:15px;background:#f5fafb;border:1px solid #e4eff0}.quality-item div{display:block}.quality-item b{display:block;color:#214554;white-space:nowrap}.quality-item span{display:block;margin-top:5px}.quality-item span,.quality-item small{color:#8ca0a7;font-size:11px}.quality-item strong{display:block;margin:8px 0 5px;color:#0b9488;font-size:24px}.report-panel,.quality-card,.log-panel{margin-top:2px}.knowledge-tip{margin-bottom:16px}.el-dialog :deep(.el-input__wrapper),.el-dialog :deep(.el-select__wrapper),.el-dialog :deep(.el-textarea__inner){border-radius:11px}
.stream-panel{
  position:relative;
  overflow:hidden;
  border:1px solid #bfe9e4;
  background:#ffffff;
  box-shadow:0 14px 34px rgba(8,92,104,.09);
}
.stream-panel:before{
  content:"";
  position:absolute;
  inset:0 0 auto 0;
  height:4px;
  background:linear-gradient(90deg,#10958f,#5ad6c6,#2f8de4);
}
.stream-panel .panel-head{
  background:#fff;
}
.stream-output{
  min-height:220px;
  max-height:420px;
  overflow:auto;
  margin:0 22px 24px;
  padding:20px 22px;
  border-radius:16px;
  border:1px solid rgba(4,107,112,.18);
  background:linear-gradient(180deg,#073449,#092f3c);
  color:#e9fffb;
  font-size:15px;
  font-family:"Microsoft YaHei", "PingFang SC", Consolas, monospace;
  line-height:1.9;
  white-space:pre-wrap;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.05),0 10px 22px rgba(5,45,58,.12);
}
.stream-output::selection{background:#25b8aa;color:#fff}
</style>





