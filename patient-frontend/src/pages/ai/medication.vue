<template>
  <view class="page">
    <view class="hero">
      <text class="tag">MEDICATION GUIDE</text>
      <text class="title">AI个性化用药指导</text>
      <text class="sub">先选择一张已发药处方，再进入专属说明单查看服用时间、饮食禁忌和风险提醒。</text>
    </view>

    <view class="card prescription-picker">
      <view class="picker-head">
        <view>
          <text class="picker-tag">MY PRESCRIPTIONS</text>
          <text class="picker-title">选择我的处方</text>
        </view>
        <text class="picker-count">{{ prescriptions.length }} 张</text>
      </view>
      <text class="picker-tip">点击已发药处方后，会跳转到单独的“个性化用药说明单”页面，界面更清楚。</text>

      <view v-if="listLoading" class="list-state">正在加载我的处方...</view>
      <view
        v-for="item in prescriptions"
        :key="item.id"
        class="prescription-item"
        :class="{ disabled: item.status !== '已发药' }"
        @tap="openGuide(item)"
      >
        <view class="rx-icon">Rx</view>
        <view class="rx-main">
          <view class="rx-top">
            <text class="rx-doctor">{{ item.doctorName || '医生处方' }}</text>
            <text class="rx-status" :class="{ ready: item.status === '已发药' }">{{ item.status || '待处理' }}</text>
          </view>
          <text class="rx-drugs">{{ prescriptionMedicineNames(item) }}</text>
          <text class="rx-meta">{{ formatDate(item.createTime) }} · RX{{ String(item.id).padStart(6, '0') }}</text>
        </view>
        <text class="rx-arrow">{{ item.status === '已发药' ? '查看说明' : '待发药' }}</text>
      </view>

      <view v-if="!listLoading && !prescriptions.length" class="list-state">
        暂无处方记录，请完成就诊并由医生开具处方后查看。
      </view>
    </view>

    <view class="hint-card">
      <view class="hint-icon">AI</view>
      <view>
        <text class="hint-title">为什么只能选择已发药处方？</text>
        <text class="hint-text">用药指导会结合药房最终发出的药品生成，避免医生刚开方但药房还未确认时信息不准确。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatientPrescriptions } from '@/api/patient'
import type { Prescription, PrescriptionDrug } from '@/types/api'
import { showError } from '@/utils/request'

const prescriptions = ref<Prescription[]>([])
const listLoading = ref(false)

onLoad(loadPrescriptions)

async function loadPrescriptions() {
  if (!uni.getStorageSync('patient_token')) {
    return uni.showToast({ title: '请先完成微信授权登录', icon: 'none' })
  }
  listLoading.value = true
  try {
    const page = await getPatientPrescriptions({ page: 1, size: 20 })
    prescriptions.value = page.content || []
  } catch (error) {
    showError(error)
  } finally {
    listLoading.value = false
  }
}

function openGuide(item: Prescription) {
  if (item.status !== '已发药') {
    return uni.showToast({ title: '药房发药后即可生成用药指导', icon: 'none' })
  }
  uni.navigateTo({ url: `/pages/ai/medication-detail?prescriptionId=${item.id}` })
}

function prescriptionMedicineNames(item: Prescription) {
  try {
    const drugs = JSON.parse(item.drugs || '[]') as PrescriptionDrug[]
    return drugs.map(drug => drug.name).filter(Boolean).join('、') || '药品明细待完善'
  } catch {
    return item.drugs || '药品明细待完善'
  }
}

function formatDate(value?: string) {
  if (!value) return '开方日期待完善'
  return value.replace('T', ' ').slice(0, 10)
}
</script>

<style scoped>
.page{min-height:100vh;background:#f3f8fa;padding:28rpx 28rpx 70rpx;color:#173a49}
.hero{padding:36rpx;border-radius:34rpx;background:linear-gradient(135deg,#086b77,#10a18f);color:#fff;box-shadow:0 16rpx 40rpx rgba(8,111,118,.2)}
.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.72}.title{font-size:40rpx;font-weight:800;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7;opacity:.88}
.card,.hint-card{margin-top:22rpx;background:#fff;border-radius:30rpx;box-shadow:0 10rpx 30rpx rgba(29,68,83,.07)}
.prescription-picker{padding:26rpx}.picker-head{display:flex;align-items:center;justify-content:space-between}.picker-head>view{display:flex;flex-direction:column}.picker-tag{font-size:18rpx;letter-spacing:2rpx;color:#0a948a}.picker-title{font-size:31rpx;font-weight:850;margin-top:6rpx}.picker-count{padding:8rpx 14rpx;border-radius:16rpx;background:#e9f7f4;color:#098b80;font-size:20rpx}.picker-tip{display:block;margin:14rpx 0 20rpx;color:#7b929a;font-size:21rpx;line-height:1.6}
.prescription-item{display:flex;align-items:center;gap:15rpx;margin-top:14rpx;padding:20rpx;border:2rpx solid #e6eef0;border-radius:22rpx;background:#fbfdfd}.prescription-item:active{transform:scale(.99);background:#effaf8}.prescription-item.disabled{opacity:.62}.rx-icon{width:58rpx;height:58rpx;flex:none;border-radius:18rpx;background:#0b9188;color:#fff;display:flex;align-items:center;justify-content:center;font-size:21rpx;font-weight:850}.rx-main{flex:1;min-width:0}.rx-top{display:flex;align-items:center;justify-content:space-between;gap:12rpx}.rx-doctor{font-size:25rpx;font-weight:800}.rx-status{padding:5rpx 10rpx;border-radius:12rpx;background:#f3f0ed;color:#947c6c;font-size:18rpx}.rx-status.ready{background:#e3f7ef;color:#168363}.rx-drugs{display:block;margin-top:8rpx;font-size:22rpx;color:#4f6971;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.rx-meta{display:block;margin-top:7rpx;font-size:19rpx;color:#94a5aa}.rx-arrow{flex:none;color:#0a9188;font-size:21rpx;font-weight:750}.prescription-item.disabled .rx-arrow{font-size:18rpx;color:#899da4}.list-state{padding:28rpx 12rpx;text-align:center;color:#82979e;font-size:22rpx;line-height:1.6}
.hint-card{display:flex;gap:18rpx;padding:24rpx;background:linear-gradient(145deg,#ffffff,#eefaf8)}.hint-icon{width:58rpx;height:58rpx;flex:none;border-radius:18rpx;background:#0b9188;color:#fff;display:flex;align-items:center;justify-content:center;font-size:21rpx;font-weight:850}.hint-title,.hint-text{display:block}.hint-title{font-size:25rpx;font-weight:850}.hint-text{margin-top:8rpx;color:#7a9299;font-size:21rpx;line-height:1.65}
</style>
