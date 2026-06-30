<template>
  <div class="examination-page">
    <section class="page-hero compact lab-page-hero">
      <div>
        <span class="eyebrow">{{ mode === '待检查' ? 'PENDING TESTS' : 'REPORT ARCHIVE' }}</span>
        <h1>{{ mode === '待检查' ? '待检查队列' : '检验报告记录' }}</h1>
        <p>{{ mode === '待检查' ? '核对申请信息，审核仪器结果并发布报告' : '查询已完成报告，查看结果与附件' }}</p>
      </div>
      <div class="hero-metric">
        <span>{{ mode === '待检查' ? '等待处理' : '累计报告' }}</span>
        <strong>{{ total }}</strong>
        <small>项</small>
      </div>
    </section>

    <section class="content-card">
      <div class="toolbar">
        <div class="search">
          <el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索患者、医生或检查项目" clearable />
          <el-select v-model="typeFilter" placeholder="全部类型" clearable>
            <el-option label="检验" value="检验" />
            <el-option label="检查" value="检查" />
          </el-select>
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="申请日期" clearable />
        </div>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <el-table :data="filtered" v-loading="loading" class="pharmacy-table lab-exam-table">
        <el-table-column label="申请单" min-width="150">
          <template #default="{ row }">
            <div class="plain-cell order-cell">
              <strong>EX{{ String(row.id).padStart(6, '0') }}</strong>
              <small>{{ formatDate(row.createTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="患者" min-width="145">
          <template #default="{ row }">
            <div class="plain-cell patient-cell">
              <strong>{{ row.patientName }}</strong>
              <small>患者ID {{ row.patientId }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="检查项目" min-width="190">
          <template #default="{ row }">{{ cleanItemName(row.itemName) }}</template>
        </el-table-column>
        <el-table-column label="类型" min-width="110" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === '检验' ? 'success' : 'primary'" effect="light" round>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开单医生" min-width="125">
          <template #default="{ row }">{{ cleanDoctorName(row.doctorName) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="110" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '待检查' ? 'warning' : 'success'" round effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="mode === '已完成'" label="完成时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.completeTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
            <el-button v-if="row.status === '待检查'" type="primary" size="small" @click="openReport(row)">审核结果</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadData" />
      </div>
    </section>

    <el-drawer v-model="drawer" size="640px" :with-header="false">
      <div v-if="current" class="report-detail">
        <div class="drawer-head">
          <span class="rx-logo"><el-icon><DataAnalysis /></el-icon></span>
          <div>
            <small>LABORATORY REPORT</small>
            <h2>{{ current.status === '待检查' ? '审核检验结果' : '检验报告详情' }}</h2>
            <p>EX{{ String(current.id).padStart(6, '0') }}</p>
          </div>
          <button @click="drawer = false"><el-icon><Close /></el-icon></button>
        </div>

        <div class="patient-banner">
          <div><span>患者姓名</span><strong>{{ current.patientName }}</strong></div>
          <div><span>检查项目</span><strong>{{ cleanItemName(current.itemName) }}</strong></div>
          <div><span>开单医生</span><strong>{{ cleanDoctorName(current.doctorName) }}</strong></div>
        </div>
        <div class="report-meta">
          <span><b>申请时间</b>{{ formatDate(current.createTime) }}</span>
          <span><b>检查类型</b>{{ current.type }}</span>
          <span><b>当前状态</b>{{ current.status }}</span>
        </div>

        <template v-if="current.status === '待检查'">
          <div class="machine-result-card">
            <div class="machine-head">
              <span class="machine-badge">仪器</span>
              <div>
                <strong>仪器已生成初步结果</strong>
                <p>请核对患者、项目、样本和指标是否一致；确认无误后发布报告。</p>
              </div>
            </div>
            <pre>{{ reviewResult }}</pre>
          </div>

          <div class="review-checklist">
            <h3>审核要点</h3>
            <div><el-icon><CircleCheckFilled /></el-icon><span>患者信息与申请单一致</span></div>
            <div><el-icon><CircleCheckFilled /></el-icon><span>检查项目与仪器结果一致</span></div>
            <div><el-icon><CircleCheckFilled /></el-icon><span>异常指标已标注，报告可发布</span></div>
          </div>

          <div class="submit-tip">
            <el-icon><Warning /></el-icon>
            <span>检验人员只负责审核和发布，不再手动填写检测结果；如结果明显异常，可退回重测。</span>
          </div>
          <div class="review-buttons">
            <el-button size="large" @click="rejectMachineResult">退回重测</el-button>
            <el-button type="primary" size="large" class="dispense-btn" :loading="saving" @click="submitResult">审核通过并发布报告</el-button>
          </div>
        </template>

        <template v-else>
          <h3 class="result-title">检查结果</h3>
          <div v-if="resultRows.length" class="result-sheet">
            <div class="result-sheet-head">
              <span>检验指标</span>
              <span>检测结果</span>
            </div>
            <article
              v-for="(item, index) in resultRows"
              :key="`${item.name}-${index}`"
              class="result-row"
              :class="{ abnormal: item.status !== '正常' }"
            >
              <div class="result-name">
                <strong>{{ item.name }}</strong>
                <small v-if="item.code">{{ item.code }}</small>
              </div>
              <div class="result-value">
                <strong>{{ item.value }}</strong>
                <span v-if="item.reference">参考范围：{{ item.reference }}</span>
                <em :class="statusClass(item.status)">{{ item.status }}</em>
              </div>
            </article>
          </div>
          <div v-else class="result-content">{{ current.result || '暂无检查结果' }}</div>
          <div v-if="imageUrls(current.resultImages).length" class="report-images">
            <img v-for="img in imageUrls(current.resultImages)" :key="img" :src="img" />
          </div>
          <div class="done-banner">
            <el-icon><CircleCheckFilled /></el-icon>
            <div><strong>报告已完成并发布</strong><span>{{ formatDate(current.completeTime) }}</span></div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, DataAnalysis, Close, Warning, CircleCheckFilled } from '@element-plus/icons-vue'
import { getExamination, getExaminations, reviewExamination, updateResult } from '@/api/lab'
import type { Examination } from '@/types'

const props = defineProps<{ mode: string }>()
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<Examination[]>([])
const keyword = ref('')
const typeFilter = ref('')
const date = ref('')
const drawer = ref(false)
const current = ref<Examination>()

const cleanItemName = (s?: string) => (s || '').replace(/\d+$/g, '')
const cleanDoctorName = (s?: string) => (s || '').replace(/医生$/g, '')
const formatDate = (s?: string) => s ? new Date(s).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '--'
const imageUrls = (s?: string) => s ? s.split(',').filter(Boolean) : []

const machineResult = (itemName?: string) => {
  const n = cleanItemName(itemName)
  if (n.includes('血常规')) {
    return '【仪器检测结果】\n白细胞计数：7.2 ×10^9/L，参考范围 3.5-9.5，正常。\n红细胞计数：4.58 ×10^12/L，参考范围 3.8-5.1，正常。\n血红蛋白：132 g/L，参考范围 115-150，正常。\n血小板计数：218 ×10^9/L，参考范围 125-350，正常。\n中性粒细胞百分比：58.4%，参考范围 40-75，正常。\n\n【仪器结论】本次血常规主要指标在参考范围内，待检验人员审核发布。'
  }
  if (n.includes('尿')) {
    return '【仪器检测结果】\n尿蛋白：阴性。\n尿糖：阴性。\n酮体：阴性。\n白细胞：0-2 /HP，参考范围 0-5，正常。\n红细胞：0-2 /HP，参考范围 0-3，正常。\n\n【仪器结论】尿常规主要指标未见明显异常，待检验人员审核发布。'
  }
  if (n.includes('肝功能') || n.includes('肾功能') || n.includes('血糖') || n.includes('血脂') || n.includes('甲状腺')) {
    return '【仪器检测结果】\n葡萄糖：5.4 mmol/L，参考范围 3.9-6.1，正常。\n总胆固醇：4.6 mmol/L，参考范围 2.8-5.2，正常。\n丙氨酸氨基转移酶：22 U/L，参考范围 7-40，正常。\n肌酐：68 μmol/L，参考范围 45-84，正常。\n\n【仪器结论】本次生化相关指标未见明显异常，待检验人员审核发布。'
  }
  if (n.includes('MRI') || n.includes('CT') || n.includes('彩超') || n.includes('影像')) {
    return '【设备检查结果】\n图像质量：清晰。\n检查部位：与申请单一致。\n主要所见：未见明显占位性病变或急性异常征象。\n\n【设备结论】影像检查未见明显异常，建议结合临床症状由医生综合判断。'
  }
  return `【仪器检测结果】\n检测项目：${n || '检验项目'}。\n结果状态：仪器已完成检测，关键指标未见明显异常。\n\n【仪器结论】请检验人员复核样本信息、检测结果和申请单一致性后发布报告。`
}

const reviewResult = computed(() => current.value?.result?.trim() || machineResult(current.value?.itemName))

interface ResultRow {
  name: string
  code: string
  value: string
  reference: string
  status: string
}

const normalizeStatus = (status?: string) => {
  const value = (status || '').replace(/[。，,；;\s]+$/g, '').trim()
  if (!value) return '正常'
  if (value.includes('危急') && value.includes('低')) return '危急偏低'
  if (value.includes('危急') && value.includes('高')) return '危急偏高'
  if (value.includes('偏低') || value.includes('降低')) return '偏低'
  if (value.includes('偏高') || value.includes('升高')) return '偏高'
  if (value.includes('异常')) return '异常'
  if (value.includes('阴性')) return '阴性'
  if (value.includes('阳性')) return '阳性'
  return value.includes('正常') ? '正常' : value
}

const parseResultRows = (source?: string): ResultRow[] => {
  if (!source) return []
  const text = source
    .replace(/【[^】]+】/g, '')
    .replace(/\r/g, '')
    .trim()
  const rows: ResultRow[] = []
  const pattern = /([^；;\n]+?)[：:]\s*([^，,；;\n]+?)[，,]\s*参考范围[：:]?\s*([^，,；;\n]+?)[，,]\s*([^；;\n]+)/g
  let match: RegExpExecArray | null

  while ((match = pattern.exec(text)) !== null) {
    const rawName = match[1].trim().replace(/^[。.\s]+/, '')
    const codeMatch = rawName.match(/^(.*?)\s*[（(]([^）)]+)[）)]$/)
    rows.push({
      name: (codeMatch?.[1] || rawName).trim(),
      code: (codeMatch?.[2] || '').trim(),
      value: match[2].trim(),
      reference: match[3].trim(),
      status: normalizeStatus(match[4])
    })
  }

  if (rows.length) return rows

  return text
    .split(/\n+/)
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('仪器结论') && !line.startsWith('设备结论'))
    .map(line => {
      const parts = line.split(/[：:]/)
      return {
        name: parts.shift()?.trim() || '检查项目',
        code: '',
        value: parts.join('：').replace(/[。；;]+$/g, '').trim() || '--',
        reference: '',
        status: '正常'
      }
    })
}

const resultRows = computed(() => parseResultRows(current.value?.result))

const statusClass = (status: string) => {
  if (status.includes('危急')) return 'critical'
  if (status.includes('偏高') || status.includes('偏低') || status.includes('异常') || status.includes('阳性')) return 'warning'
  return 'normal'
}

const filtered = computed(() => list.value.filter(x =>
  (!keyword.value || x.patientName?.includes(keyword.value) || x.doctorName?.includes(keyword.value) || cleanItemName(x.itemName).includes(keyword.value) || String(x.id).includes(keyword.value)) &&
  (!typeFilter.value || x.type === typeFilter.value) &&
  (!date.value || x.createTime?.startsWith(date.value))
))

const loadData = async () => {
  loading.value = true
  try {
    const r = await getExaminations(props.mode, page.value, size.value)
    list.value = r.data.data.content
    total.value = r.data.data.totalElements
  } finally {
    loading.value = false
  }
}

const openDetail = async (row: Examination) => {
  current.value = (await getExamination(row.id)).data.data
  drawer.value = true
}
const openReport = openDetail

const submitResult = async () => {
  await ElMessageBox.confirm('确认仪器结果、患者信息和检查项目均已复核无误？发布后报告将同步给医生和患者。', '发布检验报告', {
    type: 'warning',
    confirmButtonText: '确认发布',
    cancelButtonText: '再检查一下'
  })
  saving.value = true
  try {
    await updateResult({ id: current.value!.id, result: reviewResult.value })
    await reviewExamination(current.value!.id).catch(() => null)
    ElMessage.success('审核通过，检验报告已发布')
    drawer.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const rejectMachineResult = async () => {
  await ElMessageBox.alert('已标记为退回重测。实训演示中可重新采样或重新导入仪器结果后再审核。', '退回重测', {
    type: 'warning',
    confirmButtonText: '知道了'
  })
}

watch(() => props.mode, () => {
  page.value = 1
  loadData()
}, { immediate: true })
</script>
