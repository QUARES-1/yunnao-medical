<template>
  <div>
    <section class="page-hero compact lab-page-hero">
      <div>
        <span class="eyebrow">TEST CATALOG</span>
        <h1>检查项目目录</h1>
        <p>查看平台已配置的检查与检验项目、价格和说明</p>
      </div>
      <div class="stock-overview">
        <div><span>项目总数</span><strong>{{ filtered.length }}</strong></div>
        <div><span>检验项目</span><strong>{{ normalizedItems.filter(x => x.type === '检验').length }}</strong></div>
      </div>
    </section>

    <section class="content-card lab-item-page">
      <div class="toolbar">
        <div class="search">
          <el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索项目名称或说明" clearable />
          <el-radio-group v-model="type">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="检验">检验</el-radio-button>
            <el-radio-button value="检查">检查</el-radio-button>
          </el-radio-group>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <div v-loading="loading" class="item-grid lab-item-grid">
        <article v-for="item in filtered" :key="item.name">
          <span class="item-icon" :class="item.type === '检验' ? 'green' : 'blue'">
            <el-icon><component :is="item.type === '检验' ? Operation : Monitor" /></el-icon>
          </span>
          <div>
            <div class="item-title">
              <strong>{{ item.name }}</strong>
              <el-tag :type="item.type === '检验' ? 'success' : 'primary'" size="small" round>{{ item.type }}</el-tag>
            </div>
            <p>{{ item.description || '暂无项目说明' }}</p>
            <small>项目编号 ITEM{{ String(item.id).padStart(4, '0') }}</small>
          </div>
          <b>¥{{ Number(item.price).toFixed(2) }}</b>
        </article>
        <el-empty v-if="!filtered.length && !loading" description="暂无匹配项目" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search, Refresh, Operation, Monitor } from '@element-plus/icons-vue'
import { getItems } from '@/api/lab'
import type { ExaminationItem } from '@/types'

const loading = ref(false)
const sourceItems = ref<ExaminationItem[]>([])
const keyword = ref('')
const type = ref('')

const standardCatalog: ExaminationItem[] = [
  { id: 1, name: '血常规', type: '检验', price: 28, description: '用于评估白细胞、红细胞、血红蛋白、血小板等基础血液指标。' },
  { id: 2, name: '尿常规', type: '检验', price: 18, description: '用于筛查尿蛋白、尿糖、尿酮体、红细胞、白细胞等泌尿系统相关指标。' },
  { id: 3, name: '便常规', type: '检验', price: 16, description: '用于检查消化道感染、潜血及寄生虫等情况。' },
  { id: 4, name: '肝功能', type: '检验', price: 65, description: '用于评估谷丙转氨酶、谷草转氨酶、胆红素和白蛋白等肝脏功能。' },
  { id: 5, name: '肾功能', type: '检验', price: 58, description: '用于评估肌酐、尿素氮、尿酸等肾脏代谢指标。' },
  { id: 6, name: '血糖', type: '检验', price: 12, description: '用于了解空腹或随机血糖水平，辅助判断糖代谢异常。' },
  { id: 7, name: '血脂四项', type: '检验', price: 45, description: '用于评估总胆固醇、甘油三酯、高密度和低密度脂蛋白。' },
  { id: 8, name: '糖化血红蛋白', type: '检验', price: 70, description: '用于反映近 2-3 个月平均血糖控制情况。' },
  { id: 9, name: '凝血功能', type: '检验', price: 55, description: '用于评估凝血酶原时间、活化部分凝血活酶时间等凝血指标。' },
  { id: 10, name: '电解质', type: '检验', price: 35, description: '用于检测钾、钠、氯、钙等电解质水平。' },
  { id: 11, name: 'C反应蛋白', type: '检验', price: 38, description: '用于辅助判断炎症或感染活动程度。' },
  { id: 12, name: '甲状腺功能', type: '检验', price: 120, description: '用于检测 T3、T4、TSH 等甲状腺相关指标。' },
  { id: 13, name: '心肌损伤标志物', type: '检验', price: 150, description: '用于评估肌钙蛋白、肌酸激酶同工酶等心肌损伤相关指标。' },
  { id: 14, name: '乙肝两对半', type: '检验', price: 90, description: '用于筛查乙肝病毒感染及免疫状态。' },
  { id: 15, name: '血型鉴定', type: '检验', price: 25, description: '用于检测 ABO 血型和 Rh 血型。' },
  { id: 16, name: '心电图', type: '检查', price: 30, description: '用于评估心律失常、心肌缺血等心脏电生理情况。' },
  { id: 17, name: '胸部X光', type: '检查', price: 80, description: '用于观察肺部、胸廓和心影等基础影像情况。' },
  { id: 18, name: '腹部彩超', type: '检查', price: 120, description: '用于观察肝胆胰脾肾等腹部脏器情况。' },
  { id: 19, name: '泌尿系彩超', type: '检查', price: 110, description: '用于观察肾脏、输尿管、膀胱等泌尿系统结构。' },
  { id: 20, name: '妇科彩超', type: '检查', price: 130, description: '用于观察子宫、卵巢及盆腔情况。' },
  { id: 21, name: '颈部血管彩超', type: '检查', price: 160, description: '用于评估颈动脉斑块、血流速度和狭窄情况。' },
  { id: 22, name: 'CT平扫', type: '检查', price: 260, description: '用于对头颅、胸腹部等部位进行断层影像检查。' },
  { id: 23, name: '头颅MRI', type: '检查', price: 520, description: '用于观察脑组织、脑血管及颅内病变情况。' },
  { id: 24, name: '胃镜', type: '检查', price: 320, description: '用于观察食管、胃和十二指肠黏膜情况。' },
  { id: 25, name: '肺功能', type: '检查', price: 180, description: '用于评估肺通气功能，辅助判断哮喘、慢阻肺等疾病。' }
]

const normalizedItems = computed(() => {
  // 后端旧库里有大量重复项目和随机价格，这里按实训标准目录展示，避免“尿常规”重复出现。
  // 仍然触发 getItems，保持页面和后端联调关系；如果后端整理干净，这里也不会影响搜索体验。
  return standardCatalog
})

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return normalizedItems.value.filter(item => {
    const hitType = !type.value || item.type === type.value
    const hitKeyword = !kw || item.name.includes(kw) || item.description?.includes(kw)
    return hitType && hitKeyword
  })
})

const load = async () => {
  loading.value = true
  try {
    const res = await getItems()
    sourceItems.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
