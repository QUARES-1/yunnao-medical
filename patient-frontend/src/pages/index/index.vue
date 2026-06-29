<template>
  <view class="page safe-bottom">
    <view class="hero">
      <view class="status-space" :style="{ height: headerClearance }" />
      <view class="topbar">
        <view><text class="eyebrow">NEUSOFT SMART CARE</text><text class="brand">云脑诊疗</text></view>
        <image v-if="homeAvatarUrl" class="avatar avatar-photo" :src="homeAvatarUrl" mode="aspectFill" @tap="go('/pages/profile/index')" />
        <view v-else class="avatar" @tap="go('/pages/profile/index')">{{ initial }}</view>
      </view>
      <view class="hero-copy">
        <text class="hero-title">让每一次就医，\n都更从容</text>
        <text class="hero-sub">科室查找、医生预约、诊疗记录，一站式守护您的健康</text>
      </view>
      <view class="search" @tap="go('/pages/doctors/list')"><text>⌕</text><text class="placeholder">搜索科室、医生或擅长领域</text></view>
    </view>

    <view class="content">
      <view class="quick-card">
        <view class="quick-item primary" @tap="go('/pages/doctors/list')"><text class="quick-icon">＋</text><text class="quick-title">预约挂号</text><text>按科室选择医生</text></view>
        <view class="divider" />
        <view class="quick-item" @tap="go('/pages/registrations/list')"><text class="quick-icon mint">✓</text><text class="quick-title">我的预约</text><text>查看就诊进度</text></view>
      </view>

      <view class="section-head ai-head"><text class="section-title">AI智能服务</text><text class="more">覆盖诊前、诊中、诊后 ›</text></view>
      <view class="ai-service-grid">
        <view v-for="item in aiServices" :key="item.title" class="ai-service-card" :class="item.tone" @tap="go(item.url)">
          <view class="ai-service-icon">{{ item.icon }}</view>
          <view class="ai-service-text"><text class="ai-title">{{ item.title }}</text><text class="ai-sub">{{ item.desc }}</text></view>
        </view>
      </view>

      <view class="section-head"><text class="section-title">热门科室</text><text class="more" @tap="go('/pages/departments/list')">查看全部 ›</text></view>
      <view class="dept-grid">
          <view v-for="(item, index) in popularDepartments" :key="item.id" class="dept-card" @tap="openDepartment(item.id)">
            <view class="dept-icon" :class="`tone-${index % 4}`">{{ deptIcon(index) }}</view>
            <text class="dept-name">{{ item.name }}</text>
          </view>
          <view v-if="loading" class="dept-card skeleton" />
      </view>

      <view class="health-banner">
        <view><text class="banner-tag">健康提示</text><text class="banner-title">夏日科学补水指南</text><text class="banner-sub">少量多次，关注身体发出的信号</text></view>
        <view class="pulse">♡</view>
      </view>
    </view>

    <view class="tabbar">
      <view class="tab active"><text class="tab-icon">⌂</text><text>首页</text></view>
      <view class="tab" @tap="go('/pages/registrations/list')"><text class="tab-icon">▣</text><text>挂号记录</text></view>
      <view class="tab" @tap="go('/pages/profile/index')"><text class="tab-icon">○</text><text>我的</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShareAppMessage, onShareTimeline, onShow } from '@dcloudio/uni-app'
import { getDepartments } from '@/api/patient'
import { showError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL } from '@/utils/request'
import type { Department, Patient } from '@/types/api'

const departments = ref<Department[]>([])
const loading = ref(true)
const auth = useAuthStore()
const checkingAuth = ref(false)
const headerClearance = ref('88px')
try {
  const menuButton = uni.getMenuButtonBoundingClientRect()
  headerClearance.value = `${menuButton.bottom + 12}px`
} catch {
  headerClearance.value = '88px'
}
const initial = computed(() => auth.patient?.name?.slice(0, 1) || '我')
const homeAvatarUrl = computed(() => {
  const avatar = auth.patient?.avatar
  return avatar ? (avatar.startsWith('/') ? `${API_BASE_URL}${avatar}` : avatar) : ''
})
const popularDepartments = computed(() => departments.value.slice(0, 6))
const aiServices = [
  { icon: '分', title: '诊前智能分诊', desc: '症状分析，推荐科室医生', url: '/pages/triage/index', tone: 'teal' },
  { icon: '助', title: '就医智能助手', desc: '挂号、取药、报告流程问答', url: '/pages/ai/service', tone: 'blue' },
  { icon: '药', title: 'AI用药指导', desc: '服药时间、禁忌和不良反应', url: '/pages/ai/medication', tone: 'green' },
  { icon: '报', title: '检验报告解读', desc: '异常指标与复查建议', url: '/pages/ai/report', tone: 'cyan' },
  { icon: '警', title: '危急值预警', desc: '高危指标紧急提醒', url: '/pages/ai/critical', tone: 'red' },
  { icon: '问', title: 'AI健康顾问', desc: '健康问题和用药疑问', url: '/pages/ai/chat', tone: 'purple' },
  { icon: '访', title: 'AI智能随访', desc: '恢复反馈与复诊提醒', url: '/pages/ai/followup', tone: 'orange' },
]
const icons = ['心', '儿', '内', '外', '眼', '骨']
const deptIcon = (i: number) => icons[i % icons.length]
const go = (url: string) => uni.navigateTo({ url })
const openDepartment = (id: number) => go(`/pages/doctors/list?departmentId=${id}`)

function needsProfileAuthorization(patient: Patient | null) {
  const name = patient?.name?.trim() || ''
  const avatar = patient?.avatar?.trim() || ''
  const defaultNames = ['微信用户', '未登录用户', '用户', '我']
  return !avatar || !name || defaultNames.includes(name)
}

async function ensureWechatAuthorization() {
  if (checkingAuth.value) return
  checkingAuth.value = true
  try {
    if (!auth.isLoggedIn()) {
      await auth.login()
    } else {
      await auth.loadProfile()
    }
    if (needsProfileAuthorization(auth.patient)) {
      uni.reLaunch({ url: '/pages/auth/index' })
    }
  } catch (e) {
    showError(e)
  } finally {
    checkingAuth.value = false
  }
}

onShow(async () => {
  uni.showShareMenu({ menus: ['shareAppMessage', 'shareTimeline'] })
  await ensureWechatAuthorization()
  try { departments.value = await getDepartments() } catch (e) { showError(e) } finally { loading.value = false }
})

onShareAppMessage(() => ({
  title: '云脑诊疗｜便捷预约，安心就医',
  path: '/pages/index/index',
}))

onShareTimeline(() => ({
  title: '云脑诊疗｜科室查询与医生预约',
  query: '',
}))
</script>

<style scoped>
.page{min-height:100vh;padding-bottom:150rpx}.hero{padding:0 34rpx 88rpx;color:#fff;background:linear-gradient(145deg,#075c71 0%,#087f84 58%,#28a99b 100%);border-radius:0 0 52rpx 52rpx;position:relative;overflow:hidden}.hero:after{content:'';position:absolute;width:420rpx;height:420rpx;border:1rpx solid rgba(255,255,255,.13);border-radius:50%;right:-170rpx;top:30rpx;box-shadow:0 0 0 70rpx rgba(255,255,255,.035)}.status-space{height:72rpx}.topbar{position:relative;z-index:1;display:flex;justify-content:space-between;align-items:center}.eyebrow,.brand{display:block}.eyebrow{font-size:18rpx;letter-spacing:3rpx;opacity:.68}.brand{font-size:34rpx;font-weight:700;margin-top:4rpx}.avatar{width:68rpx;height:68rpx;border-radius:24rpx;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;font-weight:700;border:1rpx solid rgba(255,255,255,.24)}.hero-copy{position:relative;z-index:1;margin:65rpx 0 40rpx}.hero-title{display:block;font-size:58rpx;line-height:1.28;font-weight:750;letter-spacing:1rpx}.hero-sub{display:block;width:100%;margin-top:20rpx;font-size:23rpx;line-height:1.7;white-space:nowrap;opacity:.8}.search{position:relative;z-index:1;height:92rpx;background:rgba(255,255,255,.96);color:#1c5560;border-radius:26rpx;display:flex;align-items:center;padding:0 30rpx;font-size:40rpx;box-shadow:0 18rpx 48rpx rgba(1,50,61,.18)}.placeholder{font-size:26rpx;color:#829da3;margin-left:20rpx}.content{padding:0 30rpx}.quick-card{display:flex;align-items:stretch;margin-top:-42rpx;position:relative;z-index:2;background:#fff;border-radius:30rpx;padding:30rpx 16rpx;box-shadow:0 14rpx 45rpx rgba(23,65,82,.10)}.quick-item{flex:1;display:flex;flex-direction:column;align-items:center;color:#91a0a8;font-size:21rpx}.quick-title{color:#1a394c;font-size:28rpx;font-weight:700;margin:12rpx 0 6rpx}.quick-icon{width:68rpx;height:68rpx;border-radius:22rpx;display:flex;align-items:center;justify-content:center;background:#e3f2ff;color:#2377b7;font-size:38rpx;font-weight:300}.quick-icon.mint{background:#e3f8f2;color:#139478}.divider{width:1rpx;background:#e9f0f3;margin:6rpx 0}.ai-head{margin-top:34rpx}.ai-service-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18rpx;margin:8rpx 4rpx 18rpx}.ai-service-card{min-height:170rpx;padding:22rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.08);display:flex;flex-direction:column;justify-content:space-between;position:relative;overflow:hidden}.ai-service-card:after{content:'';position:absolute;right:-34rpx;top:-34rpx;width:110rpx;height:110rpx;border-radius:50%;background:rgba(255,255,255,.45)}.ai-service-icon{width:62rpx;height:62rpx;border-radius:22rpx;display:flex;align-items:center;justify-content:center;font-size:27rpx;font-weight:900;background:#e5f7f4;color:#078f87}.ai-title,.ai-sub{display:block}.ai-title{font-size:27rpx;font-weight:800;color:#17384a;margin-top:14rpx}.ai-sub{font-size:21rpx;color:#82969d;line-height:1.45;margin-top:6rpx}.ai-service-card.teal{background:linear-gradient(135deg,#0b8f8d,#17b39e)}.ai-service-card.teal .ai-title,.ai-service-card.teal .ai-sub{color:#fff}.ai-service-card.teal .ai-service-icon{background:rgba(255,255,255,.2);color:#fff}.ai-service-card.blue .ai-service-icon{background:#e8f1ff;color:#4778c7}.ai-service-card.green .ai-service-icon{background:#e6f8ef;color:#16956d}.ai-service-card.cyan .ai-service-icon{background:#e8f8fb;color:#0b8da4}.ai-service-card.red .ai-service-icon{background:#fff0ed;color:#df5145}.ai-service-card.purple .ai-service-icon{background:#f2ecff;color:#7d5bc6}.ai-service-card.orange .ai-service-icon{background:#fff2df;color:#d98122}
.section-head{display:flex;justify-content:space-between;align-items:center;margin:48rpx 4rpx 24rpx}.section-title{font-size:33rpx;font-weight:750}.more{font-size:23rpx;color:#3a8c92}.dept-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18rpx;padding:2rpx 4rpx 22rpx}.dept-card{display:flex;flex-direction:column;align-items:flex-start;justify-content:center;width:100%;min-width:0;min-height:166rpx;box-sizing:border-box;padding:24rpx 20rpx;background:#fff;border-radius:28rpx;box-shadow:0 8rpx 24rpx rgba(29,68,83,.07)}.dept-icon{width:68rpx;height:68rpx;border-radius:22rpx;display:flex;align-items:center;justify-content:center;font-size:28rpx;font-weight:700;background:#e7f6f4;color:#0a8b7d}.tone-1{background:#fff1e5;color:#db752e}.tone-2{background:#eaf0ff;color:#5e76c8}.tone-3{background:#f5ebff;color:#8a61bd}.dept-name{width:100%;font-size:27rpx;font-weight:700;margin-top:18rpx;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.skeleton{background:linear-gradient(100deg,#fff,#edf3f5,#fff);animation:shine 1.2s infinite}@keyframes shine{50%{opacity:.6}}.health-banner{margin-top:24rpx;padding:32rpx;border-radius:30rpx;background:linear-gradient(130deg,#e9f8f3,#e8f2fb);display:flex;justify-content:space-between;align-items:center}.banner-tag,.banner-title,.banner-sub{display:block}.banner-tag{font-size:20rpx;color:#168777}.banner-title{font-size:29rpx;font-weight:700;margin:12rpx 0 8rpx}.banner-sub{font-size:22rpx;color:#718992}.pulse{width:100rpx;height:100rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#fff;color:#1b9c8d;font-size:50rpx;box-shadow:0 12rpx 28rpx rgba(31,143,127,.13)}.tabbar{position:fixed;left:24rpx;right:24rpx;bottom:22rpx;height:110rpx;padding-bottom:env(safe-area-inset-bottom);background:rgba(255,255,255,.96);border-radius:34rpx;display:flex;align-items:center;justify-content:space-around;box-shadow:0 14rpx 50rpx rgba(19,53,67,.18);z-index:10}.tab{width:150rpx;display:flex;flex-direction:column;align-items:center;color:#9aabb2;font-size:20rpx;gap:5rpx}.tab-icon{font-size:35rpx}.tab.active{color:#087f84;font-weight:700}
.status-space{min-height:110rpx}.topbar{padding:4rpx 2rpx 8rpx}.avatar-photo{display:block;background:#d8efeb}
</style>

