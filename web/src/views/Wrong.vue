<template>
  <main class="wrap">
    <PageHeader title="我的错题本" />

    <div v-if="loading" class="center muted">加载中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>

    <template v-else>
      <section class="panel" style="margin-bottom:16px">
        <div style="display:flex;align-items:center;gap:12px">
          <b style="font-size:14px">按考点聚合</b>
          <span class="muted">共 {{ points.length }} 个考点、{{ totalWrong }} 道未掌握</span>
          <div class="tabs" style="margin-left:auto">
            <button class="tab" :class="{ on: filter === 0 }" @click="load(0)">未掌握</button>
            <button class="tab" :class="{ on: filter === 1 }" @click="load(1)">已掌握</button>
            <button class="tab" :class="{ on: filter === -1 }" @click="load(-1)">全部</button>
          </div>
        </div>
        <div class="points">
          <div v-for="p in points" :key="p.sectionId" class="point">
            <div class="pn">{{ p.sectionName }}</div>
            <div class="pv">错题 <b>{{ p.questionCount }}</b> 道 · 累计错 <b>{{ p.wrongTimes }}</b> 次</div>
          </div>
          <div v-if="!points.length" class="muted">暂无未掌握错题</div>
        </div>
      </section>

      <section v-for="(q, i) in list" :key="q.questionId" class="panel" style="margin-bottom:14px">
        <div style="display:flex;gap:10px;align-items:center;margin-bottom:10px">
          <span class="badge">{{ i + 1 }}</span>
          <span class="badge" style="background:#fef2f2;color:#b91c1c">错 {{ q.wrongCount }} 次</span>
          <span class="muted">{{ q.sectionName }}</span>
          <span v-if="q.mastered" class="badge" style="background:#ecfdf5;color:#059669">已掌握</span>
          <div style="margin-left:auto;display:flex;gap:8px">
            <button v-if="!q.mastered" class="btn ghost" style="padding:6px 12px;font-size:12px"
                    @click="master(q.questionId)">标记已掌握</button>
            <button class="btn ghost" style="padding:6px 12px;font-size:12px;color:#b91c1c"
                    @click="remove(q.questionId)">移出</button>
          </div>
        </div>

        <div class="q">{{ q.name }}</div>
        <div v-for="(o, oi) in q.options || []" :key="oi"
             class="opt" :class="{ ok: letter(oi) === q.answer }">
          <span class="k">{{ letter(oi) }}</span>
          <span>{{ strip(o) }}</span>
        </div>
        <div class="muted" style="margin-top:8px">
          我的答案 <b>{{ q.lastAnswer || '未作答' }}</b> · 正确答案 <b>{{ q.answer }}</b>
        </div>
        <div class="muted" style="margin-top:4px">解析：{{ q.analysis }}</div>
      </section>

      <div v-if="!list.length" class="center muted">这个筛选下没有错题</div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const list = ref([])
const points = ref([])
const filter = ref(0)          // 0 未掌握 / 1 已掌握 / -1 全部
const loading = ref(true)
const error = ref('')

const totalWrong = computed(() => points.value.reduce((s, p) => s + (p.questionCount || 0), 0))
const letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const letter = (i) => letters[i]
const strip = (t) => String(t || '').replace(/^[A-H][.、．]\s*/, '')

async function load(mastered) {
  filter.value = mastered
  loading.value = true
  error.value = ''
  try {
    list.value = (await api.wrongList(mastered === -1 ? undefined : mastered)) || []
    points.value = (await api.wrongPoints()) || []
  } catch (e) {
    error.value = '错题本加载失败：' + e.message
  } finally {
    loading.value = false
  }
}

async function master(questionId) {
  await api.markMastered(questionId)
  await load(filter.value)
}

async function remove(questionId) {
  await api.removeWrong(questionId)
  await load(filter.value)
}

onMounted(() => load(0))
</script>

<style scoped>
.tabs { display: flex; gap: 6px; }
.tab {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
  color: #475569;
}
.tab.on { border-color: #93c5fd; background: #eef2ff; color: var(--brand); font-weight: 600; }
.points { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 14px; }
.point { border: 1px solid var(--line); border-radius: 10px; padding: 12px; background: #fafbfe; }
.pn { font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.pv { font-size: 12px; color: #64748b; }
.pv b { color: #b91c1c; }
</style>
