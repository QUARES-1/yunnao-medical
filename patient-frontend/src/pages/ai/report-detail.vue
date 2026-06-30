<template>
  <view class="page">
    <view v-if="loading" class="loading-card">
      <view class="loading-icon">AI</view>
      <text class="loading-title">正在解读报告...</text>
      <text class="loading-text">AI 正在分析指标异常、临床意义和复查建议。</text>
    </view>

    <view v-else-if="report" class="result">
      <view class="sheet-head">
        <view>
          <text class="eyebrow">REPORT INTERPRETATION</text>
          <text class="title">{{ reportName }}报告解读</text>
          <text class="number">检查编号：EX{{ String(report.examinationId || examinationId).padStart(6, '0') }}</text>
        </view>
        <text class="status">AI已解读</text>
      </view>

      <view class="summary-card danger">
        <view class="summary-icon">!</view>
        <view class="summary-content">
          <text class="summary-title">异常指标</text>
          <view v-if="abnormalIndicators.length" class="indicator-list">
            <view v-for="(item, index) in abnormalIndicators" :key="`${item.name}-${index}`" class="indicator-item">
              <view class="indicator-head">
                <text class="indicator-name">{{ item.name }}</text>
                <text class="indicator-status" :class="statusClass(item.status)">{{ displayStatus(item.status) }}</text>
              </view>
              <view class="indicator-data">
                <text class="indicator-value">{{ item.value }} {{ item.unit }}</text>
                <text class="indicator-reference">参考范围：{{ item.reference }}</text>
              </view>
            </view>
          </view>
          <text v-else class="summary-text">{{ abnormalFallback }}</text>
        </view>
      </view>

      <view class="block">
        <text class="block-title">通俗解读</text>
        <text class="block-text">{{ report.interpretationPatient || demo.interpretationPatient }}</text>
      </view>

      <view class="block advice">
        <text class="block-title">就医建议</text>
        <text class="block-text">{{ report.suggestions || demo.suggestions }}</text>
      </view>

      <view class="block mint">
        <text class="block-title">复查提醒</text>
        <text class="block-text">{{ report.reviewReminder || demo.reviewReminder }}</text>
      </view>

      <view class="ai-note">
        <text>AI</text>
        <view>
          <b>辅助说明</b>
          <p>AI 解读用于帮助理解报告，不能替代医生诊断。若症状明显或指标异常，请及时复诊。</p>
        </view>
      </view>

      <button class="primary" @tap="goBack">返回选择报告</button>
    </view>

    <view v-else class="empty-card">
      <view class="empty-icon">!</view>
      <text class="empty-title">没有找到报告解读</text>
      <text class="empty-text">请返回选择一份已完成的检查/检验报告。</text>
      <button class="primary" @tap="goBack">返回选择报告</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatientInterpretation } from '@/api/patient'
import type { ExaminationInterpretation } from '@/types/api'
import { showError } from '@/utils/request'

const examinationId = ref(0)
const reportName = ref('检验')
const report = ref<ExaminationInterpretation | null>(null)
const loading = ref(false)
interface AbnormalIndicator {
  name: string
  value: string
  unit?: string
  reference?: string
  status?: string
}
const demo = {
  abnormalItems: '白细胞略高、C反应蛋白升高',
  interpretationPatient: '结果提示身体可能存在炎症反应，需要结合发热、咳嗽、咽痛等症状综合判断。',
  suggestions: '建议携带报告到内科或呼吸内科复诊，由医生结合症状决定是否用药。',
  reviewReminder: '如症状持续或加重，建议 3-5 天后复查血常规。'
}

const abnormalIndicators = computed<AbnormalIndicator[]>(() => {
  const source = report.value?.abnormalItems
  if (Array.isArray(source)) return source as AbnormalIndicator[]
  if (typeof source !== 'string' || !source.trim()) return []
  try {
    const parsed = JSON.parse(source)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const abnormalFallback = computed(() => {
  const source = report.value?.abnormalItems
  if (!source) return '暂无明显异常指标'
  if (typeof source === 'string' && !source.trim().startsWith('[')) return source
  return '暂无明显异常指标'
})

function displayStatus(status?: string) {
  if (!status) return '异常'
  return status.replace('升高', '偏高').replace('降低', '偏低')
}

function statusClass(status?: string) {
  return status?.includes('低') || status?.includes('降') ? 'low' : 'high'
}

onLoad((query) => {
  const id = Number(query?.examinationId)
  if (!id) {
    uni.showToast({ title: '报告信息不存在', icon: 'none' })
    return
  }
  examinationId.value = id
  if (query?.reportName) {
    reportName.value = decodeURIComponent(String(query.reportName)).replace(/^AI演示[·・\s-]*/, '')
  }
  loadReport(id)
})

async function loadReport(id: number) {
  loading.value = true
  uni.showLoading({ title: 'AI正在解读...' })
  try {
    report.value = await getPatientInterpretation(id)
  } catch (error) {
    showError(error)
    report.value = { examinationId: id, ...demo } as ExaminationInterpretation
  } finally {
    uni.hideLoading()
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f8fb;padding:28rpx;color:#18394b}.result,.loading-card,.empty-card{background:#fff;border-radius:30rpx;box-shadow:0 10rpx 30rpx rgba(29,68,83,.08)}.result{padding:28rpx}.loading-card,.empty-card{padding:58rpx 34rpx;text-align:center}.loading-icon,.empty-icon{width:86rpx;height:86rpx;margin:0 auto 20rpx;border-radius:28rpx;background:#e6f5fb;color:#1689a9;display:flex;align-items:center;justify-content:center;font-size:30rpx;font-weight:850}.loading-title,.loading-text,.empty-title,.empty-text{display:block}.loading-title,.empty-title{font-size:31rpx;font-weight:850}.loading-text,.empty-text{margin-top:12rpx;color:#7d939a;font-size:22rpx;line-height:1.7}.sheet-head{display:flex;justify-content:space-between;align-items:flex-start;padding-bottom:24rpx;border-bottom:2rpx solid #edf2f3}.eyebrow,.title,.number{display:block}.eyebrow{font-size:18rpx;color:#1689a9;letter-spacing:2rpx}.title{font-size:36rpx;font-weight:850;margin:8rpx 0}.number{font-size:21rpx;color:#8aa0a8}.status{padding:10rpx 16rpx;background:#e8f6fb;color:#1689a9;border-radius:18rpx;font-size:21rpx}.summary-card{display:flex;gap:18rpx;margin-top:24rpx;padding:24rpx;border-radius:24rpx;background:#fff1ee}.summary-icon{width:56rpx;height:56rpx;flex:none;border-radius:18rpx;background:#f06455;color:#fff;display:flex;align-items:center;justify-content:center;font-size:26rpx;font-weight:850}.summary-content{flex:1;min-width:0}.summary-title,.summary-text{display:block}.summary-title{font-size:28rpx;font-weight:850}.summary-text{margin-top:8rpx;font-size:24rpx;line-height:1.7;color:#536b72}.indicator-list{margin-top:16rpx}.indicator-item{padding:18rpx;background:rgba(255,255,255,.76);border:1rpx solid #ffd7d1;border-radius:18rpx}.indicator-item+.indicator-item{margin-top:12rpx}.indicator-head,.indicator-data{display:flex;justify-content:space-between;align-items:center;gap:12rpx}.indicator-name{font-size:25rpx;font-weight:800;color:#294b59}.indicator-status{flex:none;padding:6rpx 13rpx;border-radius:14rpx;font-size:20rpx;font-weight:800;background:#fff0ed;color:#e2584d}.indicator-status.low{background:#eaf4ff;color:#3983ba}.indicator-value{font-size:25rpx;font-weight:800;color:#e2584d}.indicator-reference{font-size:20rpx;color:#879ba2;text-align:right}.indicator-data{margin-top:12rpx}.block{margin-top:20rpx;padding:24rpx;border-radius:24rpx;background:#f7fbfc}.block.advice{background:#eef8ff}.block.mint{background:#eaf8f5}.block-title{display:block;font-size:29rpx;font-weight:850;margin-bottom:12rpx}.block-text{display:block;font-size:25rpx;line-height:1.85;color:#536b72}.ai-note{display:flex;gap:14rpx;margin-top:22rpx;padding:18rpx;background:#edf8ff;border-radius:20rpx}.ai-note>text{width:48rpx;height:48rpx;flex:none;border-radius:15rpx;background:#3d8db2;color:#fff;display:flex;align-items:center;justify-content:center;font-size:18rpx;font-weight:850}.ai-note b,.ai-note p{display:block;margin:0}.ai-note b{font-size:22rpx}.ai-note p{font-size:20rpx;color:#6d848d;line-height:1.5;margin-top:5rpx}.primary{margin-top:26rpx;background:linear-gradient(90deg,#087d82,#2796cc);color:#fff;border-radius:22rpx;font-size:26rpx;font-weight:800}
</style>
