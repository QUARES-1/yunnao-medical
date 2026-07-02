<template>
  <div class="consultation-page" v-loading="pageLoading">
    <section class="consultation-hero">
      <div>
        <span class="hero-kicker">CLINICAL WORKSPACE</span>
        <h1>{{ isReadonly ? '诊疗详情回顾' : '患者看诊工作区' }}</h1>
        <p>{{ isReadonly ? '查看本次诊疗的病历、检查与处方信息' : '请依次完成病历记录、检查开立与处方处理' }}</p>
      </div>
      <div class="hero-number"><span>预约编号</span><strong>YN{{ String(registration?.id || '').padStart(6, '0') }}</strong></div>
    </section>
    <!-- 患者信息卡片 -->
    <el-card class="patient-card">
      <template #header>
        <div class="card-header">
          <span>患者信息</span>
          <el-tag :type="statusTagType(registration?.status)">{{ registration?.status }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="患者姓名">{{ registration?.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ registration?.patientGender || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ registration?.patientAge ? `${registration.patientAge}岁` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ registration?.patientPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="挂号日期">{{ registration?.registrationDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ registration?.timeSlot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ registration?.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史">
          <el-tag :type="hasAllergy ? 'danger' : 'success'" effect="light">{{ registration?.patientAllergyHistory || '无已知过敏史' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作区：Tabs -->
    <el-card class="workspace-card" style="margin-top: 16px;">
      <el-tabs v-model="activeTab">
        <!-- 病历 -->
        <el-tab-pane label="📝 病历记录" name="record">
          <el-form :model="recordForm" ref="recordFormRef" label-position="top" class="record-form">
            <el-form-item label="主诉" prop="chiefComplaint">
              <el-input v-model="recordForm.chiefComplaint" placeholder="患者主要症状及持续时间" maxlength="500" show-word-limit :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="现病史" prop="presentIllness">
              <el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" placeholder="详细描述现病史..." :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="既往史" prop="pastHistory">
              <el-input v-model="recordForm.pastHistory" type="textarea" :rows="2" placeholder="如：无特殊既往史" :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="体格检查" prop="physicalExamination">
              <el-input v-model="recordForm.physicalExamination" type="textarea" :rows="2" placeholder="如：T:36.5℃，P:80次/分" :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="诊断结果" prop="diagnosis">
              <el-input v-model="recordForm.diagnosis" placeholder="如：上呼吸道感染" maxlength="500" show-word-limit :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="治疗意见" prop="treatment">
              <el-input v-model="recordForm.treatment" type="textarea" :rows="2" placeholder="如：多休息，多喝水" :disabled="isReadonly" />
            </el-form-item>
            <el-form-item v-if="!isReadonly">
              <el-button type="primary" @click="saveRecord" :loading="savingRecord">保存病历</el-button>
            </el-form-item>
            <el-alert v-else type="info" :closable="false" :description="readonlyMessage" style="max-width:600px" />
          </el-form>

          <!-- AI 病历生成面板 -->
          <div v-if="!isReadonly" class="ai-panel">
            <div class="ai-panel-header">
              <span class="ai-icon">🤖</span>
              <span>AI 辅助诊疗</span>
              <el-tag size="small" type="success">DeepSeek</el-tag>
            </div>
            <div class="ai-panel-body">
              <el-input
                v-model="aiInputText"
                type="textarea"
                :rows="3"
                placeholder="请输入患者症状描述，如：患者女，28岁，咳嗽伴有发烧三天，体温38.5℃..."
                :disabled="aiGenerating"
              />
              <div class="ai-actions">
                <el-button
                  type="primary"
                  :loading="aiGenerating"
                  :disabled="!aiInputText.trim()"
                  @click="handleAiGenerate"
                  class="ai-generate-btn"
                >
                  {{ aiGenerating ? 'AI 分析中...' : '🤖 AI 生成病历' }}
                </el-button>
              </div>
              <!-- AI 生成结果展示 -->
              <div v-if="aiGeneratedRecord" class="ai-result-card">
                <div class="ai-result-header">
                  <span>✅ AI 生成结果</span>
                  <el-button type="success" size="small" @click="applyAiRecord">应用到表单</el-button>
                </div>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="主诉" :span="2">{{ aiGeneratedRecord.chiefComplaint || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="现病史" :span="2">{{ aiGeneratedRecord.presentIllness || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="既往史" :span="2">{{ aiGeneratedRecord.pastHistory || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="体格检查" :span="2">{{ aiGeneratedRecord.physicalExamination || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="诊断结果">{{ aiGeneratedRecord.diagnosis || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="治疗意见">{{ aiGeneratedRecord.treatment || '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 开检查 -->
        <el-tab-pane :label="`🔬 开立检查${completedExamCount ? `（${completedExamCount}项已完成）` : ''}`" name="exam">
          <div class="exam-section">
            <!-- 已开检查列表 -->
            <div v-if="existingExams.length > 0" class="existing-section">
              <div class="section-heading"><div class="section-title">已开检查记录</div><el-button :icon="Refresh" size="small" @click="refreshExams">刷新结果</el-button></div>
              <el-table :data="existingExams" border stripe style="width:100%;margin-bottom:20px">
                <el-table-column prop="itemName" label="项目名称" />
                <el-table-column prop="type" label="类型" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.type === '检查' ? 'primary' : 'warning'">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" :type="examStatusType(row.status)">{{ row.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="result" label="检查结果" min-width="200">
                  <template #default="{ row }">{{ row.result || '待检查' }}</template>
                </el-table-column>
                <el-table-column label="开单时间" width="170">
                  <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90">
                  <template #default="{ row }"><el-button v-if="row.status === '待检查'" link type="danger" @click="handleCancelExam(row)">撤销</el-button><span v-else>-</span></template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 检查项目选择（仅就诊中可操作） -->
            <div v-if="!isReadonly">
              <div class="section-title">开立新检查</div>
              <el-row :gutter="12" style="margin-bottom: 12px;">
                <el-col :span="6">
                  <el-select v-model="examTypeFilter" placeholder="全部类型" clearable @change="loadExamItems" style="width:100%">
                    <el-option label="检查" value="检查" />
                    <el-option label="检验" value="检验" />
                  </el-select>
                </el-col>
              </el-row>
              <el-table :data="examItems" border stripe style="width:100%">
                <el-table-column prop="name" label="项目名称" />
                <el-table-column prop="type" label="类型" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.type === '检查' ? 'primary' : 'warning'">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="price" label="价格(元)" width="100" />
                <el-table-column prop="description" label="说明" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button type="primary" size="small" @click="createExam(row)">开立</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <el-alert v-else-if="existingExams.length === 0" type="info" :closable="false" description="该挂号未开立检查" />
          </div>
        </el-tab-pane>

        <!-- 开处方 -->
        <el-tab-pane label="💊 开具处方" name="prescription">
          <div class="prescription-section">
            <!-- 已开处方展示 -->
            <div v-if="existingPrescriptions.length > 0" class="existing-section">
              <div v-for="(rx, idx) in existingPrescriptions" :key="idx" class="rx-block">
                <div class="rx-header">
                  <span class="section-title">处方 {{ idx + 1 }}</span>
                  <el-tag size="small" :type="rx.status === '已发药' ? 'success' : rx.status === '已撤销' ? 'info' : 'warning'">{{ rx.status }}</el-tag>
                  <span class="rx-time">开方时间：{{ formatTime(rx.createTime) }}</span>
                  <el-button class="print-btn" type="primary" plain size="small" :icon="Printer" @click="printPrescription(rx)">打印处方</el-button>
                  <el-button v-if="rx.status === '待发药'" link type="danger" @click="handleCancelPrescription(rx)">撤销</el-button>
                </div>
                <el-table :data="parseDrugs(rx.drugs)" border style="width:100%;margin-bottom:8px">
                  <el-table-column prop="medicineName" label="药品名称" />
                  <el-table-column prop="specification" label="规格" width="120" />
                  <el-table-column prop="quantity" label="数量" width="70" />
                  <el-table-column prop="unit" label="单位" width="70" />
                  <el-table-column prop="dosage" label="用法用量" min-width="160" />
                </el-table>
                <div class="rx-total">合计：¥{{ rx.totalAmount }}</div>
              </div>
            </div>

            <!-- 开新处方（仅就诊中可操作） -->
            <div v-if="!isReadonly">
              <div class="section-title" style="margin-top:20px">开立新处方</div>
              <el-row :gutter="12" style="margin-bottom: 16px;">
                <el-col :span="10">
                  <el-input v-model="medicineKeyword" placeholder="搜索药品名称" :prefix-icon="Search" @input="searchMedicine" clearable />
                </el-col>
              </el-row>
              <el-table :data="medicineList" border stripe max-height="240" style="width:100%;margin-bottom:16px">
                <el-table-column prop="name" label="药品名称" />
                <el-table-column prop="specification" label="规格" />
                <el-table-column prop="unit" label="单位" width="70" />
                <el-table-column prop="price" label="单价(元)" width="90" />
                <el-table-column prop="stock" label="库存" width="70" />
                <el-table-column label="添加" width="80">
                  <template #default="{ row }">
                    <el-button type="primary" size="small" :disabled="row.stock <= 0" @click="addDrug(row)">{{ row.stock > 0 ? '添加' : '无库存' }}</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 已添加药品 -->
              <div v-if="prescriptionDrugs.length > 0">
                <div class="drugs-title">本次处方药品</div>
                <el-table :data="prescriptionDrugs" border style="width:100%;margin-bottom:12px">
                  <el-table-column prop="medicineName" label="药品" />
                  <el-table-column prop="specification" label="规格" />
                  <el-table-column label="数量" width="145" align="center">
                    <template #default="{ row }">
                      <el-input-number v-model="row.quantity" :min="1" :max="row.stock" size="small" controls-position="right" class="quantity-input" @change="calcTotal" />
                    </template>
                  </el-table-column>
                  <el-table-column label="用法用量" min-width="160">
                    <template #default="{ row }">
                      <el-input v-model="row.dosage" placeholder="如：每次1粒，每日3次" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column label="小计(元)" width="90">
                    <template #default="{ row }">{{ (row.price * row.quantity).toFixed(2) }}</template>
                  </el-table-column>
                  <el-table-column label="删除" width="70">
                    <template #default="{ row, $index }">
                      <el-button type="danger" size="small" :icon="Delete" circle @click="removeDrug($index)" />
                    </template>
                  </el-table-column>
                </el-table>
                <div class="total-amount">合计：<b>¥{{ totalAmount }}</b></div>
                <el-button type="primary" @click="submitPrescriptionWithAiReview" :loading="savingPrescription" style="margin-top:12px">
                  提交处方
                </el-button>
              </div>
              <el-empty v-else description="请从上方搜索并添加药品" />
            </div>
            <el-alert v-if="isReadonly && existingPrescriptions.length === 0" type="info" :closable="false" description="该挂号未开具处方" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- AI 处方审核弹窗 -->
    <el-dialog v-model="aiReviewDialogVisible" title="🤖 AI 处方审核" width="600px" :close-on-click-modal="false">
      <div v-if="aiReviewResult">
        <div class="review-score">
          <span>AI 审核评分：</span>
          <span class="score-value" :class="aiReviewResult.reviewScore >= 80 ? 'score-pass' : 'score-fail'">
            {{ aiReviewResult.reviewScore }} / 100
          </span>
        </div>
        <div class="review-status">
          <el-tag :type="isAiReviewPassed(aiReviewResult) ? 'success' : 'warning'" size="large">
            {{ isAiReviewPassed(aiReviewResult) ? '✅ 审核通过，处方合理' : '⚠️ 存在风险，建议复核' }}
          </el-tag>
        </div>
        <div v-if="aiReviewResult.warnings && aiReviewResult.warnings.length > 0" class="review-warnings">
          <h4>⚠️ 警告信息</h4>
          <ul>
            <li v-for="(w, idx) in aiReviewResult.warnings" :key="idx">{{ w.content || w }}</li>
          </ul>
        </div>
        <div v-if="aiReviewResult.drugInteractions && aiReviewResult.drugInteractions.length > 0" class="review-warnings review-warnings--orange">
          <h4>💊 配伍/相互作用</h4>
          <ul>
            <li v-for="(w, idx) in aiReviewResult.drugInteractions" :key="idx">{{ formatReviewIssue(w) }}</li>
          </ul>
        </div>
        <div v-if="aiReviewResult.dosageIssues && aiReviewResult.dosageIssues.length > 0" class="review-warnings">
          <h4>📏 剂量风险</h4>
          <ul>
            <li v-for="(w, idx) in aiReviewResult.dosageIssues" :key="idx">{{ formatReviewIssue(w) }}</li>
          </ul>
        </div>
        <div v-if="aiReviewResult.allergyRisks && aiReviewResult.allergyRisks.length > 0" class="review-warnings">
          <h4>🧬 过敏风险</h4>
          <ul>
            <li v-for="(w, idx) in aiReviewResult.allergyRisks" :key="idx">{{ formatReviewIssue(w) }}</li>
          </ul>
        </div>
        <div v-if="aiReviewResult.suggestions" class="review-suggestions">
          <h4>💡 修改建议</h4>
          <p>{{ aiReviewResult.suggestions }}</p>
        </div>
      </div>
      <div v-else class="review-loading">
        <el-icon class="is-loading"><component is="Loading" /></el-icon>
        <span>AI 正在审核处方中...</span>
      </div>
      <template #footer>
        <el-button @click="aiReviewDialogVisible = false">返回修改处方</el-button>
        <el-button type="warning" @click="confirmPrescriptionAfterReview('skip')">忽略风险，继续提交</el-button>
        <el-button type="primary" @click="confirmPrescriptionAfterReview('ok')">确认提交处方</el-button>
      </template>
    </el-dialog>

    <!-- 完成看诊按钮 -->
    <div class="complete-bar" v-if="registration?.status === '就诊中'">
      <div><el-icon><InfoFilled /></el-icon><span>完成前请确认病历、检查和处方均已处理完毕</span></div>
      <el-button type="primary" size="large" @click="completeConsult" :loading="completing">
        ✅ 完成看诊
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete, Refresh, Printer } from '@element-plus/icons-vue'
import {
  getRegistrationDetail,
  getMedicalRecordByReg,
  saveMedicalRecord,
  getExaminationItems,
  createExamination,
  cancelExamination,
  getMedicineList,
  createPrescription,
  cancelPrescription,
  completeConsultation,
  getExaminationsByRegistration,
  getPrescriptionsByRegistration,
  aiGenerateRecord,
  aiCheckPrescription,
  aiGenerateRecordStream
} from '../../api/doctor'

const route = useRoute()
const router = useRouter()
const regId = route.params.regId

const pageLoading = ref(false)
const activeTab = ref('record')
const registration = ref(null)

const canEdit = computed(() => registration.value?.status === '就诊中')
const isReadonly = computed(() => !canEdit.value)
const readonlyMessage = computed(() => registration.value?.status === '待就诊'
  ? '请从工作台点击“开始看诊”后再编辑病历、检查和处方'
  : '该挂号已结束，诊疗资料为只读模式')
const hasAllergy = computed(() => {
  const value = registration.value?.patientAllergyHistory?.trim()
  return value && !['无', '无过敏史', '无已知过敏史'].includes(value)
})

// 病历
const recordForm = reactive({
  id: null,
  registrationId: Number(regId),
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExamination: '',
  diagnosis: '',
  treatment: ''
})
const savingRecord = ref(false)

// 检查
const examItems = ref([])
const examTypeFilter = ref('')
const existingExams = ref([])
const completedExamCount = computed(() => existingExams.value.filter(item => item.status === '已完成').length)

// 处方
const medicineKeyword = ref('')
const medicineList = ref([])
const prescriptionDrugs = ref([])
const savingPrescription = ref(false)
const completing = ref(false)
const existingPrescriptions = ref([])

// AI 相关状态
const aiInputText = ref('')
const aiGenerating = ref(false)
const aiGeneratedRecord = ref(null)
const aiReviewDialogVisible = ref(false)
const aiReviewResult = ref(null)
const aiReviewing = ref(false)

const totalAmount = computed(() => {
  return prescriptionDrugs.value.reduce((sum, d) => sum + d.price * d.quantity, 0).toFixed(2)
})

const statusTagType = (status) => {
  const map = { '待就诊': 'warning', '就诊中': 'primary', '已就诊': 'success', '已取消': 'danger' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
const examStatusType = status => ({ '已完成': 'success', '待检查': 'warning', '已撤销': 'info' }[status] || 'info')
const isAiReviewPassed = result => ['safe', 'pass', 'passed', '通过', '审核通过'].includes(result?.reviewResult)
const formatReviewIssue = item => {
  if (!item) return ''
  if (typeof item === 'string') return item
  const drug = item.drug || item.medicineName || item.name
  const issue = item.content || item.issue || item.reason || item.message || JSON.stringify(item)
  return drug ? `${drug}：${issue}` : issue
}

// 解析处方里的 drugs JSON 字符串
const parseDrugs = (drugsStr) => {
  try {
    return JSON.parse(drugsStr) || []
  } catch (e) {
    return []
  }
}

// 加载该挂号下的已开检查
const loadExistingExams = async () => {
  try {
    const res = await getExaminationsByRegistration(regId)
    existingExams.value = res.data || []
  } catch (e) {
    existingExams.value = []
  }
}

// 加载该挂号下的已开处方
const loadExistingPrescriptions = async () => {
  try {
    const res = await getPrescriptionsByRegistration(regId)
    existingPrescriptions.value = (res.data || []).filter(item => item.status !== '已撤销')
  } catch (e) {
    existingPrescriptions.value = []
  }
}

// 加载页面数据
const loadPage = async () => {
  pageLoading.value = true
  try {
    const [regRes, examRes, medRes] = await Promise.all([
      getRegistrationDetail(regId),
      getExaminationItems(),
      getMedicineList({ page: 1, size: 50 })
    ])
    registration.value = regRes.data
    examItems.value = examRes.data || []
    medicineList.value = medRes.data?.content || []

    // 并行加载病历、已开检查、已开处方
    const [recordRes] = await Promise.allSettled([
      getMedicalRecordByReg(regId),
      loadExistingExams(),
      loadExistingPrescriptions()
    ])

    if (recordRes.status === 'fulfilled' && recordRes.value.data) {
      Object.assign(recordForm, recordRes.value.data)
    }
  } catch (error) {
    ElMessage.error('该患者未挂号给当前医生，无法查看诊疗信息')
    await router.replace('/workbench')
  } finally {
    pageLoading.value = false
  }
}

// 保存病历
const saveRecord = async () => {
  if (!recordForm.chiefComplaint.trim() || !recordForm.diagnosis.trim() || !recordForm.treatment.trim()) {
    ElMessage.warning('请先填写主诉、诊断结果和治疗意见')
    return
  }
  savingRecord.value = true
  try {
    await saveMedicalRecord({ ...recordForm })
    ElMessage.success('病历保存成功')
    try {
      const res = await getMedicalRecordByReg(regId)
      if (res.data) recordForm.id = res.data.id
    } catch (e) {}
  } finally {
    savingRecord.value = false
  }
}

// 加载检查项目
const loadExamItems = async () => {
  const res = await getExaminationItems(examTypeFilter.value || undefined)
  examItems.value = res.data || []
}

// 开立检查
const createExam = async (item) => {
  await createExamination({ registrationId: Number(regId), itemId: item.id })
  ElMessage.success(`已开立检查：${item.name}`)
  await loadExistingExams()
}

const refreshExams = async () => {
  const before = completedExamCount.value
  await loadExistingExams()
  const added = completedExamCount.value - before
  if (added > 0) ElMessage.success(`有 ${added} 项新检查结果，请及时查看`)
  else ElMessage.info('已刷新，暂无新的检查结果')
}

const handleCancelExam = async row => {
  await ElMessageBox.confirm(`确认撤销“${row.itemName}”吗？`, '撤销检查', { type: 'warning' })
  await cancelExamination(row.id)
  ElMessage.success('检查项目已撤销')
  await loadExistingExams()
}

// 搜索药品
const searchMedicine = async () => {
  const res = await getMedicineList({ keyword: medicineKeyword.value, page: 1, size: 50 })
  medicineList.value = res.data?.content || []
}

// 添加药品
const addDrug = (medicine) => {
  if (!medicine.stock || medicine.stock <= 0) {
    ElMessage.warning(`${medicine.name}当前无库存`)
    return
  }
  const exists = prescriptionDrugs.value.find(d => d.medicineId === medicine.id)
  if (exists) {
    if (exists.quantity >= exists.stock) {
      ElMessage.warning(`${medicine.name}最多可开 ${exists.stock}${medicine.unit}`)
      return
    }
    exists.quantity++
    return
  }
  prescriptionDrugs.value.push({
    medicineId: medicine.id,
    medicineName: medicine.name,
    specification: medicine.specification,
    quantity: 1,
    unit: medicine.unit,
    dosage: '',
    price: medicine.price,
    stock: medicine.stock
  })
}

const removeDrug = (index) => {
  prescriptionDrugs.value.splice(index, 1)
}

const calcTotal = () => {}

// AI 生成病历（SSE 流式）
const handleAiGenerate = async () => {
  if (!aiInputText.value.trim()) {
    ElMessage.warning('请输入患者症状描述')
    return
  }
  aiGenerating.value = true
  aiGeneratedRecord.value = {
    chiefComplaint: '',
    presentIllness: '',
    pastHistory: '',
    physicalExamination: '',
    diagnosis: '',
    treatment: ''
  }
  try {
    const response = await aiGenerateRecordStream({
      patientId: registration.value?.patientId,
      inputText: aiInputText.value,
      inputType: 'symptom'
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // 解析 SSE 事件（以 \n\n 分隔）
      while (true) {
        const idx = buffer.indexOf('\n\n')
        if (idx < 0) break
        const eventText = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        let eventName = ''
        let eventData = ''
        for (const rawLine of eventText.split('\n')) {
          const line = rawLine.replace(/\r$/, '')
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            eventData += line.slice(5).trim()
          }
        }
        if (eventName && eventData) {
          try {
            const data = JSON.parse(eventData)
            if (eventName === 'data' && data.field && data.char !== undefined) {
              if (!aiGeneratedRecord.value[data.field]) aiGeneratedRecord.value[data.field] = ''
              aiGeneratedRecord.value[data.field] += data.char
            }
            if (eventName === 'complete' && data.status === 'complete') {
              ElMessage.success('AI 病历生成完成')
            }
          } catch (e) {
            // 忽略解析失败的行
          }
        }
      }
    }
  } catch (e) {
    ElMessage.error('AI 流式生成失败：' + (e.message || '请检查后端服务'))
    // 降级为普通 API
    try {
      aiGenerating.value = true
      const res = await aiGenerateRecord({
        patientId: registration.value?.patientId,
        inputText: aiInputText.value,
        inputType: 'symptom'
      })
      aiGeneratedRecord.value = res.data
      ElMessage.success('AI 病历生成完成（降级模式）')
    } catch (e2) {
      ElMessage.error('降级调用也失败：' + (e2.message || '请检查后端服务'))
    }
  } finally {
    aiGenerating.value = false
  }
}

// 将AI生成的病历应用到表单
const applyAiRecord = () => {
  if (!aiGeneratedRecord.value) return
  const fields = ['chiefComplaint', 'presentIllness', 'pastHistory', 'physicalExamination', 'diagnosis', 'treatment']
  fields.forEach(f => {
    if (aiGeneratedRecord.value[f]) {
      recordForm[f] = aiGeneratedRecord.value[f]
    }
  })
  ElMessage.success('AI 生成内容已应用到表单，请核对后保存')
}

// 带AI审核的处方提交
const submitPrescriptionWithAiReview = async () => {
  if (prescriptionDrugs.value.some(d => !d.dosage)) {
    ElMessage.warning('请填写所有药品的用法用量')
    return
  }
  if (prescriptionDrugs.value.some(d => d.quantity < 1 || d.quantity > d.stock)) {
    ElMessage.warning('处方数量不能超过当前药品库存')
    return
  }
  // 打开 AI 审核弹窗
  aiReviewDialogVisible.value = true
  aiReviewResult.value = null
  aiReviewing.value = true
  try {
    const drugs = prescriptionDrugs.value.map(d => ({
      medicineName: d.medicineName,
      specification: d.specification,
      quantity: d.quantity,
      unit: d.unit,
      dosage: d.dosage
    }))
    const res = await aiCheckPrescription({
      patientId: registration.value?.patientId,
      patientAge: registration.value?.patientAge,
      patientGender: registration.value?.patientGender,
      drugs
    })
    aiReviewResult.value = res.data
  } catch (e) {
    aiReviewResult.value = {
      reviewResult: 'manual',
      reviewScore: 0,
      warnings: [{ content: 'AI审核接口异常：' + (e.message || '未知错误') }],
      suggestions: '请人工审核处方'
    }
  } finally {
    aiReviewing.value = false
  }
}

// AI审核后确认提交
const confirmPrescriptionAfterReview = async (action) => {
  aiReviewDialogVisible.value = false
  if (action === 'skip') {
    // 忽略风险直接提交
    await doSubmitPrescription()
  } else {
    await doSubmitPrescription()
  }
}

// 实际提交处方
const doSubmitPrescription = async () => {
  savingPrescription.value = true
  try {
    const drugs = prescriptionDrugs.value.map(({ price, stock, ...rest }) => rest)
    await createPrescription({
      registrationId: Number(regId),
      drugs: JSON.stringify(drugs),
      totalAmount: parseFloat(totalAmount.value)
    })
    ElMessage.success('处方开具成功')
    prescriptionDrugs.value = []
    await loadExistingPrescriptions()
  } finally {
    savingPrescription.value = false
  }
}

// 提交处方（原始方法，保留兼容）
const submitPrescription = async () => {
  if (prescriptionDrugs.value.some(d => !d.dosage)) {
    ElMessage.warning('请填写所有药品的用法用量')
    return
  }
  if (prescriptionDrugs.value.some(d => d.quantity < 1 || d.quantity > d.stock)) {
    ElMessage.warning('处方数量不能超过当前药品库存')
    return
  }
  savingPrescription.value = true
  try {
    const drugs = prescriptionDrugs.value.map(({ price, stock, ...rest }) => rest)
    await createPrescription({
      registrationId: Number(regId),
      drugs: JSON.stringify(drugs),
      totalAmount: parseFloat(totalAmount.value)
    })
    ElMessage.success('处方开具成功')
    prescriptionDrugs.value = []
    await loadExistingPrescriptions()
  } finally {
    savingPrescription.value = false
  }
}

const handleCancelPrescription = async rx => {
  await ElMessageBox.confirm('确认撤销该处方吗？撤销后药品库存将自动返还。', '撤销处方', { type: 'warning' })
  await cancelPrescription(rx.id)
  ElMessage.success('处方已撤销，库存已返还')
  await Promise.all([loadExistingPrescriptions(), searchMedicine()])
}

const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]))
const printPrescription = rx => {
  const drugs = parseDrugs(rx.drugs)
  const rows = drugs.map(d => `<tr><td>${escapeHtml(d.medicineName)}</td><td>${escapeHtml(d.specification)}</td><td>${escapeHtml(d.quantity)}${escapeHtml(d.unit)}</td><td>${escapeHtml(d.dosage)}</td></tr>`).join('')
  const win = window.open('', '_blank', 'width=860,height=720')
  if (!win) return ElMessage.warning('浏览器阻止了打印窗口，请允许弹窗后重试')
  win.document.write(`<!doctype html><html><head><title>处方单</title><style>body{font-family:Arial,"Microsoft YaHei";padding:40px;color:#223}h1{text-align:center}table{width:100%;border-collapse:collapse;margin:24px 0}th,td{border:1px solid #bbb;padding:10px;text-align:left}.meta{display:flex;justify-content:space-between;line-height:2}.total{text-align:right;font-size:18px}.sign{margin-top:55px;text-align:right}</style></head><body><h1>云脑诊疗平台处方单</h1><div class="meta"><span>患者：${escapeHtml(registration.value?.patientName)}</span><span>科室：${escapeHtml(registration.value?.departmentName)}</span><span>医生：${escapeHtml(registration.value?.doctorName)}</span></div><div class="meta"><span>处方编号：RX${String(rx.id).padStart(6,'0')}</span><span>开方时间：${escapeHtml(formatTime(rx.createTime))}</span></div><table><thead><tr><th>药品</th><th>规格</th><th>数量</th><th>用法用量</th></tr></thead><tbody>${rows}</tbody></table><div class="total">合计：¥${escapeHtml(rx.totalAmount)}</div><div class="sign">医师签名：________________</div></body></html>`)
  win.document.close(); win.focus(); setTimeout(() => win.print(), 250)
}

// 完成看诊
const completeConsult = async () => {
  if (!recordForm.id || !recordForm.chiefComplaint.trim() || !recordForm.diagnosis.trim() || !recordForm.treatment.trim()) {
    activeTab.value = 'record'
    ElMessage.warning('请先保存完整病历，再完成看诊')
    return
  }
  await ElMessageBox.confirm('确认完成本次看诊？完成后将无法继续修改。', '提示', { type: 'warning' })
  completing.value = true
  try {
    await completeConsultation(regId)
    ElMessage.success('看诊已完成')
    router.push('/workbench')
  } finally {
    completing.value = false
  }
}

onMounted(loadPage)
</script>

<style scoped>
.consultation-page{padding:2px}.consultation-hero{position:relative;overflow:hidden;display:flex;align-items:center;justify-content:space-between;min-height:126px;padding:25px 31px;border-radius:20px;background:linear-gradient(125deg,#073b5c,#087b7b 65%,#18a18e);color:#fff;box-shadow:0 15px 36px rgba(10,76,90,.15)}.consultation-hero:after{content:'';position:absolute;width:260px;height:260px;right:-75px;top:-155px;border:1px solid rgba(255,255,255,.12);border-radius:50%;box-shadow:0 0 0 45px rgba(255,255,255,.03)}.consultation-hero>div{position:relative;z-index:1}.hero-kicker{font-size:10px;font-weight:750;letter-spacing:2px;opacity:.64}.consultation-hero h1{margin:7px 0 6px;font-size:25px}.consultation-hero p{color:rgba(255,255,255,.65);font-size:11px}.hero-number{min-width:140px;padding:13px 16px;border:1px solid rgba(255,255,255,.16);border-radius:14px;background:rgba(255,255,255,.1)}.hero-number span,.hero-number strong{display:block}.hero-number span{color:rgba(255,255,255,.56);font-size:9px}.hero-number strong{margin-top:4px;font-size:16px;letter-spacing:1px}.patient-card,.workspace-card{border:1px solid #e1ebed;border-radius:19px;box-shadow:0 9px 28px rgba(26,67,81,.06)}.patient-card{margin-top:17px}.patient-card :deep(.el-card__header){padding:16px 22px;border-bottom:1px solid #eaf0f1}.patient-card :deep(.el-card__body){padding:18px 22px}.workspace-card :deep(.el-card__body){padding:0}.workspace-card :deep(.el-tabs__header){margin:0;padding:0 24px;border-bottom:1px solid #e7eef0;background:#fbfcfc}.workspace-card :deep(.el-tabs__nav-wrap:after){display:none}.workspace-card :deep(.el-tabs__item){height:58px;padding:0 25px;color:#7d9299;font-size:13px}.workspace-card :deep(.el-tabs__item.is-active){color:#087f84;font-weight:700}.workspace-card :deep(.el-tabs__active-bar){height:3px;border-radius:3px;background:#17a18f}.workspace-card :deep(.el-tab-pane){padding:25px 27px 28px}.record-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 22px;max-width:none}.record-form :deep(.el-form-item){margin-bottom:19px}.record-form :deep(.el-form-item:nth-child(2)),.record-form :deep(.el-form-item:nth-child(6)),.record-form :deep(.el-form-item:nth-child(7)){grid-column:1/-1}.record-form :deep(.el-form-item__label){padding-bottom:7px;color:#526d77;font-size:12px;font-weight:650}.record-form :deep(.el-input__wrapper){min-height:43px;border-radius:10px;box-shadow:0 0 0 1px #dfe9eb inset}.record-form :deep(.el-textarea__inner){border:1px solid #dfe9eb;border-radius:11px;box-shadow:none;line-height:1.65}.record-form .el-button--primary{border:0;border-radius:10px;background:linear-gradient(135deg,#087f84,#16a393)}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;color:#294854;
}
.patient-card :deep(.el-descriptions__label) {
  width: 90px;
  font-weight: 650;color:#647d85;background:#f7fafb!important;
}
.patient-card :deep(.el-descriptions__content){color:#294852;font-weight:600}.patient-card :deep(.el-descriptions__cell){padding:13px 15px!important;border-color:#e6edef!important}
.section-title {
  font-weight: 600;
  font-size: 15px;
  color:#294852;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left:3px solid #16a293;
}
.existing-section {
  margin-bottom: 20px;
}
.rx-block {
  background:#f8fbfb;
  border:1px solid #dfeaec;
  border-radius:14px;
  padding:18px;
  margin-bottom: 16px;
}
.rx-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.rx-header .section-title {
  margin-bottom: 0;
}
.rx-time {
  color: #909399;
  font-size: 13px;
  margin-left: auto;
}
.rx-total {
  text-align: right;
  font-size: 15px;
  color: #606266;
}
.rx-total b {
  color: #e6a23c;
  font-size: 18px;
}
.drugs-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.total-amount {
  text-align: right;
  font-size: 16px;
  color: #606266;
  margin-top: 8px;
}
.total-amount b {
  color: #e6a23c;
  font-size: 20px;
}
.exam-section :deep(.el-table),.prescription-section :deep(.el-table){border-radius:12px;overflow:hidden}.exam-section :deep(.el-table th.el-table__cell),.prescription-section :deep(.el-table th.el-table__cell){background:#f6f9fa;color:#687f87;font-size:12px}.exam-section :deep(.el-button--primary),.prescription-section :deep(.el-button--primary){border:0;border-radius:9px;background:linear-gradient(135deg,#087f84,#16a393)}.drugs-title{padding-left:10px;border-left:3px solid #16a293}.complete-bar{display:flex;align-items:center;justify-content:space-between;margin-top:16px;padding:17px 20px;border:1px solid #d9e9e7;border-radius:16px;background:linear-gradient(100deg,#eff9f6,#f8fcfb);box-shadow:0 7px 20px rgba(26,67,81,.05)}.complete-bar>div{display:flex;align-items:center;gap:8px;color:#71878e;font-size:11px}.complete-bar>div .el-icon{color:#138f80;font-size:17px}.complete-bar .el-button{border:0;border-radius:11px;background:linear-gradient(135deg,#087f84,#16a393);box-shadow:0 8px 18px rgba(8,127,132,.18)}@media(max-width:800px){.hero-number{display:none}.record-form{grid-template-columns:1fr}.record-form :deep(.el-form-item:nth-child(n)){grid-column:auto}.complete-bar{align-items:stretch;flex-direction:column;gap:14px}}
.consultation-page{min-width:0;max-width:100%;overflow-x:hidden}.patient-card,.workspace-card{min-width:0}.exam-section,.prescription-section{min-width:0;max-width:100%;overflow:hidden}.exam-section :deep(.el-table),.prescription-section :deep(.el-table){max-width:100%}.quantity-input{width:105px}.quantity-input :deep(.el-input__wrapper){padding-left:8px;padding-right:30px}
.section-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}.section-heading .section-title{margin-bottom:0}.section-heading .el-button{border-radius:9px;color:#087f84}.print-btn{height:32px!important;padding:0 14px!important;border:1px solid #0c938d!important;border-radius:9px!important;background:#e9f8f5!important;color:#087f84!important;font-weight:700!important;box-shadow:0 4px 10px rgba(8,127,132,.12)}.print-btn:hover{background:#0b928b!important;color:#fff!important;transform:translateY(-1px)}

/* AI 面板样式 */
.ai-panel {
  margin-top: 20px;
  border: 1px solid #d5edeb;
  border-radius: 14px;
  background: linear-gradient(135deg, #f6fdfc, #edf8f6);
  overflow: hidden;
}
.ai-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #087f84, #16a393);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
}
.ai-panel-header .ai-icon {
  font-size: 20px;
}
.ai-panel-header .el-tag {
  margin-left: auto;
}
.ai-panel-body {
  padding: 16px 18px;
}
.ai-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.ai-generate-btn {
  border: 0 !important;
  border-radius: 9px !important;
  background: linear-gradient(135deg, #087f84, #16a393) !important;
  box-shadow: 0 6px 16px rgba(8,127,132,.2) !important;
}
.ai-result-card {
  margin-top: 16px;
  border: 1px solid #c8e6e1;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}
.ai-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #eaf8f5;
  font-weight: 600;
  color: #087f84;
  border-bottom: 1px solid #d5edeb;
}

/* AI 审核弹窗样式 */
.review-score {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}
.score-value {
  font-size: 28px;
}
.score-pass { color: #67c23a; }
.score-fail { color: #f56c6c; }
.review-status {
  margin-bottom: 16px;
}
.review-warnings {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fef0f0;
  border-radius: 8px;
  border-left: 4px solid #f56c6c;
}
.review-warnings--orange {
  background: #fff7e8;
  border-left-color: #e6a23c;
}
.review-warnings h4 {
  margin: 0 0 8px 0;
  color: #f56c6c;
}
.review-warnings ul {
  margin: 0;
  padding-left: 20px;
}
.review-warnings li {
  line-height: 1.8;
  color: #606266;
}
.review-suggestions {
  margin-top: 16px;
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}
.review-suggestions h4 {
  margin: 0 0 8px 0;
  color: #409eff;
}
.review-suggestions p {
  margin: 0;
  color: #606266;
  white-space: pre-wrap;
}
.review-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: #909399;
  font-size: 15px;
}
</style>




