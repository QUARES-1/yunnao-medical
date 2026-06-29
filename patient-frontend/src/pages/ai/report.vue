<template>
  <view class="page">
    <view class="hero">
      <text class="tag">REPORT INTERPRETATION</text>
      <text class="title">AI检验报告自动解读</text>
      <text class="sub">选择一份已完成的检查/检验报告，AI 会标注异常指标、解释含义并给出复查建议。</text>
    </view>

    <view class="card report-picker">
      <view class="picker-head">
        <view>
          <text class="picker-tag">MY REPORTS</text>
          <text class="picker-title">选择我的报告</text>
        </view>
        <text class="picker-count">{{ reports.length }} 份</text>
      </view>
      <text class="picker-tip">点击“已完成”的报告进入 AI 解读详情；未完成的检查需要检验科录入结果后才能解读。</text>

      <view v-if="loading" class="list-state">正在加载我的检查报告...</view>
      <view
        v-for="item in reports"
        :key="item.id"
        class="report-item"
        :class="{ disabled: item.status !== '已完成' }"
        @tap="openInterpretation(item)"
      >
        <view class="report-icon">{{ item.type === '检验' ? '验' : '检' }}</view>
        <view class="report-main">
          <view class="report-top">
            <text class="report-name">{{ cleanReportName(item.itemName) }}</text>
            <text class="report-status" :class="{ done: item.status === '已完成' }">{{ item.status || '待检查' }}</text>
          </view>
          <text class="report-meta">{{ item.type || '检查检验' }} · {{ item.doctorName || '开单医生' }}</text>
          <text class="report-time">{{ formatTime(item.completeTime || item.createTime) }} · EX{{ String(item.id).padStart(6, '0') }}</text>
        </view>
        <text class="report-arrow">{{ item.status === '已完成' ? 'AI解读' : '待出报告' }}</text>
      </view>

      <view v-if="!loading && !reports.length" class="list-state">
        暂无检查检验记录。医生开立检查、检验科录入结果后，这里会显示您的报告。
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatientExaminations } from '@/api/patient'
import type { Examination } from '@/types/api'
import { showError } from '@/utils/request'

const reports = ref<Examination[]>([])
const loading = ref(false)

onLoad(loadReports)

async function loadReports() {
  if (!uni.getStorageSync('patient_token')) {
    return uni.showToast({ title: '请先完成微信授权登录', icon: 'none' })
  }
  loading.value = true
  try {
    const page = await getPatientExaminations({ page: 1, size: 30 })
    reports.value = page.content || []
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function openInterpretation(item: Examination) {
  if (item.status !== '已完成') {
    return uni.showToast({ title: '报告完成后才能 AI 解读', icon: 'none' })
  }
  const reportName = encodeURIComponent(cleanReportName(item.itemName))
  uni.navigateTo({ url: `/pages/ai/report-detail?examinationId=${item.id}&reportName=${reportName}` })
}

function cleanReportName(name?: string) {
  return (name || '检查项目').replace(/^AI演示[·・\s-]*/, '')
}

function formatTime(value?: string) {
  if (!value) return '时间待完善'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f8fb;padding:28rpx;color:#18394b}.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#075c71,#2796cc);color:#fff;box-shadow:0 16rpx 40rpx rgba(39,150,204,.18)}.tag,.title,.sub{display:block}.tag{font-size:20rpx;letter-spacing:3rpx;opacity:.72}.title{font-size:42rpx;font-weight:800;margin:12rpx 0}.sub{font-size:25rpx;line-height:1.7;opacity:.88}.card{margin-top:24rpx;background:#fff;border-radius:30rpx;box-shadow:0 10rpx 30rpx rgba(29,68,83,.08)}.report-picker{padding:26rpx}.picker-head{display:flex;align-items:center;justify-content:space-between}.picker-head>view{display:flex;flex-direction:column}.picker-tag{font-size:18rpx;letter-spacing:2rpx;color:#1888a5}.picker-title{font-size:31rpx;font-weight:850;margin-top:6rpx}.picker-count{padding:8rpx 14rpx;border-radius:16rpx;background:#e8f6fb;color:#197fa2;font-size:20rpx}.picker-tip{display:block;margin:14rpx 0 20rpx;color:#7b929a;font-size:21rpx;line-height:1.6}.report-item{display:flex;align-items:center;gap:15rpx;margin-top:14rpx;padding:20rpx;border:2rpx solid #e4eef2;border-radius:22rpx;background:#fbfdfd}.report-item:active{transform:scale(.99);background:#eef9fc}.report-item.disabled{opacity:.58}.report-icon{width:58rpx;height:58rpx;flex:none;border-radius:18rpx;background:#1689a9;color:#fff;display:flex;align-items:center;justify-content:center;font-size:24rpx;font-weight:850}.report-main{flex:1;min-width:0}.report-top{display:flex;align-items:center;justify-content:space-between;gap:12rpx}.report-name{font-size:26rpx;font-weight:850;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.report-status{padding:5rpx 10rpx;border-radius:12rpx;background:#f1f0ed;color:#947c6c;font-size:18rpx}.report-status.done{background:#e2f7f3;color:#11826f}.report-meta,.report-time{display:block}.report-meta{margin-top:8rpx;font-size:22rpx;color:#4f6971}.report-time{margin-top:7rpx;font-size:19rpx;color:#94a5aa}.report-arrow{flex:none;color:#1689a9;font-size:21rpx;font-weight:750}.report-item.disabled .report-arrow{font-size:18rpx;color:#899da4}.list-state{padding:30rpx 12rpx;text-align:center;color:#82979e;font-size:22rpx;line-height:1.6}
</style>
