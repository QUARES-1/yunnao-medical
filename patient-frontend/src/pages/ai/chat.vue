<template>
  <view class="page safe-bottom">
    <view class="top-card">
      <text class="eyebrow">AI HEALTH ASSISTANT</text>
      <text class="title">AI 健康顾问</text>
      <text class="sub">可以咨询就医流程、常见健康问题，也可以让 AI 结合你的就诊记录给建议。</text>
    </view>

    <view class="quick-list">
      <text v-for="item in quickQuestions" :key="item" class="quick" @tap="askQuick(item)">{{ item }}</text>
    </view>

    <scroll-view class="messages" scroll-y :scroll-into-view="lastMessageId">
      <view v-for="(msg, index) in messages" :id="`msg-${index}`" :key="index" class="message" :class="msg.role">
        <view class="bubble">
          <text>{{ msg.content }}</text>
          <view v-if="msg.related?.length" class="related">
            <text v-for="q in msg.related" :key="q" @tap="askQuick(q)">{{ q }}</text>
          </view>
          <view v-if="msg.departmentId" class="recommend" @tap="openDepartment(msg.departmentId)">推荐科室：{{ msg.department }}，点击查看医生 ›</view>
        </view>
      </view>
      <view v-if="loading" id="loading" class="message ai"><view class="bubble loading">AI 正在认真思考...</view></view>
    </scroll-view>

    <view class="composer">
      <input v-model="question" class="input" confirm-type="send" placeholder="输入你想咨询的问题" @confirm="send" />
      <button class="send" :loading="loading" @tap="send">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { askAiChat, healthConsult } from '@/api/patient'
import { showError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

interface Message { role: 'user' | 'ai'; content: string; related?: string[]; department?: string; departmentId?: number }
const auth = useAuthStore()
const question = ref('')
const loading = ref(false)
const lastMessageId = ref('')
const sessionId = `patient-${Date.now()}`
const messages = ref<Message[]>([
  { role: 'ai', content: '你好呀，我是云脑诊疗 AI 健康顾问。你可以问我“感冒怎么办”“挂号流程”“胃痛挂什么科”等问题。' }
])
const quickQuestions = ['发烧咳嗽应该挂什么科？', '胃痛反酸怎么办？', '高血压平时注意什么？']

function scrollBottom() {
  nextTick(() => { lastMessageId.value = loading.value ? 'loading' : `msg-${messages.value.length - 1}` })
}
function askQuick(text: string) { question.value = text; send() }
async function send() {
  const text = question.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  question.value = ''
  loading.value = true
  scrollBottom()
  try {
    let res
    if (auth.isLoggedIn()) {
      res = await healthConsult({ question: text, includeHistory: true })
    } else {
      res = await askAiChat({ question: text, sessionId })
    }
    messages.value.push({
      role: 'ai',
      content: res.answer || '我暂时没有得到明确答案，建议你到线下医院进一步咨询。',
      related: res.relatedQuestions || [],
      department: res.recommendDepartment,
      departmentId: res.recommendDepartmentId
    })
  } catch (e) { showError(e) } finally { loading.value = false; scrollBottom() }
}
function openDepartment(id: number) { uni.navigateTo({ url: `/pages/doctors/list?departmentId=${id}` }) }
onLoad((query) => {
  const q = query?.question ? decodeURIComponent(String(query.question)) : ''
  if (q) { question.value = q; send() }
})
</script>

<style scoped>
.page{min-height:100vh;background:#f3f8fb;padding:24rpx 24rpx 150rpx;box-sizing:border-box}.top-card{padding:36rpx 32rpx;border-radius:34rpx;background:linear-gradient(145deg,#075c71,#0b918d 60%,#18b49f);color:#fff;box-shadow:0 16rpx 38rpx rgba(8,127,132,.2)}.eyebrow,.title,.sub{display:block}.eyebrow{font-size:18rpx;letter-spacing:4rpx;opacity:.72}.title{margin-top:14rpx;font-size:44rpx;font-weight:850}.sub{margin-top:14rpx;font-size:24rpx;line-height:1.65;opacity:.86}.quick-list{display:flex;gap:14rpx;overflow-x:auto;margin:24rpx 0}.quick{white-space:nowrap;padding:14rpx 20rpx;border-radius:999rpx;background:#fff;color:#0b918d;font-size:23rpx;box-shadow:0 6rpx 18rpx rgba(30,72,88,.06)}.messages{height:calc(100vh - 420rpx)}.message{display:flex;margin:18rpx 4rpx}.message.user{justify-content:flex-end}.message.ai{justify-content:flex-start}.bubble{max-width:78%;padding:22rpx 24rpx;border-radius:26rpx;font-size:27rpx;line-height:1.65;box-shadow:0 8rpx 24rpx rgba(30,72,88,.06)}.ai .bubble{background:#fff;color:#17384a;border-top-left-radius:8rpx}.user .bubble{background:#0b918d;color:#fff;border-top-right-radius:8rpx}.loading{color:#8097a1}.related{display:flex;flex-direction:column;gap:10rpx;margin-top:18rpx}.related text{padding:12rpx 16rpx;border-radius:18rpx;background:#edf8f6;color:#0b918d;font-size:23rpx}.recommend{margin-top:18rpx;padding:16rpx;border-radius:18rpx;background:#e9f7f4;color:#087f84;font-size:24rpx}.composer{position:fixed;left:24rpx;right:24rpx;bottom:24rpx;padding:18rpx;display:flex;gap:16rpx;border-radius:30rpx;background:#fff;box-shadow:0 14rpx 44rpx rgba(30,72,88,.14);padding-bottom:calc(18rpx + env(safe-area-inset-bottom))}.input{flex:1;height:76rpx;padding:0 24rpx;border-radius:22rpx;background:#f3f8fb;color:#17384a;font-size:27rpx}.send{width:136rpx;height:76rpx;line-height:76rpx;border-radius:22rpx;background:#0b918d;color:#fff;font-size:27rpx;font-weight:800}
</style>
