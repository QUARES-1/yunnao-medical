<template>
  <scroll-view class="page" scroll-y :enhanced="true" :show-scrollbar="false">
    <view class="hero">
      <text class="tag">AI FOLLOW-UP</text>
      <text class="title">AI智能随访</text>
      <text class="sub">就诊后按计划填写恢复情况，AI 会判断是否有异常信号，并给出复诊提醒。</text>
    </view>

    <view class="overview">
      <view>
        <b>{{ pending.length }}</b>
        <text>待填写</text>
      </view>
      <view>
        <b>{{ plans.length }}</b>
        <text>随访计划</text>
      </view>
      <view>
        <b>{{ completedTotal }}</b>
        <text>已完成</text>
      </view>
    </view>

    <view v-if="lastResult" :class="['result-card', lastResult.abnormalFlag === 1 ? 'danger' : 'safe']">
      <view class="result-head">
        <view>
          <text class="section-tag">LATEST AI RESULT</text>
          <text class="section-title">最近一次 AI 分析反馈</text>
        </view>
        <text class="result-badge">{{ lastResult.abnormalFlag === 1 ? '建议复诊' : '恢复平稳' }}</text>
      </view>
      <text class="result-text">{{ lastResult.aiAnalysis || 'AI 已完成分析，请继续按医嘱观察恢复情况。' }}</text>
      <view class="result-actions">
        <text>这就是填写随访的作用：系统会帮你筛查恢复风险，并把异常情况标记出来。</text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <view>
          <text class="section-tag">PENDING TASKS</text>
          <text class="section-title">待填写随访</text>
          <text class="section-desc">请按时填写，医生和系统会根据你的反馈判断是否需要复诊。</text>
        </view>
        <button class="refresh-btn" size="mini" :loading="loading" @tap="loadData">刷新</button>
      </view>

      <view v-for="group in groupedPending" :key="group.planId" class="task-group">
        <view class="group-head" @tap="toggleGroup(group.planId)">
          <view class="group-icon">访</view>
          <view class="group-main">
            <text class="group-title">{{ group.name }}</text>
            <text class="group-desc">{{ group.items.length }} 次待填写 · 点击进入查看第 1、2、3 次任务</text>
          </view>
          <view class="group-count">
            <b>{{ group.items.length }}</b>
            <text>{{ isExpanded(group.planId) ? '收起' : '进入' }}</text>
          </view>
        </view>

        <view v-if="isExpanded(group.planId)" class="group-tasks">
          <view v-for="item in group.items" :key="item.id" class="task-card">
            <view class="task-date">
              <b>{{ day(item.followUpTime) }}</b>
              <text>{{ month(item.followUpTime) }}</text>
            </view>
            <view class="task-main">
              <text class="task-title">第 {{ groupTaskOrder(group, item) }} 次随访</text>
              <text class="task-sub">{{ timeText(item.followUpTime) }}</text>
              <text class="task-purpose">反馈体温、症状变化、服药情况、伤口恢复</text>
            </view>
            <button size="mini" @tap="openSubmit(item)">填写并分析</button>
          </view>
        </view>
      </view>

      <view v-if="!loading && !pending.length" class="empty">
        <text class="empty-title">当前没有待填写任务</text>
        <text>如果医生创建了新的随访计划，任务会自动出现在这里。</text>
      </view>
    </view>

    <view class="section">
      <text class="section-tag">MY PLANS</text>
      <text class="section-title">我的随访计划</text>
      <text class="section-desc">这里显示医生安排的随访节奏，以及你已经完成了多少次。</text>

      <view v-for="item in plans" :key="item.id" class="plan-card">
        <view class="plan-icon">访</view>
        <view class="plan-main">
          <text class="plan-name">{{ item.disease || '康复随访计划' }}</text>
          <text class="plan-meta">{{ item.planType || 'AI智能随访' }} · 共 {{ item.totalTimes || 0 }} 次</text>
          <view class="progress">
            <i :style="{ width: progress(item) + '%' }"></i>
          </view>
          <text class="plan-tip">完成进度 {{ progress(item) }}%，异常反馈会提醒医生关注。</text>
        </view>
        <view class="plan-count">
          <b>{{ item.completedTimes || 0 }}</b>
          <text>/{{ item.totalTimes || 0 }}</text>
        </view>
      </view>

      <view v-if="!loading && !plans.length" class="empty">
        <text class="empty-title">暂无随访计划</text>
        <text>完成就诊后，医生可以为你创建术后、用药或慢病随访计划。</text>
      </view>
    </view>

    <view class="flow-card">
      <text class="section-tag">HOW IT WORKS</text>
      <text class="section-title">随访流程</text>
      <view class="flow-row"><b>1</b><text>医生创建随访计划</text></view>
      <view class="flow-row"><b>2</b><text>患者按提醒填写恢复情况</text></view>
      <view class="flow-row"><b>3</b><text>AI 分析异常信号，必要时提醒复诊</text></view>
    </view>

    <view class="bottom-space"></view>

    <view v-if="showForm" class="mask">
      <view class="form-sheet">
        <view class="form-head">
          <view>
            <text>FOLLOW-UP FORM</text>
            <b>恢复情况反馈</b>
            <em>{{ current ? planName(current.planId) : '随访任务' }}</em>
          </view>
          <button @tap="showForm = false">×</button>
        </view>

        <scroll-view class="form-body" scroll-y :show-scrollbar="true" :enhanced="true">
          <view class="form-guide">
            <b>填写后会发生什么？</b>
            <text>AI 会根据你的体温、症状、服药和伤口情况生成评估结果。若发现高热、胸痛、呼吸困难、伤口流脓等异常，会提示尽快复诊。</text>
          </view>

          <label>整体恢复情况</label>
          <view class="options">
            <text v-for="x in recoveryOptions" :key="x" :class="{ active: form.recovery === x }" @tap="form.recovery = x">{{ x }}</text>
          </view>

          <label>当前体温（℃）</label>
          <input v-model="form.temperature" type="digit" placeholder="例如 36.7" />

          <label>目前仍有的不适或新症状</label>
          <textarea v-model="form.symptoms" placeholder="请如实填写，例如：咳嗽、疼痛、胸闷、伤口红肿、没有明显不适等" />

          <label>服药情况</label>
          <view class="options">
            <text v-for="x in medicineOptions" :key="x" :class="{ active: form.medicine === x }" @tap="form.medicine = x">{{ x }}</text>
          </view>

          <label>伤口情况</label>
          <input v-model="form.wound" placeholder="如无伤口请填写“无”" />

          <view class="safety-tip">
            <b>AI异常提醒规则</b>
            <text>如果出现持续高热、症状加重、胸痛、呼吸困难、伤口流脓或自行停药，系统会标记为异常恢复信号。</text>
          </view>

          <view class="scroll-space"></view>
        </scroll-view>

        <button class="submit" :loading="submitting" @tap="submitForm">提交并生成 AI 反馈</button>
      </view>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getFollowUpDetail, getFollowUpPlans, getPendingFollowUps, submitFollowUp } from '@/api/patient'
import type { FollowUpPlan, FollowUpRecord } from '@/types/api'
import { showError } from '@/utils/request'

type FollowUpDetail = FollowUpRecord & { aiAnalysis?: string; abnormalFlag?: number }

const loading = ref(false)
const submitting = ref(false)
const showForm = ref(false)
const plans = ref<FollowUpPlan[]>([])
const pending = ref<FollowUpRecord[]>([])
const current = ref<FollowUpRecord>()
const lastResult = ref<FollowUpDetail | null>(null)
const expandedPlanIds = ref<number[]>([])

const recoveryOptions = ['明显好转', '略有好转', '无明显变化', '症状加重']
const medicineOptions = ['按时服药', '偶尔漏服', '已自行停药']
const form = reactive({
  recovery: '略有好转',
  temperature: '36.7',
  symptoms: '',
  medicine: '按时服药',
  wound: '无'
})

const completedTotal = computed(() => plans.value.reduce((n, x) => n + (x.completedTimes || 0), 0))
const groupedPending = computed(() => {
  const map = new Map<number, { planId: number; name: string; items: FollowUpRecord[] }>()
  pending.value.forEach(item => {
    const planId = item.planId || 0
    if (!map.has(planId)) {
      map.set(planId, { planId, name: planName(planId), items: [] })
    }
    map.get(planId)!.items.push(item)
  })
  return Array.from(map.values()).map(group => ({
    ...group,
    items: group.items.slice().sort((a, b) => toDate(a.followUpTime).getTime() - toDate(b.followUpTime).getTime())
  }))
})

const progress = (x: FollowUpPlan) => Math.min(100, Math.round(((x.completedTimes || 0) / Math.max(1, x.totalTimes || 1)) * 100))
const planName = (id?: number) => plans.value.find(x => x.id === id)?.disease || '康复情况随访'
const groupTaskOrder = (group: { items: FollowUpRecord[] }, x: FollowUpRecord) => group.items.findIndex(y => y.id === x.id) + 1 || 1
const toDate = (v?: string) => (v ? new Date(v) : new Date())
const day = (v?: string) => String(toDate(v).getDate()).padStart(2, '0')
const month = (v?: string) => `${toDate(v).getMonth() + 1}月`
const timeText = (v?: string) => {
  if (!v) return '待提醒'
  const d = toDate(v)
  const diff = d.getTime() - Date.now()
  return diff > 86400000 ? `${Math.ceil(diff / 86400000)}天后提醒` : diff > 0 ? '今日待填写' : '已到随访时间'
}

async function loadData() {
  loading.value = true
  try {
    const [p, r] = await Promise.all([
      getFollowUpPlans({ page: 1, size: 20 }),
      getPendingFollowUps({ page: 1, size: 30 })
    ])
    plans.value = p.content || []
    pending.value = r.content || []
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

function isExpanded(planId: number) {
  return expandedPlanIds.value.includes(planId)
}

function toggleGroup(planId: number) {
  if (isExpanded(planId)) {
    expandedPlanIds.value = expandedPlanIds.value.filter(id => id !== planId)
  } else {
    expandedPlanIds.value = [...expandedPlanIds.value, planId]
  }
}

function openSubmit(item: FollowUpRecord) {
  current.value = item
  form.recovery = '略有好转'
  form.temperature = '36.7'
  form.symptoms = ''
  form.medicine = '按时服药'
  form.wound = '无'
  showForm.value = true
}

async function submitForm() {
  if (!current.value) return
  if (!form.symptoms.trim()) {
    return uni.showToast({ title: '请填写当前症状', icon: 'none' })
  }
  submitting.value = true
  try {
    await submitFollowUp(current.value.id, { ...form })
    const detail = await getFollowUpDetail(current.value.id) as FollowUpDetail
    lastResult.value = detail
    showForm.value = false

    if (detail.abnormalFlag === 1) {
      uni.showModal({
        title: 'AI发现异常信号',
        content: detail.aiAnalysis || '建议尽快联系医生并安排复诊。',
        showCancel: false,
        confirmText: '查看反馈'
      })
    } else {
      uni.showModal({
        title: 'AI随访分析完成',
        content: detail.aiAnalysis || '目前恢复情况总体平稳，请继续按医嘱康复。',
        showCancel: false,
        confirmText: '知道了'
      })
    }
    await loadData()
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page{height:100vh;background:#f4f8fb;color:#18394b;box-sizing:border-box;padding:28rpx}
.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#075c71,#6976d9);color:#fff;box-shadow:0 18rpx 36rpx rgba(32,80,123,.18)}
.tag,.title,.sub{display:block}.tag,.section-tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:42rpx;font-weight:850;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7}
.result-card,.section,.flow-card{margin-top:20rpx;padding:27rpx;border-radius:29rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}
.overview{display:grid;grid-template-columns:repeat(3,1fr);gap:0;margin:20rpx 0 22rpx;padding:22rpx 0;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.overview view{position:relative;text-align:center}.overview view+view::before{content:'';position:absolute;left:0;top:10rpx;bottom:10rpx;width:1rpx;background:#e9f1f2}.overview b,.overview text{display:block}.overview b{font-size:38rpx;line-height:1;color:#168d86;font-weight:900}.overview text{margin-top:10rpx;font-size:20rpx;color:#7f949b;font-weight:700}
.section-head,.result-head{display:flex;justify-content:space-between;gap:18rpx;align-items:flex-start}.section-tag{display:block;color:#3c86ad}.section-title{display:block;margin:5rpx 0 8rpx;font-size:30rpx;font-weight:850}.section-desc{display:block;margin-bottom:18rpx;font-size:22rpx;color:#81949a;line-height:1.5}.section-head button,.task-card button{margin:0;background:#eaf7f6;color:#087f84;border-radius:16rpx}.refresh-btn{width:74rpx!important;height:54rpx!important;line-height:54rpx!important;padding:0!important;font-size:21rpx!important;border-radius:18rpx!important;flex:none}
.task-group{border:1rpx solid #e3eef0;border-radius:25rpx;background:#fbfefe;overflow:hidden}.task-group+.task-group{margin-top:16rpx}.group-head{display:flex;align-items:center;gap:16rpx;padding:22rpx}.group-icon{width:58rpx;height:58rpx;border-radius:18rpx;background:#e8f5f2;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:900}.group-main{flex:1;min-width:0}.group-title,.group-desc{display:block}.group-title{font-size:27rpx;font-weight:900;color:#18394b}.group-desc{margin-top:7rpx;font-size:20rpx;color:#7f9399}.group-count{text-align:center;min-width:62rpx}.group-count b,.group-count text{display:block}.group-count b{font-size:31rpx;color:#168d86}.group-count text{margin-top:2rpx;font-size:18rpx;color:#7d9299}.group-tasks{padding:0 16rpx 16rpx}
.task-card,.plan-card{display:flex;align-items:center;gap:16rpx;padding:19rpx;border:1rpx solid #e4edef;border-radius:21rpx;background:#fff}.task-card+.task-card,.plan-card+.plan-card{margin-top:13rpx}.task-date{width:65rpx;flex:none;text-align:center}.task-date b,.task-date text{display:block}.task-date b{font-size:30rpx;color:#5373c7}.task-date text{font-size:18rpx;color:#95a3aa}.task-main,.plan-main{flex:1;min-width:0}.task-title,.task-sub,.task-purpose,.plan-name,.plan-meta,.plan-tip{display:block}.task-title,.plan-name{font-size:25rpx;font-weight:850}.task-sub,.plan-meta{margin-top:6rpx;font-size:19rpx;color:#8a9ba1}.task-purpose,.plan-tip{margin-top:7rpx;font-size:19rpx;color:#5f9b9b;line-height:1.4}.plan-icon{width:55rpx;height:55rpx;border-radius:17rpx;background:#e7f5f2;color:#0b8a80;display:flex;align-items:center;justify-content:center;font-weight:850}.plan-count b{font-size:28rpx;color:#168d86}.plan-count text{font-size:19rpx;color:#96a5aa}.progress{height:8rpx;margin-top:12rpx;border-radius:8rpx;background:#edf2f3}.progress i{display:block;height:100%;border-radius:8rpx;background:linear-gradient(90deg,#159d91,#6374d5)}
.empty{text-align:center;color:#91a3aa;font-size:22rpx;padding:28rpx;line-height:1.6}.empty-title{display:block;font-size:25rpx;color:#405a64;font-weight:800;margin-bottom:6rpx}
.result-card.safe{background:#f0fbf8}.result-card.danger{background:#fff1ef}.result-badge{padding:10rpx 16rpx;border-radius:999rpx;background:#dff7ef;color:#08806f;font-size:21rpx;font-weight:800}.result-card.danger .result-badge{background:#ffe0dc;color:#d65144}.result-text{display:block;margin-top:14rpx;font-size:24rpx;line-height:1.7;color:#365763}.result-actions{margin-top:16rpx;padding:16rpx;border-radius:18rpx;background:rgba(255,255,255,.7);font-size:21rpx;color:#71858c;line-height:1.6}
.flow-row{display:flex;align-items:center;gap:14rpx;margin-top:18rpx}.flow-row b{width:44rpx;height:44rpx;border-radius:14rpx;background:#e8f5f2;color:#0b8a80;display:flex;align-items:center;justify-content:center}.flow-row text{font-size:24rpx;color:#4d6670}.bottom-space{height:42rpx}
.mask{position:fixed;z-index:99;inset:0;background:rgba(10,31,40,.52);display:flex;align-items:flex-end}.form-sheet{box-sizing:border-box;width:100%;height:88vh;padding:28rpx 30rpx 24rpx;border-radius:35rpx 35rpx 0 0;background:#fff;display:flex;flex-direction:column;overflow:hidden}.form-head{flex:none;display:flex;justify-content:space-between;align-items:center;padding-bottom:10rpx}.form-head text,.form-head b,.form-head em{display:block;font-style:normal}.form-head text{font-size:18rpx;color:#3c86ad;letter-spacing:2rpx}.form-head b{font-size:32rpx;margin-top:4rpx}.form-head em{margin-top:4rpx;font-size:22rpx;color:#83959b}.form-head button{margin:0;background:#f0f4f5;border-radius:16rpx}.form-body{height:calc(88vh - 230rpx);min-height:0}.form-guide{padding:18rpx;border-radius:20rpx;background:#eef8ff;margin:8rpx 0 18rpx}.form-guide b,.form-guide text{display:block}.form-guide b{font-size:24rpx}.form-guide text{margin-top:6rpx;font-size:21rpx;color:#5f7580;line-height:1.6}.form-body label{display:block;margin:24rpx 0 11rpx;font-size:24rpx;font-weight:800}.options{display:flex;flex-wrap:wrap;gap:10rpx}.options text{padding:13rpx 17rpx;border-radius:16rpx;background:#f1f5f6;color:#61777f;font-size:21rpx}.options text.active{background:#def4f0;color:#087f84;font-weight:800}.form-body input{box-sizing:border-box;width:100%;height:82rpx;line-height:82rpx;padding:0 20rpx;border-radius:18rpx;background:#f5f8f9;font-size:25rpx;color:#233f4b}.form-body textarea{box-sizing:border-box;width:100%;height:165rpx;padding:20rpx;border-radius:18rpx;background:#f5f8f9;font-size:24rpx;line-height:1.6;color:#233f4b}.safety-tip{margin-top:22rpx;padding:18rpx;border-radius:17rpx;background:#fff4e4;color:#94611d;font-size:20rpx;line-height:1.6}.safety-tip b,.safety-tip text{display:block}.scroll-space{height:35rpx}.submit{flex:none;margin-top:16rpx;background:linear-gradient(90deg,#078f87,#6976d9);color:#fff;border-radius:20rpx;font-weight:850}
</style>
