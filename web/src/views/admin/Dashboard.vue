<template>
  <div class="adm-content">
    <!-- 今日核心指标 -->
    <div class="stats">
      <div v-for="k in kpis" :key="k.key" class="stat" :class="k.tone">
        <div class="stat-top">
          <span>{{ k.label }}</span>
          <i class="dot"></i>
        </div>
        <b>{{ k.value }}</b>
        <small>{{ k.unit }}</small>
        <span class="spark">
          <i v-for="(h, i) in k.spark" :key="i" :style="{ height: h + '%' }"></i>
        </span>
      </div>
    </div>

    <!-- 15 天趋势 + 分类占比 -->
    <div class="chart-row">
      <div class="adm-panel chart-main">
        <div class="panel-head">
          <div>
            <b>近 15 天趋势</b>
            <span class="muted">共 {{ trend.labels.length }} 天</span>
          </div>
          <div class="adm-subtabs">
            <button v-for="t in metrics" :key="t.type" class="adm-subtab"
                    :class="{ on: metric === t.type }" @click="metric = t.type">{{ t.label }}</button>
          </div>
        </div>

        <TrendChart v-if="current.series" :labels="trend.labels" :values="current.series.data"
                    :color="current.color" :unit="current.unit" :avg="current.avg" />
        <div v-else class="adm-empty">暂无趋势数据</div>

        <div class="summary">
          <span>最高 <b>{{ current.max }}</b></span>
          <span>最低 <b>{{ current.min }}</b></span>
          <span>日均 <b>{{ current.avgText }}</b></span>
        </div>
      </div>

      <div class="adm-panel chart-side">
        <div class="panel-head"><b>课程订单额占比</b></div>
        <DonutChart :items="courseShare" label="订单总额" />
      </div>
    </div>

    <!-- 热门课程 TOP10 -->
    <div class="adm-panel">
      <div class="panel-head">
        <b>热门课程 TOP10</b>
        <span class="muted">按新增学员排序</span>
      </div>
      <BarList :items="topItems" unit="人" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api'
import TrendChart from '../../components/TrendChart.vue'
import BarList from '../../components/BarList.vue'
import DonutChart from '../../components/DonutChart.vue'

const today = ref({})
const top10 = ref([])
const trend = ref({ labels: [], series: [] })
const metric = ref(1)

const metrics = [
  { type: 1, label: '访问量', color: '#1d4ed8', unit: '次' },
  { type: 2, label: '订单金额', color: '#0ea5e9', unit: '元' },
  { type: 3, label: '订单笔数', color: '#22c55e', unit: '笔' },
  { type: 4, label: '新增学员', color: '#f59e0b', unit: '人' }
]

// 指标卡里的迷你柱状图：用 15 天里最后 8 天做示意
const sparkOf = (type) => {
  const s = trend.value.series.find((x) => x.name && metrics.find((m) => m.label === x.name.split(' ')[0] && m.type === type))
  const data = (s && s.data) || []
  const tail = data.slice(-8)
  const max = Math.max(...tail, 1)
  return tail.map((v) => Math.max(12, Math.round((Number(v) / max) * 100)))
}

const kpis = computed(() => [
  { key: 'visits', label: '今日访问量', value: fmt(today.value.visits), unit: '万次', spark: sparkOf(1), tone: '' },
  { key: 'amount', label: '今日订单额', value: fmt(today.value.orderAmount), unit: '万元', spark: sparkOf(2), tone: 'sky' },
  { key: 'num', label: '今日订单数', value: fmt(today.value.orderNum), unit: '笔', spark: sparkOf(3), tone: 'green' },
  { key: 'stu', label: '今日新增学员', value: fmt(today.value.stuNewNum), unit: '人', spark: sparkOf(4), tone: 'amber' }
])

const current = computed(() => {
  const m = metrics.find((x) => x.type === metric.value) || metrics[0]
  const name = trend.value.labels.length ? metricName(metric.value) : ''
  const s = trend.value.series.find((x) => x.name && x.name.indexOf(name) === 0)
  if (!s) return { series: null }
  const data = s.data.map(Number)
  const max = Math.max(...data)
  const min = Math.min(...data)
  const avg = data.reduce((a, b) => a + b, 0) / data.length
  return {
    series: s,
    color: m.color,
    unit: m.unit,
    avg,
    max: fmt(max) + m.unit,
    min: fmt(min) + m.unit,
    avgText: fmt(Math.round(avg * 100) / 100) + m.unit
  }
})

const topItems = computed(() => top10.value.map((c) => ({ name: c.name, value: c.newStuNum })))

// 课程维度差异更明显
const courseShare = computed(() => {
  const all = top10.value
    .map((c) => ({ name: c.name, value: Number(c.orderAmount) || 0 }))
    .sort((a, b) => b.value - a.value)
  if (all.length <= 6) return all
  const head = all.slice(0, 5)
  const rest = all.slice(5).reduce((a, b) => a + b.value, 0)
  return [...head, { name: '其它课程', value: rest }]
})

function metricName(type) {
  return { 1: '访问量', 2: '订单金额', 3: '订单笔数', 4: '新增学员' }[type] || ''
}

function fmt(v) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (!isFinite(n)) return v
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

onMounted(async () => {
  try {
    today.value = (await api.dashboardToday()) || {}
  } catch (e) {
    today.value = {}
  }
  try {
    const t = await api.dashboardTop10(1)
    top10.value = (t && t.hot) || []
  } catch (e) {
    top10.value = []
  }
  try {
    const b = (await api.boardData('1,2,3,4')) || {}
    const xAxis = (b.xaxis && b.xaxis[0] && b.xaxis[0].data) || []
    trend.value = { labels: Array.isArray(xAxis) ? xAxis : [], series: b.series || [] }
  } catch (e) {
    trend.value = { labels: [], series: [] }
  }
})
</script>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat {
  position: relative; background: #fff; border: 1px solid var(--line); border-radius: 12px;
  padding: 14px 16px 12px; display: grid; gap: 2px; overflow: hidden;
}
.stat-top { display: flex; align-items: center; justify-content: space-between; }
.stat-top span { font-size: 12px; color: #64748b; }
.stat .dot { width: 8px; height: 8px; border-radius: 50%; background: #1d4ed8; }
.stat.sky .dot { background: #0ea5e9; }
.stat.green .dot { background: #22c55e; }
.stat.amber .dot { background: #f59e0b; }
.stat b { font-size: 26px; color: #0f172a; line-height: 1.2; }
.stat small { font-size: 11px; color: #94a3b8; }
.stat .spark { position: absolute; right: 12px; bottom: 10px; display: flex; align-items: flex-end; gap: 3px; height: 34px; opacity: .85; }
.stat .spark i { width: 4px; border-radius: 2px; background: #1d4ed8; display: block; }
.stat.sky .spark i { background: #0ea5e9; }
.stat.green .spark i { background: #22c55e; }
.stat.amber .spark i { background: #f59e0b; }

.chart-row { display: grid; grid-template-columns: minmax(0, 2.1fr) minmax(280px, 1fr); gap: 14px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; gap: 10px; }
.panel-head b { font-size: 14px; }
.panel-head .muted { margin-left: 8px; font-size: 12px; }
.summary { display: flex; gap: 22px; padding-top: 10px; font-size: 12px; color: #64748b; }
.summary b { color: #0f172a; }

@media (max-width: 1200px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
