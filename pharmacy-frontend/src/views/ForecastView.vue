<template>
  <div class="forecast-page">
    <section class="forecast-hero">
      <div>
        <span class="eyebrow">AI STOCK FORECAST</span>
        <h1>AI 药品库存智能预测</h1>
        <p>融合历史消耗、季节因素与处方量趋势，重点预测未来 1 个月（30天）需求并生成采购建议。</p>
        <div class="period-badge">预测周期：未来 1 个月（30 天）</div>
        <div class="forecast-actions">
          <el-date-picker v-model="period" type="month" value-format="YYYY-MM" format="YYYY年MM月" placeholder="选择预测月份"/>
          <el-button type="primary" :loading="generating" :icon="MagicStick" @click="generate">生成新预测</el-button>
          <el-button class="stream-btn" :loading="pharmacyStore.streaming" @click="startStreamReport">AI流式生成报告</el-button>
        </div>
      </div>
      <div class="ai-orbit"><el-icon><TrendCharts/></el-icon><strong>未来 1 个月</strong><span>30 DAYS FORECAST</span></div>
    </section>

    <section class="forecast-card stream-card" v-if="detail || pharmacyStore.streamText">
      <div class="forecast-card-head">
        <div>
          <span class="eyebrow">SSE STREAMING</span>
          <h2>AI库存预测流式预览</h2>
          <p>后端通过 SSE 实时推送 AI 预测内容，页面会逐字展示生成过程。</p>
        </div>
        <el-button size="small" :loading="pharmacyStore.streaming" @click="startStreamReport">{{ pharmacyStore.streamText ? '重新生成' : '开始生成' }}</el-button>
      </div>
      <pre class="stream-output">{{ pharmacyStore.streamText || '点击“开始生成”，AI会根据当前库存预测逐字生成采购建议。' }}</pre>
    </section>

    <template v-if="detail">
      <section class="forecast-summary">
        <article><span>预测药品</span><strong>{{ rows.length }}</strong><small>覆盖全部在库药品</small></article>
        <article><span>未来1个月预计消耗金额</span><strong>¥{{ money(detail.totalForecastAmount) }}</strong><small>{{ detail.forecastPeriod }} · 30天预测</small></article>
        <article><span>建议采购金额</span><strong>¥{{ money(detail.totalPurchaseAmount) }}</strong><small>按安全库存测算</small></article>
        <article class="risk"><span>需处理药品</span><strong>{{ riskCount }}</strong><small>缺货或积压风险</small></article>
      </section>

      <section class="forecast-card">
        <div class="forecast-card-head">
          <div><span class="eyebrow">MEDICINE FORECAST</span><h2>逐药预测与采购清单</h2><p>预测生成于 {{ formatTime(detail.createTime) }}，点击风险标签可快速筛选。</p></div>
          <div class="forecast-filters">
            <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索药品名称"/>
            <el-select v-model="riskFilter" clearable placeholder="全部风险">
              <el-option v-for="item in ['高风险','需补货','库存合理','可能积压']" :key="item" :label="item" :value="item"/>
            </el-select>
          </div>
        </div>
        <el-table :data="filteredRows" v-loading="loading" class="pharmacy-table forecast-table">
          <el-table-column label="药品" min-width="200">
            <template #default="{row}"><div class="forecast-medicine"><div><strong>{{row.name}}</strong><small>{{row.specification||'规格未录入'}} · {{row.categoryName||'未分类'}}</small></div></div></template>
          </el-table-column>
          <el-table-column label="历史消耗" min-width="125"><template #default="{row}"><strong>{{row.history30}}</strong> {{row.unit}}<small class="sub-cell">近90天 {{row.history90}}</small></template></el-table-column>
          <el-table-column label="消耗趋势" min-width="145"><template #default="{row}"><span :class="row.trendRate>0?'trend-up':'trend-down'">{{row.trendRate>0?'↑':'↓'}} {{Math.abs(row.trendRate)}}%</span><small class="sub-cell">季节系数 {{row.seasonFactor}}</small></template></el-table-column>
          <el-table-column label="未来30天预测" min-width="140"><template #default="{row}"><strong class="forecast-number">{{row.forecastConsume}}</strong> {{row.unit}}<small class="sub-cell">安全库存 {{row.safetyStock}}</small></template></el-table-column>
          <el-table-column label="当前库存" min-width="135"><template #default="{row}"><strong>{{row.currentStock}}</strong> {{row.unit}}<small class="sub-cell">{{row.stockCoverageDays>=999?'暂无稳定消耗':`约可用 ${row.stockCoverageDays} 天`}}</small></template></el-table-column>
          <el-table-column label="采购建议" min-width="135"><template #default="{row}"><strong :class="{purchase:row.suggestPurchase>0}">{{row.suggestPurchase>0?`采购 ${row.suggestPurchase}`:'暂不采购'}}</strong><small class="sub-cell">{{row.unit}}</small></template></el-table-column>
          <el-table-column label="风险" width="105"><template #default="{row}"><el-tag :type="tagType(row.riskLevel)" effect="light">{{row.riskLevel}}</el-tag></template></el-table-column>
        </el-table>
      </section>

      <section class="forecast-card purchase-card">
        <div class="forecast-card-head">
          <div>
            <span class="eyebrow">PURCHASE LIST</span>
            <h2>AI 采购清单</h2>
            <p>只展示需要采购的药品，左侧为药品名称，右侧为建议采购量。</p>
          </div>
          <el-tag type="success">未来1个月</el-tag>
        </div>
        <el-table :data="purchaseRows" class="pharmacy-table purchase-table">
          <el-table-column label="药品名称" min-width="260">
            <template #default="{row}">
              <div class="purchase-medicine">
                <strong>{{row.name}}</strong>
                <small>{{row.specification||'规格未录入'}} · {{row.categoryName||'未分类'}}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="当前库存" min-width="120">
            <template #default="{row}">{{row.currentStock}} {{row.unit}}</template>
          </el-table-column>
          <el-table-column label="未来30天预测" min-width="130">
            <template #default="{row}">{{row.forecastConsume}} {{row.unit}}</template>
          </el-table-column>
          <el-table-column label="建议采购量" min-width="150" align="right">
            <template #default="{row}">
              <strong class="purchase-amount">{{row.suggestPurchase}} {{row.unit}}</strong>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!purchaseRows.length" description="本周期暂无需要采购的药品" :image-size="70"/>
      </section>
    </template>

    <section v-else class="forecast-card empty-forecast" v-loading="loading">
      <el-empty description="暂无库存预测，选择月份后生成第一份预测"/>
    </section>
  </div>
</template>

<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {MagicStick,Search,TrendCharts} from '@element-plus/icons-vue'
import {generateStockForecast,getStockForecast,getStockForecastList} from '@/api/pharmacy'
import {usePharmacyStore} from '@/stores/pharmacy'
import type {ForecastMedicine,StockForecast} from '@/types'

const nextMonth=()=>{const date=new Date();date.setMonth(date.getMonth()+1);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}`}
const pharmacyStore=usePharmacyStore()
const period=ref(nextMonth()),generating=ref(false),loading=ref(false),detail=ref<StockForecast>(),history=ref<StockForecast[]>([]),keyword=ref(''),riskFilter=ref('')
const parseArray=<T,>(value?:string):T[]=>{try{return JSON.parse(value||'[]')}catch{return[]}}
const rows=computed(()=>parseArray<ForecastMedicine>(detail.value?.forecastData))
const filteredRows=computed(()=>rows.value.filter(item=>(!keyword.value||item.name.includes(keyword.value))&&(!riskFilter.value||item.riskLevel===riskFilter.value)))
const purchaseRows=computed(()=>rows.value.filter(item=>Number(item.suggestPurchase||0)>0).sort((a,b)=>Number(b.suggestPurchase||0)-Number(a.suggestPurchase||0)))
const riskCount=computed(()=>rows.value.filter(item=>item.riskLevel!=='库存合理').length)
const money=(value?:number)=>Number(value||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})
const formatTime=(value:string)=>new Date(value).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'})
const tagType=(risk:string)=>risk==='高风险'?'danger':risk==='需补货'?'warning':risk==='可能积压'?'info':'success'
const openDetail=async(id:number)=>{loading.value=true;try{detail.value=(await getStockForecast(id)).data.data}finally{loading.value=false}}
const loadHistory=async()=>{loading.value=true;try{history.value=(await getStockForecastList(1,10)).data.data.content;if(!detail.value&&history.value[0])await openDetail(history.value[0].id)}finally{loading.value=false}}
const generate=async()=>{if(!period.value)return ElMessage.warning('请选择预测月份');generating.value=true;try{const result=(await generateStockForecast(period.value)).data.data;await loadHistory();await openDetail(result.id);pharmacyStore.clearStream();ElMessage.success(`已生成 ${result.forecastPeriod} 库存预测，共分析 ${result.medicineCount} 种药品`)}finally{generating.value=false}}
const startStreamReport=async()=>{if(!detail.value)return ElMessage.warning('请先生成或加载一份库存预测');await pharmacyStore.streamForecastReport({period:detail.value.forecastPeriod,medicines:rows.value,totalForecastAmount:detail.value.totalForecastAmount,totalPurchaseAmount:detail.value.totalPurchaseAmount})}
onMounted(loadHistory)
</script>

<style scoped>
.forecast-page{display:flex;flex-direction:column;gap:18px}.forecast-hero{position:relative;display:flex;justify-content:space-between;align-items:center;min-height:210px;padding:35px 42px;border-radius:22px;overflow:hidden;background:linear-gradient(115deg,#073f4b,#08746f 58%,#13a18b);color:#fff;box-shadow:0 18px 40px rgba(17,79,84,.13)}.forecast-hero:after{content:"";position:absolute;width:310px;height:310px;border:1px solid rgba(255,255,255,.12);border-radius:50%;right:-75px;top:-115px;box-shadow:0 0 0 50px rgba(255,255,255,.035)}.forecast-hero>div{position:relative;z-index:2}.forecast-hero .eyebrow{color:#5de1cc}.forecast-hero h1{margin:12px 0 10px;font-size:30px}.forecast-hero p{margin:0;color:rgba(255,255,255,.7);font-size:11px}.period-badge{display:inline-flex;align-items:center;margin-top:14px;padding:8px 13px;border:1px solid rgba(255,255,255,.24);border-radius:999px;background:rgba(255,255,255,.12);font-size:12px;font-weight:800;letter-spacing:.4px}.forecast-actions{display:flex;gap:10px;margin-top:20px}.forecast-actions :deep(.el-input__wrapper){height:40px;border:0;border-radius:10px}.forecast-actions .el-button{height:40px;border:0;border-radius:10px;background:#fff;color:#087d71;font-weight:700}.forecast-actions .stream-btn{background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.35);color:#fff}.ai-orbit{display:grid;place-items:center;width:150px;height:150px;border:1px solid rgba(255,255,255,.22);border-radius:50%;margin-right:32px;box-shadow:0 0 0 24px rgba(255,255,255,.035),0 0 0 48px rgba(255,255,255,.02)}.ai-orbit .el-icon{font-size:50px;color:#7de7d4}.ai-orbit strong{font-size:16px;line-height:1;color:#fff}.ai-orbit span{font-size:8px;letter-spacing:2px;color:rgba(255,255,255,.65)}.forecast-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:15px}.forecast-summary article{padding:20px 22px;border:1px solid #e0ebeb;border-radius:17px;background:#fff;box-shadow:0 8px 22px rgba(25,73,78,.035)}.forecast-summary span,.forecast-summary strong,.forecast-summary small{display:block}.forecast-summary span{color:#81969b;font-size:9px}.forecast-summary strong{margin:7px 0 5px;font-size:23px}.forecast-summary small{color:#a2b1b4;font-size:8px}.forecast-summary .risk strong{color:#de7566}.forecast-card{padding:23px;border:1px solid #e0ebeb;border-radius:18px;background:#fff;box-shadow:0 10px 28px rgba(23,67,72,.045)}.forecast-card-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.forecast-card-head h2{margin:5px 0 0;font-size:16px}.forecast-card-head p{margin:6px 0 0;color:#91a3a7;font-size:9px}.forecast-filters{display:flex;gap:10px}.forecast-filters .el-input{width:220px}.forecast-filters .el-select{width:130px}.forecast-table :deep(.el-table__cell),.purchase-table :deep(.el-table__cell){vertical-align:middle}.forecast-medicine{display:flex;align-items:center;gap:11px}.forecast-medicine strong,.forecast-medicine small,.sub-cell,.purchase-medicine strong,.purchase-medicine small{display:block}.forecast-medicine small,.sub-cell,.purchase-medicine small{margin-top:4px;color:#97a8ac;font-size:8px}.forecast-number,.purchase,.purchase-amount{color:#078e80}.purchase-card{margin-bottom:20px}.purchase-medicine strong{font-size:13px;color:#17373e}.purchase-amount{font-size:15px}.trend-up{color:#e06f5f;font-weight:700}.trend-down{color:#2a9b7d;font-weight:700}.reason{color:#607b81;font-size:9px;line-height:1.7}.insight-grid{display:grid;grid-template-columns:1.15fr .85fr;gap:18px}.advice-list>div{display:grid;grid-template-columns:35px 1fr;gap:12px;align-items:start;padding:13px 0;border-top:1px solid #eaf0f0}.advice-list span{display:grid;place-items:center;width:31px;height:31px;border-radius:10px;background:#e4f6f2;color:#078d80;font-size:9px;font-weight:800}.advice-list p{margin:4px 0;color:#526f75;font-size:10px;line-height:1.7}.factor-list{display:flex;flex-wrap:wrap;gap:9px}.factor-list span{padding:8px 11px;border-radius:9px;background:#edf8f6;color:#268276;font-size:9px}.model-note{display:flex;gap:10px;margin-top:18px;padding:14px;border-radius:12px;background:#f5f8f8;color:#7c9196}.model-note .el-icon{flex:0 0 auto;color:#0a9385}.model-note p{margin:0;font-size:9px;line-height:1.7}.stream-card{border-color:#bdece5;background:linear-gradient(180deg,#ffffff,#f4fffd)}.stream-output{min-height:170px;max-height:310px;overflow:auto;margin:0;padding:18px;border-radius:14px;background:#073f4b;color:#e9fffb;font-size:14px;line-height:1.9;white-space:pre-wrap;font-family:'Microsoft YaHei',Consolas,monospace;box-shadow:inset 0 0 0 1px rgba(255,255,255,.08)}.empty-forecast{min-height:250px}@media(max-width:1250px){.forecast-summary{grid-template-columns:repeat(2,1fr)}.insight-grid{grid-template-columns:1fr}}
.purchase-card{
  max-width:960px;
  margin:0 auto 20px;
  padding:20px 22px;
}
.purchase-card .forecast-card-head{
  margin-bottom:12px;
}
.purchase-table :deep(.el-table__header th){
  height:38px;
}
.purchase-table :deep(.el-table__body td){
  padding:8px 0!important;
}
.purchase-medicine strong{
  font-size:15px;
}
.purchase-medicine small{
  font-size:12px;
}
.purchase-amount{
  font-size:18px;
}
</style>






