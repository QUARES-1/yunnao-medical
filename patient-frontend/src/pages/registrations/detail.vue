<template>
  <view v-if="item" class="page">
    <view class="state">
      <text class="check">✓</text>
      <text class="status">{{ item.status }}</text>
      <text class="status-tip">请按预约时间提前到院候诊</text>
    </view>

    <view class="visit-time">
      <view class="time-icon">诊</view>
      <view class="time-content">
        <text class="time-label">就诊时间</text>
        <text class="time-date">{{ item.registrationDate }}</text>
      </view>
      <view class="period">{{ item.timeSlot }}</view>
    </view>

    <view class="card">
      <view><text>就诊科室</text><b>{{ item.departmentName }}</b></view>
      <view><text>接诊医生</text><b>{{ item.doctorName }}</b></view>
      <view><text>患者姓名</text><b>{{ item.patientName }}</b></view>
      <view><text>预约编号</text><b>YN{{ String(item.id).padStart(6, '0') }}</b></view>
    </view>

    <navigator
      class="finish-button"
      url="/pages/index/index"
      open-type="reLaunch"
      hover-class="finish-button-hover"
    >完成并返回首页</navigator>
    <button v-if="item.status === '待就诊'" class="cancel-button" @tap="cancel">取消预约</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { cancelRegistration, getRegistrationDetail } from '@/api/patient'
import { showError } from '@/utils/request'
import type { Registration } from '@/types/api'

const item = ref<Registration>()
let id = 0

async function load() {
  try {
    item.value = await getRegistrationDetail(id)
  } catch (error) {
    showError(error)
  }
}

onLoad((query) => {
  id = Number(query?.id)
  load()
})

function cancel() {
  uni.showModal({
    title: '确认取消预约？',
    content: '取消后如需就诊，请重新预约。',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await cancelRegistration(id)
        uni.showToast({ title: '已取消' })
        load()
      } catch (error) {
        showError(error)
      }
    },
  })
}
</script>

<style scoped>
.page{padding:30rpx 30rpx 56rpx}.state{text-align:center;padding:34rpx 20rpx 28rpx}.state text{display:block}.check{margin:auto;width:84rpx;height:84rpx;line-height:84rpx;border-radius:50%;background:#dbf4ed;color:#078778;font-size:42rpx;font-weight:700}.status{font-size:36rpx;font-weight:700;color:#334b55;margin:18rpx 0 8rpx}.status-tip{font-size:23rpx;color:#8aa0a8}.visit-time{display:flex;align-items:center;background:linear-gradient(135deg,#087f84,#13a092);color:#fff;border-radius:28rpx;padding:30rpx;margin-bottom:26rpx;box-shadow:0 12rpx 30rpx rgba(8,127,132,.15)}.time-icon{width:76rpx;height:76rpx;border-radius:22rpx;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;font-size:30rpx;font-weight:700;margin-right:22rpx}.time-content{flex:1}.time-content text{display:block}.time-label{font-size:23rpx;opacity:.78;margin-bottom:7rpx}.time-date{font-size:36rpx;font-weight:750;letter-spacing:1rpx}.period{min-width:100rpx;padding:15rpx 20rpx;border-radius:20rpx;background:#fff;color:#087f84;text-align:center;font-size:32rpx;font-weight:750}.card{background:#fff;border-radius:28rpx;padding:12rpx 28rpx}.card view{display:flex;justify-content:space-between;align-items:center;padding:27rpx 0;border-bottom:1rpx solid #edf2f3;font-size:25rpx}.card view:last-child{border:0}.card text{color:#84969e}.card b{font-weight:650;color:#344f59}.finish-button{display:flex;align-items:center;justify-content:center;height:82rpx;margin-top:36rpx;background:#087f84;color:#fff;border-radius:22rpx;font-size:28rpx;font-weight:650}.finish-button-hover{opacity:.82;transform:scale(.99)}.cancel-button{margin-top:20rpx;background:#fff;color:#d25d5d;border-radius:22rpx;font-size:26rpx}
</style>
