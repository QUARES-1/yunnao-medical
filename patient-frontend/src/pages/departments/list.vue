<template>
  <view class="page">
    <view class="intro">
      <text class="intro-kicker">DEPARTMENT GUIDE</text>
      <text class="intro-title">选择就诊科室</text>
      <text class="intro-copy">按症状选择对应科室，查看该科室全部医生</text>
    </view>

    <view class="search-box">
      <text class="search-icon">⌕</text>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索科室名称"
        placeholder-class="search-placeholder"
      />
      <text v-if="keyword" class="clear" @tap="keyword = ''">×</text>
    </view>

    <view class="summary">共 {{ filteredDepartments.length }} 个科室</view>

    <view class="department-grid">
      <view
        v-for="(department, index) in filteredDepartments"
        :key="department.id"
        class="department-card"
        @tap="openDepartment(department.id)"
      >
        <view class="icon" :class="`tone-${index % 4}`">{{ department.name.slice(0, 1) }}</view>
        <view class="department-content">
          <text class="department-name">{{ department.name }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="!loading && !filteredDepartments.length" class="empty">
      <view class="empty-icon">⌕</view>
      <text class="empty-title">没有找到相关科室</text>
      <text class="empty-copy">请尝试输入其他科室名称</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getDepartments } from '@/api/patient'
import { showError } from '@/utils/request'
import type { Department } from '@/types/api'

const departments = ref<Department[]>([])
const keyword = ref('')
const loading = ref(true)

const filteredDepartments = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return departments.value
  return departments.value.filter((department) =>
    department.name.toLowerCase().includes(value)
  )
})

const openDepartment = (id: number) => {
  uni.navigateTo({ url: `/pages/doctors/list?departmentId=${id}` })
}

onLoad(async () => {
  try {
    departments.value = await getDepartments()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx 30rpx 50rpx;background:#f4f8fb}.intro{position:relative;overflow:hidden;padding:34rpx;border-radius:30rpx;background:linear-gradient(140deg,#075f72,#15958e);color:#fff;box-shadow:0 14rpx 34rpx rgba(8,127,132,.15)}.intro:after{content:'';position:absolute;width:220rpx;height:220rpx;right:-70rpx;top:-90rpx;border:1rpx solid rgba(255,255,255,.16);border-radius:50%;box-shadow:0 0 0 42rpx rgba(255,255,255,.04)}.intro text{display:block;position:relative;z-index:1}.intro-kicker{font-size:17rpx;letter-spacing:3rpx;opacity:.65}.intro-title{margin-top:12rpx;font-size:38rpx;font-weight:750}.intro-copy{margin-top:12rpx;font-size:23rpx;opacity:.78}.search-box{height:82rpx;display:flex;align-items:center;margin-top:24rpx;padding:0 24rpx;border-radius:24rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(24,62,76,.05)}.search-icon{color:#0b8285;font-size:38rpx;margin-right:14rpx}.search-input{flex:1;height:82rpx;font-size:25rpx;color:#17384a}.search-placeholder{color:#93a5ac}.clear{width:44rpx;height:44rpx;border-radius:50%;background:#dce8ea;color:#6d858c;text-align:center;line-height:40rpx;font-size:34rpx}.summary{margin:28rpx 4rpx 18rpx;color:#82969e;font-size:23rpx}.department-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18rpx}.department-card{position:relative;min-width:0;min-height:148rpx;padding:25rpx 23rpx;box-sizing:border-box;border-radius:28rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(29,68,83,.06);display:flex;align-items:center;gap:18rpx}.icon{width:66rpx;height:66rpx;flex:0 0 66rpx;display:flex;align-items:center;justify-content:center;border-radius:21rpx;background:#e6f6f3;color:#09877c;font-size:27rpx;font-weight:750}.tone-1{background:#fff0e4;color:#d87531}.tone-2{background:#eaf0ff;color:#6177c5}.tone-3{background:#f4ebff;color:#8961bb}.department-content{min-width:0;padding-right:22rpx}.department-content text{display:block}.department-name{color:#17384a;font-size:28rpx;font-weight:750;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.arrow{position:absolute;right:20rpx;top:50%;transform:translateY(-50%);color:#8fb7b7;font-size:34rpx}.empty{display:flex;flex-direction:column;align-items:center;padding:120rpx 20rpx;color:#92a4ab}.empty-icon{width:92rpx;height:92rpx;display:flex;align-items:center;justify-content:center;border-radius:28rpx;background:#e7f1f2;color:#6d9b9c;font-size:46rpx}.empty-title{margin-top:22rpx;color:#425f6b;font-size:28rpx;font-weight:700}.empty-copy{margin-top:10rpx;font-size:22rpx}
</style>
