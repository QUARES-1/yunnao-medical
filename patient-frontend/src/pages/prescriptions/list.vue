<template>
  <scroll-view class="page" scroll-y :enhanced="true" :show-scrollbar="false">
    <view class="hero">
      <text class="tag">PRESCRIPTIONS</text>
      <text class="title">我的处方</text>
      <text class="sub">查看医生开具的处方记录、药品明细、金额和发药状态。</text>
    </view>

    <view class="summary">
      <view>
        <b>{{ total }}</b>
        <text>处方总数</text>
      </view>
      <view>
        <b>{{ dispensedCount }}</b>
        <text>已发药</text>
      </view>
      <view>
        <b>{{ pendingCount }}</b>
        <text>待发药</text>
      </view>
    </view>

    <view v-for="item in prescriptions" :key="item.id" class="prescription-card">
      <view class="prescription-head">
        <view class="rx-icon">Rx</view>
        <view class="prescription-title">
          <text>处方 RX{{ String(item.id).padStart(6, '0') }}</text>
          <b>{{ item.doctorName || '医生' }} · {{ formatTime(item.createTime) }}</b>
        </view>
        <text :class="['status', item.status === '已发药' ? 'done' : 'pending']">{{ item.status || '待发药' }}</text>
      </view>

      <view class="drug-list">
        <view v-for="(drug, index) in parseDrugs(item.drugs)" :key="index" class="drug-row">
          <view>
            <text>{{ drug.name || '药品' }}</text>
            <b>{{ drug.specification || '规格未填写' }}</b>
          </view>
          <em>x{{ drug.quantity || 1 }}{{ drug.unit || '' }}</em>
        </view>
        <view v-if="!parseDrugs(item.drugs).length" class="drug-empty">暂无药品明细</view>
      </view>

      <view class="total-row">
        <text>合计金额</text>
        <b>￥{{ money(item.totalAmount) }}</b>
      </view>
    </view>

    <view v-if="!loading && !prescriptions.length" class="empty">
      <view class="empty-icon">Rx</view>
      <text class="empty-title">暂无处方记录</text>
      <text>医生开具处方后，这里会显示药品明细和发药状态。</text>
    </view>

    <button v-if="hasMore" class="load-more" :loading="loading" @tap="loadMore">加载更多</button>
    <view class="bottom-space"></view>
  </scroll-view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { usePrescriptionsStore } from '@/stores/prescriptions'
import type { PrescriptionDrug } from '@/types/api'
import { showError } from '@/utils/request'

const prescriptionStore = usePrescriptionsStore()
const { records: prescriptions, loading, total, hasMore, dispensedCount, pendingCount } = storeToRefs(prescriptionStore)

function parseDrugs(value?: string): PrescriptionDrug[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}
function money(value?: number) { return Number(value || 0).toFixed(2) }
function formatTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
async function loadMore() {
  try { await prescriptionStore.loadMore() } catch (error) { showError(error) }
}
onMounted(async () => {
  try { await prescriptionStore.load(true) } catch (error) { showError(error) }
})
</script>

<style scoped>
.page{height:100vh;box-sizing:border-box;padding:28rpx;background:#f4f8fb;color:#18394b}
.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#0b7777,#1fae96);color:#fff;box-shadow:0 18rpx 36rpx rgba(22,141,134,.18)}
.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:42rpx;font-weight:900;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7}
.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:0;margin:22rpx 0;padding:24rpx 0;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.summary view{position:relative;text-align:center}.summary view+view::before{content:'';position:absolute;left:0;top:10rpx;bottom:10rpx;width:1rpx;background:#e9f1f2}.summary b,.summary text{display:block}.summary b{font-size:38rpx;line-height:1;color:#0c8f87;font-weight:900}.summary text{margin-top:10rpx;font-size:20rpx;color:#8ca0a6;font-weight:700}
.prescription-card{margin-top:18rpx;padding:26rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}
.prescription-head{display:flex;align-items:center;gap:16rpx}.rx-icon{width:62rpx;height:62rpx;border-radius:20rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.prescription-title{flex:1;min-width:0}.prescription-title text,.prescription-title b{display:block}.prescription-title text{font-size:30rpx;font-weight:900}.prescription-title b{margin-top:5rpx;font-size:21rpx;color:#8ba0a6}.status{padding:8rpx 14rpx;border-radius:999rpx;font-size:20rpx;font-weight:800}.status.done{background:#e9f8f4;color:#0c8f87}.status.pending{background:#fff3df;color:#c97918}
.drug-list{margin-top:22rpx;border-top:1rpx solid #edf2f3}.drug-row{display:flex;justify-content:space-between;gap:16rpx;padding:18rpx 0;border-bottom:1rpx solid #edf2f3}.drug-row text,.drug-row b{display:block}.drug-row text{font-size:25rpx;font-weight:850;color:#244655}.drug-row b{margin-top:6rpx;font-size:21rpx;color:#8ba0a6}.drug-row em{font-style:normal;font-size:24rpx;color:#0c8f87;font-weight:850}.drug-empty{padding:22rpx;text-align:center;color:#91a3aa;font-size:22rpx}
.total-row{display:flex;justify-content:space-between;align-items:center;padding-top:20rpx}.total-row text{font-size:24rpx;color:#7e939a}.total-row b{font-size:34rpx;color:#e2972c}
.empty{margin-top:28rpx;padding:48rpx 28rpx;border-radius:28rpx;background:#fff;text-align:center;color:#8ca0a6}.empty-icon{width:80rpx;height:80rpx;margin:0 auto 18rpx;border-radius:26rpx;background:#e6f6f3;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.empty-title{display:block;margin-bottom:8rpx;font-size:30rpx;font-weight:900;color:#244655}.empty text:last-child{display:block;font-size:23rpx;line-height:1.7}
.load-more{margin:24rpx 0 0;background:#0c9289;color:#fff;border-radius:20rpx;font-weight:850}.bottom-space{height:40rpx}
</style>

