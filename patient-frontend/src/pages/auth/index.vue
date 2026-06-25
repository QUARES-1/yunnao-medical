<template>
  <view class="auth-page">
    <view class="bg-orb orb-one" />
    <view class="bg-orb orb-two" />

    <view class="brand-block">
      <text class="eyebrow">NEUSOFT SMART CARE</text>
      <text class="brand">云脑诊疗</text>
      <text class="slogan">微信授权后，为你建立专属就诊档案</text>
    </view>

    <view class="auth-card">
      <view class="icon-wrap">
        <text class="wechat-icon">微</text>
      </view>
      <text class="title">微信授权登录</text>
      <text class="subtitle">授权头像和昵称后，即可预约挂号、查看挂号记录和诊疗信息</text>

      <view class="wechat-profile-card">
        <button class="avatar-button" open-type="chooseAvatar" @chooseavatar="chooseAvatar">
          <image v-if="previewUrl" class="avatar" :src="previewUrl" mode="aspectFill" />
          <view v-else class="avatar avatar-placeholder">头像</view>
          <view class="camera">＋</view>
        </button>
        <text class="avatar-tip">点击头像区域授权微信头像</text>
        <view class="name-panel">
          <text class="field-label">微信昵称</text>
          <input
            v-model="name"
            class="nickname-input"
            type="nickname"
            maxlength="20"
            placeholder="点击填写微信昵称"
            placeholder-class="input-placeholder"
          />
          <text class="field-tip">用于挂号记录和就诊身份展示</text>
        </view>
      </view>

      <button class="auth-button" :loading="saving || auth.loading" @tap="authorizeAndEnter">
        授权并进入小程序
      </button>
      <text class="privacy-tip">仅用于本实训项目中的患者身份展示，不会用于其他用途</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { updatePatientInfo, uploadAvatar } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL, showError } from '@/utils/request'
import type { Patient } from '@/types/api'

const auth = useAuthStore()
const name = ref('')
const localAvatar = ref('')
const savedAvatar = ref('')
const saving = ref(false)

const previewUrl = computed(() => localAvatar.value || (savedAvatar.value ? (savedAvatar.value.startsWith('/') ? `${API_BASE_URL}${savedAvatar.value}` : savedAvatar.value) : ''))

function needsAuthorization(patient: Patient | null) {
  const nickname = patient?.name?.trim() || ''
  const avatar = patient?.avatar?.trim() || ''
  const defaultNames = ['微信用户', '未登录用户', '用户', '我']
  return !avatar || !nickname || defaultNames.includes(nickname)
}

function chooseAvatar(event: { detail: { avatarUrl: string } }) {
  localAvatar.value = event.detail.avatarUrl
}

function isInvalidPatientError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '')
  return message.includes('患者不存在') || message.includes('登录已过期') || message.includes('token') || message.includes('Token')
}

async function ensureLogin() {
  if (!auth.isLoggedIn()) {
    await auth.login()
    return
  }

  try {
    await auth.loadProfile()
  } catch (error) {
    if (!isInvalidPatientError(error)) throw error
    auth.logout()
    await auth.login()
  }
}

async function authorizeAndEnter() {
  const nickname = name.value.trim()
  if (!previewUrl.value) return uni.showToast({ title: '请先授权微信头像', icon: 'none' })
  if (!nickname) return uni.showToast({ title: '请填写微信昵称', icon: 'none' })
  saving.value = true
  try {
    await ensureLogin()
    const avatar = localAvatar.value ? await uploadAvatar(localAvatar.value) : savedAvatar.value
    await updatePatientInfo({ name: nickname, avatar })
    await auth.loadProfile()
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (e) {
    showError(e)
  } finally {
    saving.value = false
  }
}

onLoad(async () => {
  try {
    await ensureLogin()
    name.value = auth.patient?.name || ''
    savedAvatar.value = auth.patient?.avatar || ''
    if (!needsAuthorization(auth.patient)) {
      uni.reLaunch({ url: '/pages/index/index' })
    }
  } catch (e) {
    showError(e)
  }
})
</script>

<style scoped>
.auth-page{position:relative;min-height:100vh;box-sizing:border-box;padding:108rpx 34rpx 52rpx;background:linear-gradient(160deg,#075c71 0%,#078383 48%,#f4f8fb 48%,#f4f8fb 100%);overflow:hidden}.bg-orb{position:absolute;border-radius:50%;border:1rpx solid rgba(255,255,255,.15)}.orb-one{width:420rpx;height:420rpx;right:-150rpx;top:30rpx;box-shadow:0 0 0 70rpx rgba(255,255,255,.04)}.orb-two{width:260rpx;height:260rpx;left:-120rpx;top:300rpx;background:rgba(255,255,255,.06)}.brand-block{position:relative;z-index:1;color:#fff;margin-bottom:44rpx}.eyebrow,.brand,.slogan{display:block}.eyebrow{font-size:20rpx;letter-spacing:4rpx;opacity:.72}.brand{margin-top:10rpx;font-size:46rpx;font-weight:800}.slogan{margin-top:20rpx;width:560rpx;font-size:27rpx;line-height:1.6;opacity:.9}.auth-card{position:relative;z-index:2;padding:42rpx 30rpx 36rpx;border-radius:42rpx;background:rgba(255,255,255,.97);box-shadow:0 24rpx 70rpx rgba(9,69,82,.18)}.icon-wrap{width:86rpx;height:86rpx;margin:0 auto 16rpx;border-radius:30rpx;background:#e4f7f3;display:flex;align-items:center;justify-content:center}.wechat-icon{color:#087f84;font-size:45rpx;font-weight:800}.title,.subtitle{display:block;text-align:center}.title{color:#17384a;font-size:42rpx;font-weight:800}.subtitle{width:560rpx;max-width:100%;margin:16rpx auto 0;color:#7f939b;font-size:24rpx;line-height:1.6}.wechat-profile-card{display:flex;flex-direction:column;align-items:center;margin-top:36rpx;padding:30rpx 26rpx 28rpx;border:2rpx solid #d8eeec;border-radius:32rpx;background:linear-gradient(135deg,#f2fbfa,#ffffff);box-shadow:0 12rpx 34rpx rgba(8,127,132,.08)}.avatar-button{position:relative;width:172rpx;height:172rpx;margin:0;padding:0;border:0;border-radius:52rpx;background:transparent;overflow:visible}.avatar-button:after{border:0}.avatar{width:172rpx;height:172rpx;display:block;border-radius:52rpx;background:#dcefed;box-shadow:0 14rpx 36rpx rgba(8,127,132,.18)}.avatar-placeholder{display:flex;align-items:center;justify-content:center;color:#087f84;font-size:32rpx;font-weight:700}.camera{position:absolute;right:-8rpx;bottom:-8rpx;width:56rpx;height:56rpx;display:flex;align-items:center;justify-content:center;border:7rpx solid #fff;border-radius:50%;background:#087f84;color:#fff;font-size:32rpx}.avatar-tip{display:block;margin-top:18rpx;text-align:center;color:#3d8d8b;font-size:24rpx}.name-panel{width:100%;margin-top:28rpx}.field-label{display:block;margin-bottom:16rpx;color:#17384a;font-size:32rpx;font-weight:800}.nickname-input{height:90rpx;padding:0 26rpx;border:2rpx solid #cfe4e5;border-radius:24rpx;background:#fff;color:#17384a;font-size:32rpx;font-weight:700}.field-tip{display:block;margin-top:12rpx;color:#8ba0a7;font-size:22rpx}.input-placeholder{color:#a1b0b5;font-weight:400}.auth-button{height:94rpx;margin-top:30rpx;border-radius:26rpx;background:linear-gradient(135deg,#087f84,#15a092);color:#fff;font-size:31rpx;font-weight:800;box-shadow:0 14rpx 32rpx rgba(8,127,132,.22)}.privacy-tip{display:block;margin-top:22rpx;text-align:center;color:#98a9af;font-size:21rpx;line-height:1.5}
</style>
