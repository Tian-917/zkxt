<template>
  <div class="dc">
    <svg viewBox="0 0 180 180" class="dc-svg">
      <circle cx="90" cy="90" r="70" fill="none" stroke="#f1f5f9" stroke-width="18" />
      <circle v-for="(seg, i) in segments" :key="i"
              cx="90" cy="90" r="70" fill="none"
              :stroke="seg.color" stroke-width="18"
              :stroke-dasharray="`${seg.len} ${circ - seg.len}`"
              :stroke-dashoffset="-seg.offset"
              transform="rotate(-90 90 90)" stroke-linecap="butt" />
      <text x="90" y="86" text-anchor="middle" class="dc-total">{{ totalText }}</text>
      <text x="90" y="106" text-anchor="middle" class="dc-sub">{{ label }}</text>
    </svg>

    <div class="dc-legend">
      <div v-for="(seg, i) in segments" :key="'l' + i" class="dc-item">
        <i :style="{ background: seg.color }"></i>
        <span class="dc-name" :title="seg.name">{{ seg.name }}</span>
        <span class="dc-pct">{{ seg.pct }}%</span>
      </div>
      <div v-if="!segments.length" class="adm-empty">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  label: { type: String, default: '合计' }
})

const PALETTE = ['#1d4ed8', '#0ea5e9', '#22c55e', '#f59e0b', '#e11d48', '#8b5cf6', '#14b8a6', '#f97316', '#64748b', '#ec4899']
const circ = 2 * Math.PI * 70

const sum = computed(() => props.items.reduce((a, b) => a + (Number(b.value) || 0), 0))

const segments = computed(() => {
  if (!sum.value) return []
  let offset = 0
  return props.items.map((it, i) => {
    const value = Number(it.value) || 0
    const ratio = value / sum.value
    const len = ratio * circ
    const seg = {
      name: it.name,
      color: PALETTE[i % PALETTE.length],
      len,
      offset,
      pct: Math.round(ratio * 100)
    }
    offset += len
    return seg
  })
})

const totalText = computed(() => {
  const n = sum.value
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
})
</script>

<style scoped>
.dc { display: grid; grid-template-columns: 180px 1fr; gap: 16px; align-items: center; }
.dc-svg { width: 180px; height: 180px; }
.dc-total { font-size: 18px; font-weight: 700; fill: #0f172a; }
.dc-sub { font-size: 11px; fill: #94a3b8; }
.dc-legend { display: grid; gap: 7px; }
.dc-item { display: grid; grid-template-columns: 10px 1fr 40px; align-items: center; gap: 8px; font-size: 12px; }
.dc-item i { width: 10px; height: 10px; border-radius: 3px; }
.dc-name { color: #475569; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dc-pct { text-align: right; color: #0f172a; font-weight: 600; }
</style>
