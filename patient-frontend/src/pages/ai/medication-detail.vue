<template>
  <view class="page">
    <view v-if="loading" class="loading-card">
      <view class="loading-icon">AI</view>
      <text class="loading-title">正在生成个性化用药说明...</text>
      <text class="loading-text">正在结合处方、患者资料和诊断信息分析，请稍等。</text>
    </view>

    <view v-else-if="guide" class="guide">
      <view class="sheet-head">
        <view>
          <text class="sheet-eyebrow">AI MEDICATION PLAN</text>
          <text class="sheet-title">个性化用药说明单</text>
          <text class="sheet-number">处方编号：RX{{ String(guide.prescriptionId).padStart(6, '0') }}</text>
        </view>
        <view class="status-wrap">
          <text class="ai-live" :class="{ fallback: !guide.aiGenerated }">{{ guide.source || 'AI实时生成' }}</text>
          <text class="status">{{ guide.prescriptionStatus || '已生成' }}</text>
        </view>
      </view>

      <view class="patient-card">
        <view class="patient-main">
          <view class="patient-avatar">{{ (guide.patientName || '患').slice(0, 1) }}</view>
          <view>
            <text class="patient-name">{{ guide.patientName || '患者' }}</text>
            <text class="patient-sub">{{ guide.patientGender || '性别未填' }} · {{ guide.patientAge || '年龄未填' }}岁</text>
          </view>
        </view>
        <view class="meta-line"><text>临床诊断</text><b>{{ guide.diagnosis || '未填写' }}</b></view>
        <view class="meta-line"><text>过敏史</text><b>{{ guide.allergyHistory || '未记录' }}</b></view>
        <view class="meta-line"><text>开方医生</text><b>{{ guide.doctorName || '未填写' }}</b></view>
      </view>

      <view class="section-head">
        <view><text class="section-index">01</text><text class="section-title">处方药品与服法</text></view>
        <text class="count">共 {{ guide.medications.length }} 种</text>
      </view>

      <view v-for="(medicine, index) in guide.medications" :key="`${medicine.name}-${index}`" class="medicine-card">
        <view class="medicine-head">
          <view class="medicine-no">{{ index + 1 }}</view>
          <view class="medicine-info">
            <text class="medicine-name">{{ medicine.name }}</text>
            <text class="medicine-spec">{{ medicine.specification }} · {{ medicine.quantity }}{{ medicine.unit }}</text>
          </view>
        </view>

        <view class="usage-box">
          <text class="usage-label">医生用法</text>
          <text class="usage-value">{{ medicine.usage }}</text>
        </view>

        <view class="advice-row time">
          <view class="advice-icon">时</view>
          <view><text class="advice-title">服用时间</text><text class="advice-text">{{ medicine.takingTime }}</text></view>
        </view>
        <view class="advice-row food">
          <view class="advice-icon">忌</view>
          <view><text class="advice-title">饮食禁忌</text><text class="advice-text">{{ medicine.dietRestrictions }}</text></view>
        </view>
        <view class="advice-row warning">
          <view class="advice-icon">!</view>
          <view><text class="advice-title">常见不良反应</text><text class="advice-text">{{ medicine.adverseReactions }}</text></view>
        </view>
        <view class="advice-row note">
          <view class="advice-icon">注</view>
          <view><text class="advice-title">重要注意事项</text><text class="advice-text">{{ medicine.precautions }}</text></view>
        </view>
        <view class="missed"><b>漏服处理</b><text>{{ medicine.missedDose }}</text></view>
      </view>

      <view class="section-head">
        <view><text class="section-index">02</text><text class="section-title">总体健康提醒</text></view>
      </view>
      <view class="summary-card">
        <view v-for="(advice, index) in guide.generalAdvice" :key="advice" class="summary-item">
          <text>{{ index + 1 }}</text><b>{{ advice }}</b>
        </view>
        <view class="follow-up"><text>复诊提醒</text><b>{{ guide.followUpAdvice }}</b></view>
      </view>

      <view class="ai-note">
        <text>AI</text>
        <view><b>智能生成说明</b><p>内容结合患者资料和处方生成，实际用药请以医生处方及药师交代为准。</p></view>
      </view>

      <view class="bottom-actions">
        <button class="secondary" @tap="backToList">重新选择处方</button>
        <button class="print-button" @tap="openPrint">
          生成打印单
          <text>已打印 {{ guide.printCount || 0 }} 次</text>
        </button>
      </view>
    </view>

    <view v-else class="empty-card">
      <view class="empty-icon">!</view>
      <text class="empty-title">没有生成用药指导</text>
      <text class="empty-text">请返回选择一张已发药处方后重试。</text>
      <button class="back-button" @tap="backToList">返回选择处方</button>
    </view>

    <view v-if="printPreview && guide" class="print-mask">
      <view class="print-sheet">
        <view class="print-brand"><text>云脑诊疗</text><b>患者用药指导单</b></view>
        <text class="print-code">处方 RX{{ String(guide.prescriptionId).padStart(6, '0') }}</text>
        <view class="print-patient">{{ guide.patientName }}　{{ guide.patientGender }}　{{ guide.patientAge }}岁</view>
        <view v-for="(medicine, index) in guide.medications" :key="index" class="print-medicine">
          <b>{{ index + 1 }}. {{ medicine.name }}（{{ medicine.specification }}）</b>
          <text>用法：{{ medicine.usage }}</text>
          <text>时间：{{ medicine.takingTime }}</text>
          <text>禁忌：{{ medicine.dietRestrictions }}</text>
          <text>注意：{{ medicine.precautions }}</text>
        </view>
        <text class="print-footer">本说明由 AI 辅助生成，请以医嘱为准。</text>
        <view class="print-actions">
          <button @tap="printPreview = false">返回查看</button>
          <button class="confirm" :loading="printing" @tap="confirmPrint">确认打印</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { generateMedicationGuide, markMedicationGuidePrinted } from '@/api/patient'
import type { MedicationGuide } from '@/types/api'
import { showError } from '@/utils/request'

const guide = ref<MedicationGuide | null>(null)
const loading = ref(false)
const printing = ref(false)
const printPreview = ref(false)

onLoad((query) => {
  const prescriptionId = Number(query?.prescriptionId)
  if (!prescriptionId) {
    uni.showToast({ title: '处方信息不存在', icon: 'none' })
    return
  }
  loadGuide(prescriptionId)
})

async function loadGuide(prescriptionId: number) {
  loading.value = true
  uni.showLoading({ title: 'AI正在生成...' })
  try {
    guide.value = await generateMedicationGuide(prescriptionId)
  } catch (error) {
    showError(error)
  } finally {
    uni.hideLoading()
    loading.value = false
  }
}

function openPrint() {
  printPreview.value = true
}

async function confirmPrint() {
  if (!guide.value) return
  printing.value = true
  try {
    await markMedicationGuidePrinted(guide.value.id)
    guide.value.printCount = (guide.value.printCount || 0) + 1
    printPreview.value = false
    uni.showToast({ title: '打印任务已生成', icon: 'success' })
  } catch (error) {
    showError(error)
  } finally {
    printing.value = false
  }
}

function backToList() {
  uni.navigateBack()
}
</script>

<style scoped>
.page{min-height:100vh;background:#f3f8fa;padding:28rpx 28rpx 70rpx;color:#173a49}.guide,.loading-card,.empty-card{background:#fff;border-radius:30rpx;box-shadow:0 10rpx 30rpx rgba(29,68,83,.07)}.guide{padding:28rpx}.loading-card,.empty-card{padding:56rpx 32rpx;text-align:center}.loading-icon,.empty-icon{width:86rpx;height:86rpx;margin:0 auto 20rpx;border-radius:28rpx;background:#e4f6f2;color:#0a9187;display:flex;align-items:center;justify-content:center;font-size:30rpx;font-weight:850}.loading-title,.loading-text,.empty-title,.empty-text{display:block}.loading-title,.empty-title{font-size:31rpx;font-weight:850}.loading-text,.empty-text{margin-top:12rpx;color:#7d939a;font-size:22rpx;line-height:1.7}.back-button{margin-top:28rpx;background:#0b948a;color:#fff;border-radius:20rpx;font-size:25rpx}
.sheet-head{display:flex;justify-content:space-between;align-items:flex-start;padding-bottom:24rpx;border-bottom:2rpx solid #edf2f3}.sheet-eyebrow,.sheet-title,.sheet-number{display:block}.sheet-eyebrow{font-size:18rpx;color:#0a9188;letter-spacing:2rpx}.sheet-title{font-size:34rpx;font-weight:850;margin:8rpx 0}.sheet-number{font-size:21rpx;color:#8aa0a8}.status-wrap{display:flex;flex-direction:column;align-items:flex-end;gap:9rpx}.ai-live{padding:8rpx 13rpx;background:#e8f2ff;color:#3479a5;border-radius:15rpx;font-size:18rpx;font-weight:700}.ai-live.fallback{background:#fff3df;color:#b7772c}.status{padding:10rpx 16rpx;background:#e4f7f2;color:#088978;border-radius:18rpx;font-size:21rpx}
.patient-card{margin-top:22rpx;background:linear-gradient(145deg,#edf9f7,#f7fbfc);border:2rpx solid #e1efef;border-radius:24rpx;padding:22rpx}.patient-main{display:flex;align-items:center;margin-bottom:18rpx}.patient-avatar{width:72rpx;height:72rpx;border-radius:22rpx;background:#11968e;color:#fff;display:flex;align-items:center;justify-content:center;font-size:30rpx;font-weight:800;margin-right:18rpx}.patient-name,.patient-sub{display:block}.patient-name{font-size:29rpx;font-weight:800}.patient-sub{font-size:22rpx;color:#789098;margin-top:5rpx}.meta-line{display:flex;justify-content:space-between;gap:20rpx;padding:14rpx 0;border-top:2rpx solid rgba(211,230,230,.7);font-size:22rpx}.meta-line text{color:#7f959d}.meta-line b{font-weight:650;text-align:right}
.section-head{display:flex;align-items:center;justify-content:space-between;margin:34rpx 0 18rpx}.section-head>view{display:flex;align-items:center;gap:12rpx}.section-index{font-size:19rpx;color:#0b958d;font-weight:800}.section-title{font-size:29rpx;font-weight:850}.count{font-size:21rpx;color:#8ca1a8}.medicine-card{margin-bottom:20rpx;padding:24rpx;background:#fff;border:2rpx solid #e5edef;border-radius:26rpx;box-shadow:0 8rpx 24rpx rgba(27,66,78,.06)}.medicine-head{display:flex;align-items:center}.medicine-no{width:54rpx;height:54rpx;border-radius:17rpx;background:#0d928a;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;margin-right:16rpx}.medicine-info{flex:1}.medicine-name,.medicine-spec{display:block}.medicine-name{font-size:30rpx;font-weight:850}.medicine-spec{font-size:21rpx;color:#81979f;margin-top:6rpx}.usage-box{margin:20rpx 0;padding:18rpx;background:#eaf7f5;border-radius:18rpx}.usage-label,.usage-value{display:block}.usage-label{font-size:19rpx;color:#0c9086;font-weight:750}.usage-value{font-size:25rpx;font-weight:750;margin-top:7rpx}
.advice-row{display:flex;gap:14rpx;padding:18rpx 0;border-bottom:2rpx solid #f0f3f4}.advice-icon{flex:none;width:44rpx;height:44rpx;border-radius:14rpx;display:flex;align-items:center;justify-content:center;font-size:20rpx;font-weight:850}.advice-row>view:last-child{flex:1}.time .advice-icon{background:#e5f3ff;color:#347fa6}.food .advice-icon{background:#fff0df;color:#bf762b}.warning .advice-icon{background:#ffebe9;color:#d65c55}.note .advice-icon{background:#e8f7ef;color:#278667}.advice-title,.advice-text{display:block}.advice-title{font-size:23rpx;font-weight:800}.advice-text{font-size:22rpx;color:#607980;line-height:1.65;margin-top:5rpx}.missed{margin-top:18rpx;padding:16rpx;background:#f5f8f9;border-radius:16rpx}.missed b,.missed text{display:block}.missed b{font-size:21rpx;color:#4f6870}.missed text{font-size:21rpx;color:#71878e;line-height:1.55;margin-top:5rpx}
.summary-card{padding:22rpx;background:#f2f9f8;border-radius:24rpx}.summary-item{display:flex;gap:14rpx;align-items:flex-start;padding:12rpx 0}.summary-item text{width:34rpx;height:34rpx;flex:none;border-radius:50%;background:#13a08e;color:#fff;text-align:center;line-height:34rpx;font-size:18rpx}.summary-item b{font-size:22rpx;line-height:1.6;font-weight:600}.follow-up{margin-top:12rpx;padding:18rpx;background:#fff;border-radius:18rpx}.follow-up text,.follow-up b{display:block}.follow-up text{font-size:20rpx;color:#d27633;font-weight:800}.follow-up b{font-size:22rpx;line-height:1.6;margin-top:6rpx}.ai-note{display:flex;gap:14rpx;margin-top:20rpx;padding:18rpx;background:#edf8ff;border-radius:20rpx}.ai-note>text{width:48rpx;height:48rpx;flex:none;border-radius:15rpx;background:#3d8db2;color:#fff;display:flex;align-items:center;justify-content:center;font-size:18rpx;font-weight:850}.ai-note b,.ai-note p{display:block;margin:0}.ai-note b{font-size:22rpx}.ai-note p{font-size:20rpx;color:#6d848d;line-height:1.5;margin-top:5rpx}
.bottom-actions{display:flex;gap:16rpx;margin-top:26rpx}.bottom-actions button{height:78rpx;line-height:78rpx;border-radius:22rpx;font-size:24rpx;font-weight:800}.secondary{width:220rpx;background:#edf6f5;color:#087f78}.print-button{flex:1;background:linear-gradient(90deg,#087d82,#10a18f);color:#fff}.print-button text{margin-left:10rpx;font-size:18rpx;opacity:.78}
.print-mask{position:fixed;inset:0;z-index:99;background:rgba(13,35,43,.72);padding:50rpx 26rpx;overflow-y:auto}.print-sheet{background:#fff;border-radius:12rpx;padding:32rpx;color:#222}.print-brand{text-align:center;border-bottom:3rpx solid #222;padding-bottom:18rpx}.print-brand text,.print-brand b{display:block}.print-brand text{font-size:22rpx;letter-spacing:4rpx}.print-brand b{font-size:34rpx;margin-top:8rpx}.print-code{display:block;text-align:center;font-size:20rpx;color:#666;margin:14rpx}.print-patient{padding:14rpx;border-top:2rpx solid #ddd;border-bottom:2rpx solid #ddd;font-size:22rpx}.print-medicine{padding:18rpx 0;border-bottom:2rpx dashed #bbb}.print-medicine b,.print-medicine text{display:block}.print-medicine b{font-size:24rpx}.print-medicine text{font-size:20rpx;line-height:1.55;margin-top:6rpx}.print-footer{display:block;font-size:18rpx;color:#777;text-align:center;margin:22rpx 0}.print-actions{display:flex;gap:16rpx}.print-actions button{flex:1;font-size:23rpx;border-radius:18rpx}.print-actions .confirm{background:#078f87;color:#fff}
</style>
