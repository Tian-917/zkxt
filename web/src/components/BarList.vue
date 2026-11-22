<template>
  <div class="bl">
    <div v-for="(it, i) in items" :key="i" class="bl-row">
      <span class="bl-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
      <span class="bl-name" :title="it.name">{{ it.name }}</span>
      <span class="bl-track">
        <i :style="{ width: pct(it.value) + '%', background: colorOf(i) }"></i>
      </span>
      <span class="bl-val">{{ format(it.value) }}<em>{{ unit }}</em></span>
    </div>
    <div v-if="!items.length" class="adm-empty">暂无数据</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  unit: { type: String, default: '' }
})

const max = computed(() => {
  const m = Math.max(...props.items.map((i) => Number(i.value) || 0), 1)
  return m
})

const PALETTE = ['#1d4ed8', '#2563eb', '#3b82f6', '#60a5fa', '#93c5fd', '#0ea5e9', '#38bdf8', '#7dd3fc', '#a5b4fc', '#c7d2fe']
const colorOf = (i) => PALETTE[Math.min(i, PALETTE.length - 1)]
const pct = (v) => Math.max(4, Math.round(((Number(v) || 0) / max.value) * 100))

function format(v) {
  const n = Number(v) || 0
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}
</script>

<style scoped>
.bl { display: grid; gap: 2px; }
.bl-row {
  display: grid; grid-template-columns: 22px 1fr 42% 76px; align-items: center;
  gap: 10px; padding: 7px 0; border-bottom: 1px dashed var(--line); font-size: 13px;
}
.bl-row:last-child { border-bottom: none; }
.bl-rank { color: #94a3b8; font-size: 12px; text-align: center; }
.bl-rank.top { color: #e11d48; font-weight: 700; }
.bl-name { color: #334155; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bl-track { background: #f1f5f9; border-radius: 999px; height: 10px; overflow: hidden; }
.bl-track i { display: block; height: 100%; border-radius: 999px; transition: width .4s ease; }
.bl-val { text-align: right; color: #0f172a; font-weight: 600; }
.bl-val em { font-style: normal; font-size: 11px; color: #94a3b8; margin-left: 3px; font-weight: 400; }
</style>
