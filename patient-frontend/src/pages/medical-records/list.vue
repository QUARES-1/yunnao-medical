<template>
  <scroll-view class="page" scroll-y :enhanced="true" :show-scrollbar="false">
    <view class="hero">
      <text class="tag">MEDICAL RECORD</text>
      <text class="title">电子病历</text>
      <text class="sub">查看医生为你书写的门诊病历，包括主诉、诊断结果和治疗建议。</text>
    </view>

    <view class="summary">
      <view>
        <b>{{ total }}</b>
        <text>病历总数</text>
      </view>
      <view>
        <b>{{ records.length }}</b>
        <text>当前显示</text>
      </view>
    </view>

    <view v-for="item in records" :key="item.id" class="record-card">
      <view class="record-head">
        <view class="record-icon">病</view>
        <view class="record-title">
          <text>{{ item.diagnosis || '门诊病历' }}</text>
          <b>{{ formatTime(item.createTime) }}</b>
        </view>
        <text class="status">已归档</text>
      </view>

      <view class="info-grid">
        <view>
          <text>接诊医生</text>
          <b>{{ item.doctorName || '医生' }}</b>
        </view>
        <view>
          <text>病历编号</text>
          <b>MR{{ String(item.id).padStart(6, '0') }}</b>
        </view>
      </view>

      <view class="block" v-if="item.chiefComplaint">
        <text>主诉</text>
        <b>{{ item.chiefComplaint }}</b>
      </view>
      <view class="block" v-if="item.presentIllness">
        <text>现病史</text>
        <b>{{ item.presentIllness }}</b>
      </view>
      <view class="block" v-if="item.physicalExamination">
        <text>体格检查</text>
        <b>{{ item.physicalExamination }}</b>
      </view>
      <view class="block" v-if="item.treatment">
        <text>治疗建议</text>
        <b>{{ item.treatment }}</b>
      </view>
    </view>

    <view v-if="!loading && !records.length" class="empty">
      <view class="empty-icon">病</view>
      <text class="empty-title">暂无电子病历</text>
      <text>医生完成看诊并保存病历后，这里会显示你的电子病历内容。</text>
    </view>

    <button v-if="hasMore" class="load-more" :loading="loading" @tap="loadMore">加载更多</button>
    <view class="bottom-space"></view>
  </scroll-view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMedicalRecords } from '@/api/patient'
import type { MedicalRecord } from '@/types/api'
import { showError } from '@/utils/request'

const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const hasMore = ref(false)
const records = ref<MedicalRecord[]>([])

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function loadData(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      records.value = []
    }
    const res = await getMedicalRecords({ page: page.value, size })
    const list = res.content || []
    records.value = reset ? list : records.value.concat(list)
    total.value = res.totalElements || records.value.length
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
.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#078285,#24a79b);color:#fff;box-shadow:0 18rpx 36rpx rgba(22,141,134,.18)}
.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:42rpx;font-weight:900;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7}
.summary{display:grid;grid-template-columns:repeat(2,1fr);gap:0;margin:22rpx 0;padding:24rpx 0;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.summary view{position:relative;text-align:center}.summary view+view::before{content:'';position:absolute;left:0;top:10rpx;bottom:10rpx;width:1rpx;background:#e9f1f2}.summary b,.summary text{display:block}.summary b{font-size:38rpx;line-height:1;color:#0c8f87;font-weight:900}.summary text{margin-top:10rpx;font-size:20rpx;color:#8ca0a6;font-weight:700}
.record-card{margin-top:18rpx;padding:26rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}
.record-head{display:flex;align-items:center;gap:16rpx}.record-icon{width:62rpx;height:62rpx;border-radius:20rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.record-title{flex:1;min-width:0}.record-title text,.record-title b{display:block}.record-title text{font-size:30rpx;font-weight:900}.record-title b{margin-top:5rpx;font-size:21rpx;color:#8ba0a6}.status{padding:8rpx 14rpx;border-radius:999rpx;background:#e9f8f4;color:#0c8f87;font-size:20rpx;font-weight:800}
.info-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12rpx;margin:22rpx 0}.info-grid view{padding:18rpx;border-radius:18rpx;background:#f7fbfc}.info-grid text,.info-grid b{display:block}.info-grid text{font-size:20rpx;color:#8ba0a6}.info-grid b{margin-top:6rpx;font-size:24rpx;color:#274c5b}
.block{padding:20rpx 0;border-top:1rpx solid #edf2f3}.block text,.block b{display:block}.block text{font-size:21rpx;color:#7e939a;font-weight:800}.block b{margin-top:8rpx;font-size:24rpx;color:#284a58;line-height:1.7;font-weight:500}
.empty{margin-top:28rpx;padding:48rpx 28rpx;border-radius:28rpx;background:#fff;text-align:center;color:#8ca0a6}.empty-icon{width:80rpx;height:80rpx;margin:0 auto 18rpx;border-radius:26rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.empty-title{display:block;margin-bottom:8rpx;font-size:30rpx;font-weight:900;color:#244655}.empty text:last-child{display:block;font-size:23rpx;line-height:1.7}
.load-more{margin:24rpx 0 0;background:#0c9289;color:#fff;border-radius:20rpx;font-weight:850}.bottom-space{height:40rpx}
</style>
