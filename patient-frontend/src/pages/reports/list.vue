<template>
  <scroll-view class="page" scroll-y :enhanced="true" :show-scrollbar="false">
    <view class="hero">
      <text class="tag">REPORTS</text>
      <text class="title">检查报告</text>
      <text class="sub">查看医生开立的检查检验记录、报告状态和检查结果。</text>
    </view>

    <view class="summary">
      <view>
        <b>{{ total }}</b>
        <text>报告总数</text>
      </view>
      <view>
        <b>{{ completedCount }}</b>
        <text>已完成</text>
      </view>
      <view>
        <b>{{ pendingCount }}</b>
        <text>待检查</text>
      </view>
    </view>

    <view v-for="item in reports" :key="item.id" class="report-card" @tap="goDetail(item)">
      <view class="report-head">
        <view class="report-icon">检</view>
        <view class="report-title">
          <text>{{ cleanName(item.itemName) }}</text>
          <b>{{ item.type || '检查检验' }} · {{ formatTime(item.completeTime || item.createTime) }}</b>
        </view>
        <text :class="['status', item.status === '已完成' ? 'done' : 'pending']">{{ item.status || '待检查' }}</text>
      </view>

      <view class="info-grid">
        <view>
          <text>开单医生</text>
          <b>{{ item.doctorName || '医生' }}</b>
        </view>
        <view>
          <text>报告编号</text>
          <b>EX{{ String(item.id).padStart(6, '0') }}</b>
        </view>
      </view>

      <view class="result-summary">
        <text>{{ item.result ? resultSummary(item.result) : '检验科录入结果后可查看完整报告。' }}</text>
        <b>{{ item.result ? '查看详情 ›' : '等待报告 ›' }}</b>
      </view>
    </view>

    <view v-if="!loading && !reports.length" class="empty">
      <view class="empty-icon">检</view>
      <text class="empty-title">暂无检查报告</text>
      <text>医生开立检查并由检验科录入结果后，报告会显示在这里。</text>
    </view>

    <button v-if="hasMore" class="load-more" :loading="loading" @tap="loadMore">加载更多</button>
    <view class="bottom-space"></view>
  </scroll-view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getPatientExaminations } from '@/api/patient'
import type { Examination } from '@/types/api'
import { showError } from '@/utils/request'

const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const hasMore = ref(false)
const reports = ref<Examination[]>([])

const completedCount = computed(() => reports.value.filter(x => x.status === '已完成').length)
const pendingCount = computed(() => reports.value.filter(x => x.status !== '已完成').length)

function cleanName(value?: string) {
  return (value || '检查报告').replace(/^AI演示·/, '')
}

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function resultSummary(result: string) {
  return result.split(/[；;]/).filter(Boolean).slice(0, 1).join('；') || '已有检查结果'
}

function goDetail(item: Examination) {
  uni.navigateTo({ url: `/pages/reports/detail?id=${item.id}` })
}

async function loadData(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      reports.value = []
    }
    const res = await getPatientExaminations({ page: page.value, size })
    const list = res.content || []
    reports.value = reset ? list : reports.value.concat(list)
    total.value = res.totalElements || reports.value.length
    hasMore.value = page.value < (res.totalPages || 1)
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadData()
}

onMounted(() => loadData(true))
</script>

<style scoped>
.page{height:100vh;box-sizing:border-box;padding:28rpx;background:#f4f8fb;color:#18394b}
.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#138a9b,#37b3a4);color:#fff;box-shadow:0 18rpx 36rpx rgba(22,141,134,.18)}
.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:42rpx;font-weight:900;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7}
.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:0;margin:22rpx 0;padding:24rpx 0;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.summary view{position:relative;text-align:center}.summary view+view::before{content:'';position:absolute;left:0;top:10rpx;bottom:10rpx;width:1rpx;background:#e9f1f2}.summary b,.summary text{display:block}.summary b{font-size:38rpx;line-height:1;color:#0c8f87;font-weight:900}.summary text{margin-top:10rpx;font-size:20rpx;color:#8ca0a6;font-weight:700}
.report-card{margin-top:18rpx;padding:26rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}
.report-head{display:flex;align-items:center;gap:16rpx}.report-icon{width:62rpx;height:62rpx;border-radius:20rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.report-title{flex:1;min-width:0}.report-title text,.report-title b{display:block}.report-title text{font-size:30rpx;font-weight:900}.report-title b{margin-top:5rpx;font-size:21rpx;color:#8ba0a6}.status{padding:8rpx 14rpx;border-radius:999rpx;font-size:20rpx;font-weight:800}.status.done{background:#e9f8f4;color:#0c8f87}.status.pending{background:#fff3df;color:#c97918}
.info-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12rpx;margin:22rpx 0}.info-grid view{padding:18rpx;border-radius:18rpx;background:#f7fbfc}.info-grid text,.info-grid b{display:block}.info-grid text{font-size:20rpx;color:#8ba0a6}.info-grid b{margin-top:6rpx;font-size:24rpx;color:#274c5b}
.result-summary{display:flex;justify-content:space-between;gap:16rpx;align-items:center;padding-top:20rpx;border-top:1rpx solid #edf2f3}.result-summary text{flex:1;min-width:0;font-size:22rpx;color:#7e939a;line-height:1.5;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.result-summary b{flex:none;font-size:22rpx;color:#0c8f87}
.empty{margin-top:28rpx;padding:48rpx 28rpx;border-radius:28rpx;background:#fff;text-align:center;color:#8ca0a6}.empty-icon{width:80rpx;height:80rpx;margin:0 auto 18rpx;border-radius:26rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.empty-title{display:block;margin-bottom:8rpx;font-size:30rpx;font-weight:900;color:#244655}.empty text:last-child{display:block;font-size:23rpx;line-height:1.7}
.load-more{margin:24rpx 0 0;background:#0c9289;color:#fff;border-radius:20rpx;font-weight:850}.bottom-space{height:40rpx}
</style>
