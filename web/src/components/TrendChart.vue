<template>
  <div class="tc">
    <svg :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" class="tc-svg"
         @mousemove="onMove" @mouseleave="hover = -1">
      <defs>
        <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="color" stop-opacity="0.28" />
          <stop offset="100%" :stop-color="color" stop-opacity="0.02" />
        </linearGradient>
      </defs>

      <!-- 横向网格 + 左侧刻度 -->
      <g v-for="(g, i) in grid" :key="'g' + i">
        <line :x1="PAD.l" :x2="W - PAD.r" :y1="g.y" :y2="g.y" class="tc-grid" />
        <text :x="PAD.l - 10" :y="g.y + 4" class="tc-tick" text-anchor="end">{{ g.label }}</text>
      </g>

      <!-- 面积 -->
      <path :d="areaPath" :fill="`url(#${gid})`" />
      <!-- 折线 -->
      <path :d="linePath" :stroke="color" fill="none" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
      <!-- 平均值虚线 -->
      <line v-if="average !== null" :x1="PAD.l" :x2="W - PAD.r" :y1="yOf(average)" :y2="yOf(average)"
            class="tc-avg" stroke-dasharray="4 4" />

      <!-- 悬停竖线 -->
      <line v-if="hover >= 0" :x1="xOf(hover)" :x2="xOf(hover)" :y1="PAD.t" :y2="H - PAD.b" class="tc-hover" />

      <!-- 数据点（悬停时高亮当前点） -->
      <circle v-for="(v, i) in values" :key="'p' + i" :cx="xOf(i)" :cy="yOf(v)"
              :r="i === hover ? 5 : 2.5" :fill="i === hover ? '#fff' : color" :stroke="color" stroke-width="2" />

      <!-- 底部日期 -->
      <text v-for="i in labelIdx" :key="'x' + i" :x="xOf(i)" :y="H - PAD.b + 18"
            class="tc-tick" text-anchor="middle">{{ labels[i] }}</text>
    </svg>

    <div v-if="hover >= 0" class="tc-tip" :style="tipStyle">
      <b>{{ labels[hover] }}</b>
      <span><i :style="{ background: color }"></i>{{ format(values[hover]) }}{{ unit }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  labels: { type: Array, default: () => [] },
  values: { type: Array, default: () => [] },
  color: { type: String, default: '#1d4ed8' },
  unit: { type: String, default: '' },
  avg: { type: Number, default: null }
})

const W = 720
const H = 250
const PAD = { l: 58, r: 16, t: 16, b: 30 }
const hover = ref(-1)
const gid = 'g' + Math.random().toString(36).slice(2, 8)

const nums = computed(() => props.values.map((v) => Number(v) || 0))
const vMax = computed(() => (nums.value.length ? Math.max(...nums.value) : 1))
const vMin = computed(() => (nums.value.length ? Math.min(...nums.value) : 0))
// 上下留白
// 刻度取 1/2/5 × 10^n
const nice = computed(() => {
  const span = Math.max(vMax.value - vMin.value, Math.abs(vMax.value) * 0.05, 1)
  const raw = span / 4
  const mag = Math.pow(10, Math.floor(Math.log10(raw)))
  const norm = raw / mag
  const step = (norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10) * mag
  const lo = Math.max(0, Math.floor(vMin.value / step) * step)
  const hi = Math.ceil(vMax.value / step) * step
  return { lo, hi: hi === lo ? lo + step : hi, step }
})
const lo = computed(() => nice.value.lo)
const hi = computed(() => nice.value.hi)

const xOf = (i) => {
  const n = Math.max(props.values.length - 1, 1)
  return PAD.l + (i * (W - PAD.l - PAD.r)) / n
}
const yOf = (v) => {
  const ratio = (Number(v) - lo.value) / (hi.value - lo.value)
  return H - PAD.b - ratio * (H - PAD.t - PAD.b)
}

const linePath = computed(() => nums.value.map((v, i) => `${i ? 'L' : 'M'}${xOf(i)},${yOf(v)}`).join(' '))
const areaPath = computed(() => {
  if (!nums.value.length) return ''
  const bottom = H - PAD.b
  return `${linePath.value} L${xOf(nums.value.length - 1)},${bottom} L${xOf(0)},${bottom} Z`
})

const grid = computed(() => {
  const out = []
  for (let v = lo.value; v <= hi.value + nice.value.step / 2; v += nice.value.step) {
    out.push({ y: yOf(v), label: format(v) })
  }
  return out
})

// 15 个点太密，隔一个显示
const labelIdx = computed(() => {
  const n = props.labels.length
  const step = n > 10 ? 2 : 1
  const out = []
  for (let i = 0; i < n; i += step) out.push(i)
  if (out[out.length - 1] !== n - 1 && n > 1) out.push(n - 1)
  return out
})

const average = computed(() => {
  if (props.avg !== null && props.avg !== undefined) return Number(props.avg)
  if (!nums.value.length) return null
  return nums.value.reduce((a, b) => a + b, 0) / nums.value.length
})

function format(v) {
  const n = Number(v)
  if (!isFinite(n)) return '-'
  if (Math.abs(n) >= 1000) return n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
  return String(Math.round(n * 100) / 100)
}

function onMove(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * W
  const n = Math.max(props.values.length - 1, 1)
  const i = Math.round(((x - PAD.l) / (W - PAD.l - PAD.r)) * n)
  hover.value = Math.min(Math.max(i, 0), props.values.length - 1)
}

const tipStyle = computed(() => {
  const pct = props.values.length > 1 ? hover.value / (props.values.length - 1) : 0
  return { left: `calc(${pct * 100}% - 40px)`, top: '6px' }
})
</script>

<style scoped>
.tc { position: relative; }
.tc-svg { width: 100%; height: 250px; display: block; }
.tc-grid { stroke: #eef2f7; stroke-width: 1; }
.tc-tick { fill: #94a3b8; font-size: 11px; }
.tc-avg { stroke: #cbd5e1; stroke-width: 1; }
.tc-hover { stroke: #cbd5e1; stroke-width: 1; }
.tc-tip {
  position: absolute; background: #0f172a; color: #fff; border-radius: 8px;
  padding: 6px 10px; font-size: 12px; display: grid; gap: 2px; pointer-events: none;
  box-shadow: 0 6px 18px rgba(15, 23, 42, .22);
}
.tc-tip b { font-size: 11px; color: #cbd5e1; font-weight: 500; }
.tc-tip span { display: flex; align-items: center; gap: 6px; }
.tc-tip i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
</style>
