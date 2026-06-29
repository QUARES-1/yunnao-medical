<template>
  <view class="page safe-bottom">
    <view class="hero">
      <text class="eyebrow">AI TRIAGE</text>
      <text class="title">智能分诊助手</text>
      <text class="sub">把不舒服的地方告诉我，我会帮你推荐合适的科室和医生。</text>
    </view>

    <view class="card input-card">
      <text class="label">请描述症状</text>
      <textarea v-model="chiefComplaint" class="textarea" maxlength="300" placeholder="例如：头痛两天，伴随发热、嗓子疼，体温38.2℃" />
      <view class="chips">
        <text v-for="item in examples" :key="item" class="chip" @tap="chiefComplaint = item">{{ item }}</text>
      </view>
      <button class="primary-btn" :loading="loading" @tap="submit">开始 AI 分诊</button>
    </view>

    <view v-if="result" class="card result-card">
      <view class="result-top">
        <view>
          <text class="small">推荐科室</text>
          <text class="dept">{{ result.recommendDepartment || '综合内科' }}</text>
        </view>
        <view class="score"><text>{{ result.confidence || 80 }}</text><text>匹配度</text></view>
      </view>
      <view class="analysis">
        <text class="analysis-title">AI 分析</text>
        <text class="analysis-text">{{ result.analysis || '建议结合症状变化选择对应科室进一步就诊，如症状加重请及时线下就医。' }}</text>
      </view>
      <view class="actions">
        <button class="ghost" @tap="openDepartment">查看该科室医生</button>
        <button class="solid" @tap="goChat">继续问 AI</button>
      </view>
    </view>

    <view v-if="result?.recommendDoctors?.length" class="section">
      <view class="section-head"><text>推荐医生</text><text class="more" @tap="openDepartment">更多 ›</text></view>
      <view v-for="doc in result.recommendDoctors" :key="doc.id" class="doctor-card" @tap="openDoctor(doc.id)">
        <view class="avatar">{{ doc.name?.slice(0, 1) || '医' }}</view>
        <view class="doc-info"><text class="doc-name">{{ doc.name }} <text class="tag">{{ doc.title || '医生' }}</text></text><text class="doc-sub">{{ doc.specialty || '专业诊疗，耐心问诊' }}</text></view>
        <text class="link">预约 ›</text>
      </view>
    </view>

    <view v-if="history.length" class="section">
      <view class="section-head"><text>最近分诊</text></view>
      <view v-for="item in history" :key="item.id" class="history-item">
        <text class="history-q">{{ item.chiefComplaint }}</text>
        <text class="history-a">推荐：{{ item.recommendDepartment || '待分析' }} · 匹配度 {{ item.confidence || '-' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { consultTriage, getTriageHistory } from '@/api/patient'
import { showError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type { TriageResult, TriageRecord } from '@/types/api'

const auth = useAuthStore()
const chiefComplaint = ref('')
const loading = ref(false)
const result = ref<TriageResult | null>(null)
const history = ref<TriageRecord[]>([])
const examples = ['头痛发热，嗓子疼', '胃痛反酸，吃饭后更明显', '胸闷心慌，活动后加重']

async function submit() {
  const text = chiefComplaint.value.trim()
  if (text.length < 4) return uni.showToast({ title: '请稍微详细描述一下症状', icon: 'none' })
  loading.value = true
  try {
    if (auth.isLoggedIn() && !auth.patient) await auth.loadProfile()
    result.value = await consultTriage({ chiefComplaint: text, patientId: auth.patient?.id })
    loadHistory()
  } catch (e) { showError(e) } finally { loading.value = false }
}
function openDepartment() {
  const id = result.value?.recommendDepartmentId
  uni.navigateTo({ url: id ? `/pages/doctors/list?departmentId=${id}` : '/pages/doctors/list' })
}
function openDoctor(id: number) { uni.navigateTo({ url: `/pages/doctors/detail?id=${id}` }) }
function goChat() { uni.navigateTo({ url: `/pages/ai/chat?question=${encodeURIComponent(chiefComplaint.value)}` }) }
async function loadHistory() {
  if (!auth.isLoggedIn()) return
  try { history.value = (await getTriageHistory({ page: 1, size: 3 })).content || [] } catch { /* history is optional */ }
}
onShow(loadHistory)
</script>

<style scoped>
.page{min-height:100vh;background:#f3f8fb;padding:28rpx 30rpx 60rpx;box-sizing:border-box}.hero{padding:44rpx 34rpx 70rpx;border-radius:38rpx;background:linear-gradient(145deg,#075c71,#0b918d 62%,#20b7a2);color:#fff;box-shadow:0 18rpx 42rpx rgba(8,127,132,.2)}.eyebrow,.title,.sub{display:block}.eyebrow{font-size:20rpx;letter-spacing:4rpx;opacity:.75}.title{margin-top:18rpx;font-size:48rpx;font-weight:850}.sub{margin-top:18rpx;font-size:25rpx;line-height:1.7;opacity:.88}.card{background:#fff;border-radius:34rpx;box-shadow:0 12rpx 34rpx rgba(30,72,88,.08)}.input-card{margin-top:-38rpx;padding:30rpx}.label{font-size:30rpx;font-weight:800;color:#17384a}.textarea{width:100%;height:210rpx;margin-top:20rpx;padding:24rpx;box-sizing:border-box;border-radius:26rpx;background:#f5fafb;color:#17384a;font-size:27rpx;line-height:1.6}.chips{display:flex;flex-wrap:wrap;gap:14rpx;margin:22rpx 0}.chip{padding:12rpx 18rpx;border-radius:999rpx;background:#e9f7f4;color:#087f84;font-size:22rpx}.primary-btn,.solid{height:88rpx;border-radius:24rpx;background:#0b918d;color:#fff;font-size:29rpx;font-weight:800}.result-card{margin-top:24rpx;padding:30rpx}.result-top{display:flex;justify-content:space-between;align-items:center}.small{display:block;color:#8aa0a8;font-size:22rpx}.dept{display:block;margin-top:8rpx;color:#17384a;font-size:42rpx;font-weight:850}.score{width:120rpx;height:120rpx;border-radius:36rpx;background:#e6f8f4;color:#0b918d;display:flex;flex-direction:column;align-items:center;justify-content:center;font-size:20rpx}.score text:first-child{font-size:36rpx;font-weight:850}.analysis{margin-top:28rpx;padding:24rpx;border-radius:24rpx;background:#f6fafb}.analysis-title{display:block;color:#17384a;font-weight:800;font-size:27rpx}.analysis-text{display:block;margin-top:12rpx;color:#607984;font-size:25rpx;line-height:1.7}.actions{display:flex;gap:18rpx;margin-top:26rpx}.ghost,.solid{flex:1}.ghost{height:88rpx;border-radius:24rpx;background:#edf7f6;color:#087f84;font-size:27rpx}.section{margin-top:34rpx}.section-head{display:flex;justify-content:space-between;margin:0 4rpx 18rpx;color:#17384a;font-size:31rpx;font-weight:850}.more{font-size:24rpx;color:#0b918d}.doctor-card,.history-item{display:flex;align-items:center;margin-bottom:18rpx;padding:24rpx;border-radius:28rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(30,72,88,.06)}.avatar{width:82rpx;height:82rpx;border-radius:26rpx;background:#cfefed;color:#087f84;display:flex;align-items:center;justify-content:center;font-size:34rpx;font-weight:850}.doc-info{flex:1;margin-left:22rpx}.doc-name,.doc-sub,.history-q,.history-a{display:block}.doc-name{font-size:29rpx;font-weight:850;color:#17384a}.tag{font-size:20rpx;color:#0b918d;background:#e8f7f4;border-radius:8rpx;padding:4rpx 8rpx}.doc-sub,.history-a{margin-top:10rpx;color:#8aa0a8;font-size:23rpx}.link{color:#0b918d;font-size:24rpx}.history-item{display:block}.history-q{font-size:27rpx;font-weight:750;color:#17384a}
</style>
