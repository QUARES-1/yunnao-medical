<template>
  <scroll-view class="page" scroll-y :enhanced="true" :show-scrollbar="false">
    <view class="hero">
      <text class="tag">REPORT DETAIL</text>
      <text class="title">{{ cleanName(report?.itemName) }}</text>
      <text class="sub">{{ report?.type || '检查检验' }} · {{ formatTime(report?.completeTime || report?.createTime) }}</text>
    </view>

    <view class="base-card">
      <view>
        <text>报告编号</text>
        <b>EX{{ String(report?.id || 0).padStart(6, '0') }}</b>
      </view>
      <view>
        <text>开单医生</text>
        <b>{{ report?.doctorName || '医生' }}</b>
      </view>
      <view>
        <text>报告状态</text>
        <b>{{ report?.status || '待检查' }}</b>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <view>
          <text class="section-title">检查结果明细</text>
          <text class="section-desc">逐项查看指标名称、结果、参考范围和判断状态。</text>
        </view>
        <text class="count-pill">{{ rows.length }} 项</text>
      </view>

      <view v-if="rows.length" class="result-table">
        <view v-for="(row, index) in rows" :key="index" class="result-row">
          <view class="name-side">
            <text class="item-index">{{ String(index + 1).padStart(2, '0') }}</text>
            <view>
              <text>{{ row.name }}</text>
              <b v-if="row.code">{{ row.code }}</b>
            </view>
          </view>
          <view class="value-side">
            <view class="value-line">
              <text :class="['value', row.statusClass]">{{ row.value }}</text>
              <em :class="row.statusClass">{{ row.status || '正常' }}</em>
            </view>
            <b>{{ referenceText(row) }}</b>
          </view>
        </view>
      </view>

      <view v-else class="empty-result">
        <text>暂无检查结果</text>
        <b>检验科录入结果后，这里会显示详细指标。</b>
      </view>
    </view>

    <view class="bottom-space"></view>
  </scroll-view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getExaminationDetail } from '@/api/patient'
import type { Examination } from '@/types/api'
import { showError } from '@/utils/request'

interface ResultRow {
  name: string
  code: string
  value: string
  reference: string
  status: string
  statusClass: string
}

const report = ref<Examination | null>(null)

const rows = computed(() => parseResult(report.value?.result || ''))

function cleanName(value?: string) {
  return (value || '检查报告').replace(/^AI演示·/, '')
}

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function parseResult(result: string): ResultRow[] {
  if (!result) return []
  return result
    .split(/[；;\n]/)
    .map(x => x.trim())
    .map(x => x.replace(/[。.]$/, '').trim())
    .filter(Boolean)
    .map(parseLine)
}

function parseLine(line: string): ResultRow {
  const [leftRaw, rightRaw = ''] = line.split(/[:：]/)
  const codeMatch = leftRaw.match(/(.+?)\((.+?)\)/)
  const name = (codeMatch ? codeMatch[1] : leftRaw).trim()
  const code = (codeMatch ? codeMatch[2] : '').trim()
  const segments = rightRaw.split(/[，,]/).map(x => x.trim()).filter(Boolean)
  const value = cleanSegment(segments[0] || rightRaw.trim() || '未填写')
  const referenceSeg = segments.find(x => /参考范围|参考值/.test(x))
  const reference = referenceSeg?.replace(/参考范围|参考值/, '').trim() || ''
  const explicitStatus = segments.find(x => /危急|偏高|偏低|升高|降低|正常|阳性|阴性/.test(x))?.replace(/^状态[:：]?/, '').trim()
  const status = explicitStatus || inferStatus(value)
  return { name, code, value, reference, status: normalizeStatus(status), statusClass: statusClass(status) }
}

function cleanSegment(value: string) {
  return value.replace(/[。.]$/, '').trim()
}

function inferStatus(value: string) {
  if (/\+\+|\+\+\+|阳性/.test(value)) return '阳性'
  if (/阴性/.test(value)) return '正常'
  return '正常'
}

function normalizeStatus(status: string) {
  return status.replace('升高', '偏高').replace('降低', '偏低')
}

function statusClass(status: string) {
  if (status.includes('危急') || status.includes('偏高') || status.includes('升高') || status.includes('阳性')) return 'high'
  if (status.includes('偏低') || status.includes('降低')) return 'low'
  return 'normal'
}

function referenceText(row: ResultRow) {
  if (row.reference) {
    const prefix = /阴性|阳性/.test(row.reference) ? '参考值' : '参考范围'
    return `${prefix} ${row.reference}`
  }
  if (/\+|阳性|阴性/.test(row.value)) return '参考值 阴性'
  return '参考范围 见报告'
}

onLoad(async (query) => {
  const id = Number(query?.id)
  if (!id) return uni.showToast({ title: '报告不存在', icon: 'none' })
  try {
    report.value = await getExaminationDetail(id)
  } catch (e) {
    showError(e)
  }
})
</script>

<style scoped>
.page{height:100vh;box-sizing:border-box;padding:28rpx;background:#f4f8fb;color:#18394b}
.hero{padding:34rpx;border-radius:34rpx;background:linear-gradient(135deg,#138a9b,#37b3a4);color:#fff;box-shadow:0 18rpx 36rpx rgba(22,141,134,.18)}.tag,.title,.sub{display:block}.tag{font-size:19rpx;letter-spacing:3rpx;opacity:.75}.title{font-size:40rpx;font-weight:900;margin:12rpx 0}.sub{font-size:24rpx;line-height:1.7}
.base-card,.section{margin-top:20rpx;padding:26rpx;border-radius:28rpx;background:#fff;box-shadow:0 10rpx 28rpx rgba(29,68,83,.07)}.base-card{display:grid;grid-template-columns:repeat(3,1fr);gap:0}.base-card view{position:relative;text-align:center}.base-card view+view::before{content:'';position:absolute;left:0;top:8rpx;bottom:8rpx;width:1rpx;background:#e9f1f2}.base-card text,.base-card b{display:block}.base-card text{font-size:20rpx;color:#8ba0a6}.base-card b{margin-top:8rpx;font-size:23rpx;color:#284a58}
.section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}.section-title{display:block;font-size:31rpx;font-weight:900;color:#18394b}.section-desc{display:block;margin-top:8rpx;font-size:22rpx;color:#8ba0a6;line-height:1.5}.count-pill{flex-shrink:0;padding:8rpx 18rpx;border-radius:999rpx;background:#e9fbf8;color:#0c8f87;font-size:22rpx;font-weight:900}
.result-table{margin-top:22rpx;display:flex;flex-direction:column;gap:18rpx}.result-row{display:flex;align-items:center;gap:18rpx;padding:20rpx;border:1rpx solid #e5eff1;border-radius:24rpx;background:linear-gradient(180deg,#fff,#fbfefe)}.name-side{width:43%;box-sizing:border-box;display:flex;align-items:center;gap:14rpx}.item-index{display:flex!important;align-items:center;justify-content:center;width:48rpx;height:48rpx;border-radius:16rpx;background:#eaf8f7;color:#0c8f87;font-size:20rpx;font-weight:900}.name-side text,.name-side b{display:block}.name-side view text{font-size:25rpx;font-weight:900;color:#244655}.name-side b{margin-top:6rpx;font-size:20rpx;color:#8ba0a6}.value-side{flex:1;box-sizing:border-box;text-align:right}.value-line{display:flex;align-items:center;justify-content:flex-end;gap:12rpx}.value-side text,.value-side b,.value-side em{display:block}.value{font-size:28rpx;font-weight:900}.value-side b{margin-top:8rpx;font-size:20rpx;color:#8ba0a6;font-weight:500}.value-side em{padding:5rpx 12rpx;border-radius:999rpx;font-size:20rpx;font-style:normal;font-weight:850;background:#eef8f7}.value-side em.high{background:#fff0ee}.value-side em.low{background:#edf5ff}.high{color:#dc5b4c}.low{color:#2d78c8}.normal{color:#0c8f87}
.empty-result{margin-top:22rpx;padding:36rpx;border-radius:22rpx;background:#f7fbfc;text-align:center}.empty-result text,.empty-result b{display:block}.empty-result text{font-size:28rpx;font-weight:900}.empty-result b{margin-top:8rpx;font-size:22rpx;color:#8ba0a6}
.bottom-space{height:40rpx}
</style>
