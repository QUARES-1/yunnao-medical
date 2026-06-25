<template>
  <view class="page">
    <view class="filter-panel">
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索医生、科室或擅长领域"
          placeholder-class="search-placeholder"
          confirm-type="search"
        />
        <text v-if="keyword" class="clear" @tap="keyword = ''">×</text>
      </view>

      <scroll-view scroll-x class="department-scroll" :show-scrollbar="false">
        <view class="chips">
          <view class="chip" :class="{ on: !selected }" @tap="select()">全部</view>
          <view
            v-for="department in departments"
            :key="department.id"
            class="chip"
            :class="{ on: selected === department.id }"
            @tap="select(department.id)"
          >
            {{ department.name }}
          </view>
        </view>
      </scroll-view>

      <view class="title-filter">
        <text class="filter-label">职称</text>
        <scroll-view scroll-x class="title-scroll" :show-scrollbar="false">
          <view class="title-chips">
            <view class="title-chip" :class="{ on: !selectedTitle }" @tap="selectedTitle = ''">全部职称</view>
            <view
              v-for="title in titleOptions"
              :key="title"
              class="title-chip"
              :class="{ on: selectedTitle === title }"
              @tap="selectedTitle = title"
            >{{ title }}</view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="summary">
      <text>为您找到 <b>{{ filteredDoctors.length }}</b> 位医生</text>
      <text v-if="keyword" class="result-keyword">“{{ keyword }}”</text>
    </view>

    <view
      v-for="doctor in filteredDoctors"
      :key="doctor.id"
      class="doctor-card"
      @tap="open(doctor.id)"
    >
      <view class="portrait">{{ doctor.name.slice(0, 1) }}</view>
      <view class="doctor-info">
        <view class="doctor-heading">
          <text class="doctor-name">{{ doctor.name }}</text>
          <text class="doctor-title">{{ doctor.title || '医师' }}</text>
        </view>
        <text class="department-name">{{ doctor.departmentName || '综合门诊' }}</text>
        <text class="specialty">擅长：{{ doctor.specialty || '常见病及多发病诊疗' }}</text>
        <view class="card-footer">
          <text class="available"><i />近期可预约</text>
          <text class="detail-link">查看主页 ›</text>
        </view>
      </view>
    </view>

    <view v-if="!loading && !filteredDoctors.length" class="empty-state">
      <view class="empty-icon">⌕</view>
      <text class="empty-title">没有找到相关医生</text>
      <text class="empty-copy">试试更换关键词或选择其他科室</text>
      <button v-if="keyword || selected || selectedTitle" @tap="resetFilter">清除筛选</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getDepartments, getDoctors } from '@/api/patient'
import { showError } from '@/utils/request'
import type { Department, Doctor } from '@/types/api'

const departments = ref<Department[]>([])
const doctors = ref<Doctor[]>([])
const selected = ref<number>()
const selectedTitle = ref('')
const keyword = ref('')
const loading = ref(true)

const titleOptions = computed(() => {
  const priority = ['主任医师', '副主任医师', '主治医师', '医师']
  const available = [...new Set(doctors.value.map((doctor) => doctor.title).filter(Boolean) as string[])]
  return available.sort((a, b) => {
    const aIndex = priority.indexOf(a)
    const bIndex = priority.indexOf(b)
    return (aIndex < 0 ? 99 : aIndex) - (bIndex < 0 ? 99 : bIndex)
  })
})

const filteredDoctors = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return doctors.value.filter((doctor) => {
    if (selectedTitle.value && doctor.title !== selectedTitle.value) return false
    if (!value) return true
    return [doctor.name, doctor.departmentName, doctor.title, doctor.specialty]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(value))
  })
})

async function select(id?: number) {
  selected.value = id
  selectedTitle.value = ''
  loading.value = true
  try {
    doctors.value = await getDoctors(id)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  keyword.value = ''
  selectedTitle.value = ''
  if (selected.value) select()
}

const open = (id: number) => uni.navigateTo({ url: `/pages/doctors/detail?id=${id}` })

onLoad(async (query) => {
  const departmentId = query?.departmentId ? Number(query.departmentId) : undefined
  try {
    departments.value = await getDepartments()
    await select(departmentId)
  } catch (error) {
    showError(error)
  }
})
</script>

<style scoped>
.page { min-height: 100vh; padding: 0 30rpx 44rpx; background: #f4f8fb; }
.filter-panel { position: sticky; top: 0; z-index: 5; margin: 0 -30rpx; padding: 18rpx 30rpx 22rpx; background: rgba(255,255,255,.97); box-shadow: 0 8rpx 24rpx rgba(24,62,76,.05); }
.search-box { height: 82rpx; display: flex; align-items: center; padding: 0 24rpx; border-radius: 24rpx; background: #f2f7f8; border: 2rpx solid transparent; }
.search-box:focus-within { background: #fff; border-color: #65b9b2; box-shadow: 0 8rpx 24rpx rgba(8,127,132,.10); }
.search-icon { color: #0b8285; font-size: 38rpx; line-height: 1; margin-right: 14rpx; }
.search-input { flex: 1; height: 82rpx; color: #17384a; font-size: 25rpx; }
.search-placeholder { color: #93a5ac; }
.clear { width: 44rpx; height: 44rpx; border-radius: 50%; background: #dce8ea; color: #6d858c; text-align: center; line-height: 40rpx; font-size: 34rpx; }
.department-scroll { width: 100%; margin-top: 20rpx; white-space: nowrap; }
.chips { display: flex; gap: 14rpx; padding-right: 10rpx; }
.chip { flex: none; padding: 14rpx 25rpx; border-radius: 20rpx; background: #f0f5f6; color: #6e858c; font-size: 23rpx; }
.chip.on { background: linear-gradient(135deg,#087f84,#19a092); color: #fff; font-weight: 600; box-shadow: 0 7rpx 18rpx rgba(8,127,132,.19); }
.title-filter { display: flex; align-items: center; margin-top: 18rpx; }
.filter-label { flex: none; margin-right: 16rpx; color: #516d77; font-size: 22rpx; font-weight: 700; }
.title-scroll { flex: 1; min-width: 0; white-space: nowrap; }
.title-chips { display: flex; gap: 12rpx; padding-right: 8rpx; }
.title-chip { flex: none; padding: 11rpx 21rpx; border: 1rpx solid #dfeaec; border-radius: 18rpx; background: #fff; color: #71878e; font-size: 21rpx; }
.title-chip.on { border-color: #79c4bc; background: #e7f7f3; color: #087f84; font-weight: 650; }
.summary { display: flex; justify-content: space-between; align-items: center; margin: 26rpx 4rpx 20rpx; color: #83959d; font-size: 23rpx; }
.summary b { color: #087f84; font-size: 27rpx; }
.result-keyword { max-width: 260rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #4c7a7e; }
.doctor-card { display: flex; padding: 27rpx 24rpx; margin-bottom: 20rpx; border-radius: 28rpx; background: #fff; box-shadow: 0 9rpx 28rpx rgba(30,66,80,.07); }
.portrait { width: 104rpx; height: 118rpx; flex: none; border-radius: 26rpx; background: linear-gradient(150deg,#dceff0,#b8dedc); display: flex; align-items: center; justify-content: center; color: #087f84; font-size: 37rpx; font-weight: 700; }
.doctor-info { flex: 1; min-width: 0; margin-left: 23rpx; }
.doctor-heading { display: flex; align-items: center; }
.doctor-name { color: #17384a; font-size: 30rpx; font-weight: 750; }
.doctor-title { margin-left: 13rpx; padding: 5rpx 10rpx; border-radius: 9rpx; background: #e8f7f3; color: #268e83; font-size: 19rpx; }
.department-name, .specialty { display: block; }
.department-name { margin: 11rpx 0; color: #55717b; font-size: 22rpx; }
.specialty { color: #899aa1; font-size: 21rpx; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 17rpx; padding-top: 16rpx; border-top: 1rpx solid #edf2f3; font-size: 20rpx; }
.available { color: #2b9a71; }
.available i { display: inline-block; width: 10rpx; height: 10rpx; margin-right: 9rpx; border-radius: 50%; background: #2cc58c; }
.detail-link { color: #087f84; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 120rpx 30rpx; color: #92a4ab; }
.empty-icon { width: 98rpx; height: 98rpx; display: flex; align-items: center; justify-content: center; border-radius: 32rpx; background: #e7f1f2; color: #6d9b9c; font-size: 48rpx; }
.empty-title { margin-top: 24rpx; color: #425f6b; font-size: 28rpx; font-weight: 700; }
.empty-copy { margin-top: 10rpx; font-size: 22rpx; }
.empty-state button { margin-top: 30rpx; padding: 0 32rpx; height: 68rpx; line-height: 68rpx; border-radius: 20rpx; background: #087f84; color: #fff; font-size: 23rpx; }
</style>
