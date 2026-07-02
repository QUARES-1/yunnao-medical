<template>
  <view class="page">
    <view class="hero"><text class="tag">MEDICAL ASSISTANT</text><text class="title">就医智能助手</text><text class="sub">挂号时间、取药流程、报告查询、楼层导诊、急诊位置，都可以直接问我。</text></view>
    <view class="chips"><text v-for="q in quickQuestions" :key="q" @tap="ask(q)">{{ q }}</text></view>
    <view class="card">
      <textarea v-model="question" class="textarea" placeholder="请输入你的问题，例如：检验报告在哪里查看？" />
      <button class="primary" :loading="loading" @tap="ask(question)">发送问题</button>
    </view>
    <view v-if="answer" class="result"><view class="result-head"><text class="result-title">AI回复</text><text class="source-badge">知识库优先</text></view><text class="answer">{{ answer }}</text><view v-if="related.length" class="related"><text>你还可以问：</text><text v-for="item in related" :key="item" @tap="ask(item)">{{ item }}</text></view></view>
    <view class="note">回复会优先匹配医院知识库；管理员可在 Web 管理端维护知识库、查看问答日志并沉淀标准答案。</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { streamSse } from '@/utils/sse'
import { showError } from '@/utils/request'
const question = ref('')
const answer = ref('')
const related = ref<string[]>([])
const loading = ref(false)
const quickQuestions = ['挂号时间是什么时候？', '取药流程怎么走？', '检验报告在哪里查？', '彩超和CT分别在哪儿？', '缴费在哪里办理？', '急诊在哪里？']
async function ask(q: string) {
  if (!q.trim()) return uni.showToast({ title: '请输入问题', icon: 'none' })
  question.value = q
  loading.value = true
  answer.value = ''
  related.value = []
  try {
    await streamSse({
      url: '/api/ai/chat/stream',
      method: 'POST',
      auth: false,
      data: { question: q, sessionId: `patient-service-${Date.now()}` },
      handlers: {
        onDelta: chunk => { answer.value += chunk },
        onDone: data => {
          try {
            const meta = JSON.parse(data || '{}')
            related.value = meta.relatedQuestions || []
          } catch {
            related.value = []
          }
        },
        onError: message => { throw new Error(message || 'AI回复生成失败') }
      }
    })
  }
  catch (e) { showError(e); answer.value = '网络连接失败，请检查后端服务。你也可以先在首页进入挂号记录查看预约，或到药房窗口凭预约编号取药。' }
  finally { loading.value = false }
}
</script>
<style scoped>
.page{min-height:100vh;background:#f4f8fb;padding:28rpx}.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#075c71,#0a9a8d);color:#fff}.tag,.title,.sub{display:block}.tag{font-size:20rpx;letter-spacing:3rpx;opacity:.7}.title{font-size:44rpx;font-weight:800;margin:12rpx 0}.sub{font-size:25rpx;line-height:1.7;opacity:.82}.chips{display:flex;flex-wrap:wrap;gap:16rpx;margin:26rpx 0}.chips text{padding:16rpx 22rpx;background:#e5f7f4;color:#07877c;border-radius:999rpx;font-size:24rpx}.card,.result{background:#fff;border-radius:30rpx;padding:26rpx;box-shadow:0 10rpx 30rpx rgba(29,68,83,.08)}.textarea{width:100%;height:180rpx;background:#f6fafb;border-radius:22rpx;padding:22rpx;box-sizing:border-box;font-size:26rpx}.primary{margin-top:22rpx;background:#078f87;color:#fff;border-radius:22rpx;font-weight:800}.result{margin-top:24rpx}.result-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:16rpx}.result-title{display:block;font-size:30rpx;font-weight:800;color:#18394b}.source-badge{padding:8rpx 16rpx;border-radius:999rpx;background:#e6f8f4;color:#078f87;font-size:22rpx;font-weight:700}.answer{display:block;font-size:26rpx;color:#4d6770;line-height:1.8}.related{margin-top:18rpx;display:flex;flex-wrap:wrap;gap:12rpx}.related text{font-size:23rpx;color:#078f87;background:#eefaf8;border-radius:18rpx;padding:12rpx 16rpx}.note{margin-top:22rpx;text-align:center;color:#91a3aa;font-size:22rpx;line-height:1.6}
</style>
