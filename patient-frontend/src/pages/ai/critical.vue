<template>
  <view class="page">
    <view class="hero">
      <text class="tag">CRITICAL VALUE WARNING</text>
      <text class="title">AI危急值智能预警</text>
      <text class="sub">AI 实时监控检验结果，识别高风险指标并联动医生与检验科。</text>
    </view>

    <view class="overview">
      <view><text class="num">{{ warnings.length }}</text><text>预警记录</text></view>
      <view><text class="num danger">{{ pendingCount }}</text><text>待完成处置</text></view>
      <view><text class="num safe">{{ processedCount }}</text><text>已完成处理</text></view>
    </view>

    <view v-if="loading" class="state">正在加载危急值记录...</view>
    <view v-for="item in warnings" :key="item.id" class="warning-item">
      <view class="item-head">
        <view class="alarm">!</view>
        <view class="head-main">
          <text class="warning-title">{{ criticalTitle(item.criticalItems) }}</text>
          <text class="warning-time">{{ formatTime(item.createTime) }}</text>
        </view>
        <text class="status" :class="item.status">{{ statusText(item.status) }}</text>
      </view>
      <view class="info-row"><text>报告编号</text><text>EX{{ String(item.examinationId).padStart(6, '0') }}</text></view>
      <view class="critical-detail">{{ item.criticalItems || '检验结果达到危急值范围，请尽快联系医生。' }}</view>
      <view class="info-row"><text>开单医生</text><text>{{ doctorDisplayName(item.doctorName) }}</text></view>
      <view class="info-row"><text>检验科处置</text><text class="urgent">{{ item.labRemark || '已标记加急复核' }}</text></view>
      <view v-if="item.doctorRemark" class="doctor-note">
        <text>医生处理意见</text><text>{{ item.doctorRemark }}</text>
      </view>
      <button class="report-btn" @tap="goReport(item.examinationId)">查看对应检验报告</button>
    </view>

    <view v-if="!loading && !warnings.length" class="empty">
      <view class="empty-icon">✓</view>
      <text class="empty-title">暂未发现危急值</text>
      <text class="empty-text">系统会持续监控您的检验结果，出现高风险指标时将在这里提醒。</text>
    </view>

    <view class="tips">
      <text>紧急提醒</text>
      <text>若出现胸痛、呼吸困难、意识不清、大量出血等情况，请立即拨打急救电话或前往急诊。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { getPatientCriticalWarnings } from '@/api/patient'
import type { CriticalValueWarning } from '@/types/api'
import { showError } from '@/utils/request'

const warnings = ref<CriticalValueWarning[]>([])
const loading = ref(false)
const pendingCount = computed(() => warnings.value.filter(x => x.status !== 'processed').length)
const processedCount = computed(() => warnings.value.filter(x => x.status === 'processed').length)

onLoad(loadData)
onPullDownRefresh(async () => { await loadData(); uni.stopPullDownRefresh() })

async function loadData() {
  loading.value = true
  try {
    const page = await getPatientCriticalWarnings({ page: 1, size: 30 })
    warnings.value = page.content || []
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}
function statusText(status?: string) {
  return ({ pending: '待医生确认', confirmed: '医生已确认', processed: '已处理' } as Record<string, string>)[status || ''] || '预警中'
}
function criticalTitle(value?: string) {
  const text = value?.trim()
  return text ? text.split(/[，,；;]/)[0] : '发现危急指标'
}
function doctorDisplayName(value?: string) {
  const name = value?.trim().replace(/医生$/, '')
  return name ? `${name}医生` : '开单医生信息待同步'
}
function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}
function goReport(id: number) {
  uni.navigateTo({ url: `/pages/ai/report-detail?examinationId=${id}&reportName=${encodeURIComponent('危急值检验')}` })
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f8fb;padding:28rpx;color:#18394b}.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#8b2732,#ef6b5b);color:#fff;box-shadow:0 16rpx 36rpx rgba(177,59,54,.18)}.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:41rpx;font-weight:850;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7;opacity:.9}.overview{display:grid;grid-template-columns:repeat(3,1fr);gap:12rpx;margin-top:22rpx}.overview>view{padding:22rpx 8rpx;border-radius:22rpx;background:#fff;text-align:center;box-shadow:0 8rpx 24rpx rgba(29,68,83,.06)}.overview text{display:block;font-size:19rpx;color:#85979e}.overview .num{margin-bottom:5rpx;font-size:33rpx;font-weight:850;color:#244a58}.overview .danger{color:#e4564c}.overview .safe{color:#168e77}.warning-item,.empty,.state,.tips{margin-top:22rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.warning-item{padding:25rpx;border-left:7rpx solid #ef6559}.item-head{display:flex;align-items:flex-start;gap:15rpx}.alarm{width:52rpx;height:52rpx;flex:none;border-radius:17rpx;background:#ffebe8;color:#e34e43;display:flex;align-items:center;justify-content:center;font-size:28rpx;font-weight:900}.head-main{flex:1;min-width:0}.warning-title,.warning-time{display:block}.warning-title{font-size:25rpx;font-weight:850;line-height:1.55}.warning-time{margin-top:6rpx;color:#93a3a8;font-size:19rpx}.status{flex:none;padding:7rpx 12rpx;border-radius:14rpx;background:#fff0ed;color:#dc5147;font-size:18rpx}.status.confirmed{background:#fff4dd;color:#bd7819}.status.processed{background:#e5f7f1;color:#168567}.info-row{display:flex;justify-content:space-between;gap:25rpx;padding:18rpx 0;border-bottom:1rpx solid #edf2f3;font-size:22rpx}.info-row text:first-child{color:#83969d}.info-row text:last-child{text-align:right}.urgent{color:#df5448!important}.doctor-note{margin-top:17rpx;padding:18rpx;border-radius:18rpx;background:#eef8ff}.doctor-note text{display:block}.doctor-note text:first-child{font-size:20rpx;font-weight:800;color:#287ca2}.doctor-note text:last-child{margin-top:7rpx;font-size:22rpx;line-height:1.6}.report-btn{margin-top:18rpx;background:#f0f8f9;color:#087f84;border-radius:18rpx;font-size:23rpx;font-weight:800}.empty,.state{padding:45rpx 25rpx;text-align:center;color:#84979e}.empty-icon{width:78rpx;height:78rpx;margin:auto;border-radius:26rpx;background:#e5f7f1;color:#168567;display:flex;align-items:center;justify-content:center;font-size:38rpx}.empty-title,.empty-text{display:block}.empty-title{margin-top:14rpx;font-size:29rpx;font-weight:850;color:#284a57}.empty-text{margin-top:9rpx;font-size:22rpx;line-height:1.7}.tips{padding:24rpx;background:#fff8ee}.tips text{display:block}.tips text:first-child{font-size:26rpx;font-weight:850;color:#a96a13}.tips text:last-child{margin-top:8rpx;font-size:22rpx;color:#836741;line-height:1.7}
.critical-detail{margin:18rpx 0 2rpx;padding:18rpx 20rpx;border-radius:18rpx;background:#fff3f0;color:#b7433b;font-size:22rpx;font-weight:700;line-height:1.65}
</style>
