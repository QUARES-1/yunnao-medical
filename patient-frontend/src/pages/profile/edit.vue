<template><view class="page"><view class="profile-card"><text class="title">完善个人资料</text><text class="subtitle">选择你的微信头像并填写真实昵称</text><button class="avatar-button" open-type="chooseAvatar" @chooseavatar="chooseAvatar"><image v-if="previewUrl" class="avatar" :src="previewUrl" mode="aspectFill"/><view v-else class="avatar placeholder">{{name.slice(0,1)||'我'}}</view><view class="camera">＋</view></button><text class="avatar-tip">点击选择微信头像</text><view class="field"><text class="field-label">昵称</text><input v-model="name" class="nickname-input" type="nickname" maxlength="20" placeholder="请输入你的微信昵称" placeholder-class="input-placeholder"/></view></view><button class="save-button" :loading="saving" @tap="save">保存资料</button><text class="privacy-tip">头像和昵称仅用于本实训项目中的患者身份展示</text></view></template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { updatePatientInfo, uploadAvatar } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL, showError } from '@/utils/request'

const auth = useAuthStore()
const name = ref('')
const localAvatar = ref('')
const savedAvatar = ref('')
const saving = ref(false)
const fromEntry = ref(false)

const previewUrl = computed(() => localAvatar.value || (savedAvatar.value ? (savedAvatar.value.startsWith('/') ? `${API_BASE_URL}${savedAvatar.value}` : savedAvatar.value) : ''))

function chooseAvatar(event: { detail: { avatarUrl: string } }) {
  localAvatar.value = event.detail.avatarUrl
}

async function save() {
  const nickname = name.value.trim()
  if (!nickname) return uni.showToast({ title: '请填写昵称', icon: 'none' })
  saving.value = true
  try {
    const avatar = localAvatar.value ? await uploadAvatar(localAvatar.value) : savedAvatar.value
    if (!avatar) {
      saving.value = false
      return uni.showToast({ title: '请先选择微信头像', icon: 'none' })
    }
    await updatePatientInfo({ name: nickname, avatar })
    await auth.loadProfile()
    uni.showToast({ title: '授权成功', icon: 'success' })
    setTimeout(() => {
      if (fromEntry.value) uni.reLaunch({ url: '/pages/index/index' })
      else uni.navigateBack()
    }, 600)
  } catch (e) {
    showError(e)
  } finally {
    saving.value = false
  }
}

onLoad(async (query) => {
  fromEntry.value = query?.from === 'entry'
  try {
    await auth.loadProfile()
    name.value = auth.patient?.name || ''
    savedAvatar.value = auth.patient?.avatar || ''
  } catch (e) {
    showError(e)
  }
})
</script>
<style scoped>.page{min-height:100vh;padding:32rpx 30rpx;background:#f4f8fb}.profile-card{padding:42rpx 34rpx 36rpx;border-radius:32rpx;background:#fff;box-shadow:0 10rpx 30rpx rgba(29,68,83,.06)}.title,.subtitle{display:block;text-align:center}.title{color:#17384a;font-size:36rpx;font-weight:750}.subtitle{margin-top:12rpx;color:#8a9da4;font-size:23rpx}.avatar-button{position:relative;width:180rpx;height:180rpx;margin:42rpx auto 0;padding:0;border:0;border-radius:52rpx;background:transparent;overflow:visible}.avatar-button:after{border:0}.avatar{width:180rpx;height:180rpx;display:block;border-radius:52rpx;background:#dcefed;box-shadow:0 12rpx 30rpx rgba(8,127,132,.16)}.placeholder{display:flex;align-items:center;justify-content:center;color:#087f84;font-size:56rpx;font-weight:700}.camera{position:absolute;right:-8rpx;bottom:-8rpx;width:54rpx;height:54rpx;display:flex;align-items:center;justify-content:center;border:7rpx solid #fff;border-radius:50%;background:#087f84;color:#fff;font-size:30rpx}.avatar-tip{display:block;margin-top:18rpx;text-align:center;color:#4b8e8d;font-size:22rpx}.field{margin-top:44rpx}.field-label{display:block;margin-bottom:15rpx;color:#435f69;font-size:24rpx;font-weight:700}.nickname-input{height:86rpx;padding:0 24rpx;border:2rpx solid #e1ebed;border-radius:23rpx;background:#f7fafb;color:#17384a;font-size:27rpx}.input-placeholder{color:#a1b0b5}.save-button{height:88rpx;margin-top:30rpx;border-radius:24rpx;background:linear-gradient(135deg,#087f84,#15a092);color:#fff;font-size:28rpx;font-weight:700}.privacy-tip{display:block;margin-top:24rpx;text-align:center;color:#9aaab0;font-size:20rpx}</style>
