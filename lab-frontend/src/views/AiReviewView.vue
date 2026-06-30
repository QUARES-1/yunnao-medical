<template>
  <div class="ai-review-page">
    <section class="page-hero compact ai-review-hero">
      <div>
        <span class="eyebrow">AI LAB REVIEW</span>
        <h1>检验结果 AI 智能审核</h1>
        <p>自动判断参考范围、逻辑合理性和历史波动，辅助完成正常报告初审。</p>
      </div>
      <div class="hero-metric">
        <span>自动通过率</span>
        <strong>{{ stats.passRate || 0 }}</strong><small>%</small>
      </div>
    </section>

    <section class="review-stats">
      <article><span>总审核</span><strong>{{ stats.total || 0 }}</strong><small>份报告</small></article>
      <article class="pass"><span>自动通过</span><strong>{{ stats.passCount || 0 }}</strong><small>指标正常</small></article>
      <article class="manual"><span>人工复核</span><strong>{{ stats.manualCount || 0 }}</strong><small>存在疑点</small></article>
      <article class="reject"><span>退回重测</span><strong>{{ stats.rejectCount || 0 }}</strong><small>结果异常</small></article>
    </section>


    <section v-if="labStore.streaming || labStore.streamText" class="content-card stream-panel">
      <div class="card-head">
        <div>
          <span class="eyebrow">SSE STREAMING</span>
          <h2>AI流式审核预览</h2>
        </div>
        <el-tag :type="labStore.streaming ? 'warning' : 'success'" round>{{ labStore.streaming ? '审核生成中' : '生成完成' }}</el-tag>
      </div>
      <pre class="stream-output">{{ labStore.streamText || '正在连接 AI 审核服务...' }}</pre>
    </section>
    <div class="review-grid">
      <section class="content-card">
        <div class="card-head">
          <div>
            <span class="eyebrow">REVIEW LIST</span>
            <h2>AI审核记录</h2>
          </div>
          <div class="review-actions">
            <el-select v-model="filter" placeholder="审核结论" clearable @change="reload">
              <el-option label="全部" value="" />
              <el-option label="自动通过" value="pass" />
              <el-option label="人工复核" value="manual" />
              <el-option label="退回重测" value="reject" />
            </el-select>
            <el-button :icon="Refresh" @click="reload">刷新</el-button>
          </div>
        </div>

        <el-table :data="reviews" v-loading="loading" class="pharmacy-table">
          <el-table-column label="报告编号" min-width="120">
            <template #default="{row}">EX{{ String(row.examinationId).padStart(6, '0') }}</template>
          </el-table-column>
          <el-table-column label="审核结论" min-width="115">
            <template #default="{row}">
              <el-tag :type="tagType(row.reviewResult)" round effect="light">{{ resultText(row.reviewResult) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="异常项" min-width="90">
            <template #default="{row}">{{ parseJson(row.abnormalItems).length }} 项</template>
          </el-table-column>
          <el-table-column label="逻辑问题" min-width="90">
            <template #default="{row}">{{ parseJson(row.logicIssues).length }} 项</template>
          </el-table-column>
          <el-table-column label="历史波动" min-width="90">
            <template #default="{row}">{{ parseJson(row.historyCompare).length }} 项</template>
          </el-table-column>
          <el-table-column label="审核时间" min-width="155">
            <template #default="{row}">{{ formatDate(row.reviewTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{row}">
              <el-button link type="primary" @click="open(row)">查看详情</el-button>
              <el-button link type="success" @click="stream(row)">流式审核</el-button>
              <el-button v-if="row.reviewResult==='manual'" type="primary" size="small" @click="confirm(row)">确认通过</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <span>共 {{ total }} 条审核记录</span>
          <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="load" />
        </div>
      </section>

      <section class="content-card manual-panel">
        <div class="card-head">
          <div>
            <span class="eyebrow">MANUAL REVIEW</span>
            <h2>待人工复核</h2>
          </div>
          <el-tag type="warning" round>{{ manualTotal }} 项</el-tag>
        </div>
        <article v-for="item in manualList" :key="item.id" class="manual-card" @click="open(item)">
          <span class="manual-icon">!</span>
          <div>
            <strong>EX{{ String(item.examinationId).padStart(6, '0') }}</strong>
            <p>{{ firstWarning(item) || 'AI建议人工复核该报告。' }}</p>
          </div>
          <span class="manual-entry">查看原因</span>
        </article>
        <el-empty v-if="!manualList.length && !loading" description="暂无待人工复核报告" :image-size="70" />
      </section>
    </div>

    <el-drawer v-model="drawer" size="620px" :with-header="false">
      <div v-if="current" class="review-detail">
        <div class="drawer-head">
          <span class="rx-logo"><el-icon><MagicStick /></el-icon></span>
          <div>
            <small>AI REVIEW DETAIL</small>
            <h2>AI审核详情</h2>
            <p>EX{{ String(current.examinationId).padStart(6, '0') }}</p>
          </div>
          <button @click="drawer=false"><el-icon><Close /></el-icon></button>
        </div>

        <div class="review-result-card" :class="current.reviewResult">
          <div>
            <span>审核结论</span>
            <strong>{{ resultText(current.reviewResult) }}</strong>
          </div>
          <el-tag :type="tagType(current.reviewResult)" size="large" round>
            {{ current.reviewResult === 'reject' ? '必须重新检测' : current.reviewResult === 'manual' ? '需要检验师判断' : '系统已初审通过' }}
          </el-tag>
        </div>

        <section v-if="current.reviewResult !== 'pass'" class="review-reason" :class="current.reviewResult">
          <span>{{ current.reviewResult === 'reject' ? '退回重测原因' : '人工复核原因' }}</span>
          <strong>{{ decisionReason(current) }}</strong>
          <p>{{ current.reviewResult === 'reject'
            ? '该结果存在明显不可能值或检验逻辑错误，不应直接发布，必须重新采样或重新上机检测。'
            : '该结果可能是真实异常，也可能受标本、仪器或患者近期状态影响，需要检验师结合原始曲线和历史结果确认。' }}</p>
        </section>

        <section class="review-section">
          <h3>异常指标标注</h3>
          <div v-if="parseJson(current.abnormalItems).length" class="indicator-list">
            <article v-for="(x,i) in parseJson(current.abnormalItems)" :key="i">
              <strong>{{ x.name || x.item || '异常指标' }}</strong>
              <span>{{ x.value || '--' }}</span>
              <small>参考：{{ x.reference || '未提供' }}</small>
              <el-tag :type="x.status==='偏低'?'primary':'danger'" size="small">{{ x.status || '异常' }} {{ x.status==='偏低'?'↓':'↑' }}</el-tag>
            </article>
          </div>
          <p v-else class="empty-text">未发现超出参考范围的指标。</p>
        </section>

        <section class="review-section">
          <h3>逻辑合理性校验</h3>
          <div v-if="parseJson(current.logicIssues).length" class="issue-list">
            <p v-for="(x,i) in parseJson(current.logicIssues)" :key="i"><b>{{ levelText(x.level) }}</b>{{ x.content }}</p>
          </div>
          <p v-else class="empty-text">白细胞总数、分类计数及项目间关系未发现明显矛盾。</p>
        </section>

        <section class="review-section">
          <h3>历史结果波动</h3>
          <div v-if="parseJson(current.historyCompare).length" class="history-list">
            <article v-for="(x,i) in parseJson(current.historyCompare)" :key="i">
              <strong>{{ x.item }}</strong>
              <span>上次 {{ x.lastValue }} → 本次 {{ x.currentValue }}</span>
              <el-tag :type="x.level==='significant'?'warning':'success'" size="small">{{ x.change || '平稳' }}</el-tag>
            </article>
          </div>
          <p v-else class="empty-text">暂无明显异常波动。</p>
        </section>

        <section class="review-section">
          <h3>AI建议</h3>
          <p class="suggestion">{{ current.suggestions || '建议按当前审核结论处理。' }}</p>
        </section>

        <div v-if="current.reviewResult==='manual'" class="review-footer">
          <el-button type="primary" @click="confirm(current)">人工确认通过</el-button>
          <el-button type="danger" plain @click="reject(current)">退回重测</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, MagicStick, Refresh } from '@element-plus/icons-vue'
import { useLabStore } from '@/stores/lab'
import { getManualReviews, getReviewDetail, getReviewList, getReviewStats, manualConfirm, rejectReview } from '@/api/lab'
import type { ExaminationAiReview, ReviewStats } from '@/types'

const labStore = useLabStore()
const loading = ref(false)
const drawer = ref(false)
const filter = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const manualTotal = ref(0)
const reviews = ref<ExaminationAiReview[]>([])
const manualList = ref<ExaminationAiReview[]>([])
const current = ref<ExaminationAiReview>()
const stats = reactive<ReviewStats>({ total: 0, passCount: 0, manualCount: 0, rejectCount: 0, passRate: 0 })

const parseJson = (text?: string): any[] => {
  try {
    const data = JSON.parse(text || '[]')
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}
const resultText = (v: string) => v === 'pass' ? '自动通过' : v === 'reject' ? '退回重测' : '人工复核'
const tagType = (v: string) => v === 'pass' ? 'success' : v === 'reject' ? 'danger' : 'warning'
const levelText = (v?: string) => v === 'high' ? '高风险' : v === 'medium' ? '中风险' : v === 'low' ? '低风险' : '提示'
const formatDate = (s?: string) => s ? new Date(s).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '--'
const firstWarning = (row: ExaminationAiReview) => parseJson(row.warnings)[0]?.content || parseJson(row.logicIssues)[0]?.content
const decisionReason = (row: ExaminationAiReview) =>
  firstWarning(row) ||
  row.suggestions ||
  (row.reviewResult === 'reject' ? '检测结果存在明显逻辑错误，需要退回重新检测。' : '检测结果存在需要人工判断的异常。')

const load = async () => {
  loading.value = true
  try {
    const pageData = await labStore.loadReviewDashboard(filter.value, page.value, size.value)
    reviews.value = labStore.reviewRecords
    total.value = pageData.totalElements
    manualList.value = labStore.manualReviews
    manualTotal.value = labStore.manualTotal
    Object.assign(stats, labStore.stats)
  } finally {
    loading.value = false
  }
}
const reload = () => { page.value = 1; load() }
const open = async (row: ExaminationAiReview) => {
  current.value = (await getReviewDetail(row.id)).data.data
  drawer.value = true
}
const stream = async (row: ExaminationAiReview) => {
  drawer.value = false
  await labStore.streamReview(row)
}
const confirm = async (row: ExaminationAiReview) => {
  await ElMessageBox.confirm('确认该报告审核通过？', '人工确认', { type: 'warning', confirmButtonText: '确认通过' })
  await manualConfirm(row.id)
  ElMessage.success('已人工确认通过')
  drawer.value = false
  load()
}
const reject = async (row: ExaminationAiReview) => {
  const { value } = await ElMessageBox.prompt('请输入退回重测原因', '退回重测', { inputPlaceholder: '例如：标本疑似污染，建议重新采样检测', confirmButtonText: '确认退回' })
  await rejectReview(row.id, value || '检验结果需重新采样复测')
  ElMessage.success('已退回重测')
  drawer.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.stream-panel{margin-bottom:18px;border:1px solid #bfe9e4;background:#fff;box-shadow:0 14px 34px rgba(8,92,104,.08)}
.stream-output{min-height:220px;max-height:420px;overflow:auto;margin:0 22px 24px;padding:20px 22px;border-radius:16px;border:1px solid rgba(4,107,112,.18);background:linear-gradient(180deg,#073449,#092f3c);color:#e9fffb;font-size:16px;font-family:"Microsoft YaHei", "PingFang SC", Consolas, monospace;line-height:1.9;white-space:pre-wrap;box-shadow:inset 0 0 0 1px rgba(255,255,255,.05),0 10px 22px rgba(5,45,58,.12)}
</style>
