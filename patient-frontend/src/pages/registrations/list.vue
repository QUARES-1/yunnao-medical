<template><view class="page"><view class="tabs"><text v-for="s in statuses" :key="s" :class="{on:status===s}" @tap="change(s)">{{s||'全部'}}</text></view><view v-for="r in list" :key="r.id" class="card" @tap="open(r.id)"><view class="head"><text>{{r.departmentName}}</text><text class="status">{{r.status}}</text></view><text class="doctor">{{r.doctorName}} 医生</text><view class="time">{{r.registrationDate}} · {{r.timeSlot}}</view><view class="foot">预约编号 YN{{String(r.id).padStart(6,'0')}} <text>查看详情 ›</text></view></view><view v-if="!list.length" class="empty">还没有相关挂号记录</view></view></template>
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { onShow } from '@dcloudio/uni-app'
import { showError } from '@/utils/request'
import { useRegistrationsStore } from '@/stores/registrations'

const registrationStore = useRegistrationsStore()
const { records: list, activeStatus: status } = storeToRefs(registrationStore)
const statuses = ['', '待就诊', '就诊中', '已就诊', '已取消']
async function load() {
  try { await registrationStore.load() } catch (error) { showError(error) }
}
async function change(value: string) {
  try { await registrationStore.changeStatus(value) } catch (error) { showError(error) }
}
const open = (id: number) => uni.navigateTo({ url: `/pages/registrations/detail?id=${id}` })
onShow(load)
</script>
<style scoped>.page{padding:24rpx 30rpx}.tabs{display:flex;overflow:auto;gap:25rpx;margin-bottom:26rpx}.tabs text{flex:none;padding:14rpx 4rpx;color:#80939b;font-size:24rpx}.tabs .on{color:#087f84;font-weight:700;border-bottom:5rpx solid #087f84}.card{background:#fff;border-radius:28rpx;padding:28rpx;margin-bottom:22rpx}.head{display:flex;justify-content:space-between;font-size:25rpx;font-weight:700}.status{color:#078778;background:#e8f7f3;padding:7rpx 14rpx;border-radius:12rpx;font-size:20rpx}.doctor{display:block;font-size:30rpx;font-weight:700;margin:22rpx 0 12rpx}.time{color:#5e7680;font-size:24rpx}.foot{border-top:1rpx solid #eef2f3;margin-top:23rpx;padding-top:20rpx;font-size:20rpx;color:#9aabb2}.foot text{float:right;color:#087f84}.empty{text-align:center;color:#9aabb2;padding:180rpx 0}</style>

