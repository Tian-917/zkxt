<template>
  <main class="wrap">
    <div v-if="loading" class="center muted" style="padding:60px 0">加载中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>

    <template v-else-if="course">
      <PageHeader title="课程详情" />

      <div class="detail">
        <div class="panel head">
          <div class="cover-big">{{ course.name }}</div>
          <div class="info">
            <h1>{{ course.name }}</h1>
            <p class="muted" style="margin:8px 0 14px">
              共 {{ course.sectionNum || 0 }} 节 · 有效期 {{ course.validDuration || 0 }} 天
            </p>
            <div class="price" style="font-size:15px">
              <b style="font-size:26px">{{ course.price ? fen(course.price) : '免费' }}</b>
              <span class="muted">{{ owned ? '已购买，可直接开始学习' : '购买后可解锁全部课程' }}</span>
            </div>
            <div class="actions">
              <template v-if="owned">
                <button class="btn primary" disabled>已购买</button>
                <button class="btn ghost" @click="$router.push('/learn/' + course.id)">去学习</button>
              </template>
              <template v-else>
                <button class="btn primary" :disabled="busy" @click="buy">
                  {{ busy ? '处理中…' : (course.price ? '立即购买' : '免费报名') }}
                </button>
                <button class="btn ghost" :disabled="carting" @click="addToCart">
                  {{ carting ? '加入中…' : '加入购物车' }}
                </button>
                <button class="btn ghost" @click="$router.push('/learn/' + course.id)">去学习</button>
              </template>
            </div>
            <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'" style="margin-top:12px">{{ msg }}</div>
          </div>
        </div>

        <div class="panel">
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:12px">
            <h3 style="font-size:15px">课程目录</h3>
            <span v-if="!owned" class="muted" style="font-size:12px">前 {{ TRIAL_COUNT }} 节可试看，购买后解锁全部</span>
          </div>
          <div v-for="ch in course.chapters || []" :key="ch.id" style="margin-bottom:12px">
            <div class="chapter">{{ ch.name }}</div>
            <div v-for="s in ch.sections || []" :key="s.id" class="sec"
                 :class="{ locked: !canLearn(s) }" @click="openSection(s)">
              <span class="dot"></span>
              <span style="flex:1">{{ s.name }}</span>
              <span v-if="isTrialSection(s)" class="trial-tag">试看</span>
              <span v-else-if="!owned" class="lock">🔒</span>
              <span class="muted">{{ Math.round((s.mediaDuration || 0) / 60) }}′</span>
            </div>
          </div>
          <div v-if="!(course.chapters || []).length" class="muted">暂无目录</div>
        </div>
      </div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, fen } from '../api'
import { authState } from '../auth'
import { showLoginPrompt } from '../authPrompt'

const route = useRoute()
const router = useRouter()

const TRIAL_COUNT = 2
const course = ref(null)
const owned = ref(false)
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const carting = ref(false)

async function addToCart() {
  if (!requireLogin()) return
  carting.value = true
  try {
    await api.addCart(course.value.id)
    ok.value = true
    msg.value = '已加入购物车'
  } catch (e) {
    ok.value = false
    msg.value = '加入购物车失败：' + e.message
  } finally {
    carting.value = false
  }
}
const msg = ref('')
const ok = ref(false)

// 可试看的小节 id
const trialIds = computed(() => {
  const ids = []
  for (const ch of (course.value?.chapters || [])) {
    for (const s of (ch.sections || [])) {
      if (ids.length < TRIAL_COUNT) ids.push(String(s.id))
    }
  }
  return ids
})

// 以 course_catalogue.trailer 为准，没标记的退回前 N 节
const hasTrailerFlag = computed(() =>
  (course.value?.chapters || []).some((ch) => (ch.sections || []).some((x) => x.trailer === true))
)

function isTrialSection(sec) {
  if (owned.value) return false
  return hasTrailerFlag.value ? sec.trailer === true : trialIds.value.includes(String(sec.id))
}

function canLearn(sec) {
  return owned.value || isTrialSection(sec)
}

function openSection(section) {
  if (canLearn(section)) {
    router.push('/learn/' + course.value.id + (owned.value ? '' : '?trial=1'))
    return
  }
  if (!authState.token) {
    showLoginPrompt(route.fullPath)
    return
  }
  // 已登录但没买：引导购买
  router.push('/order?courseIds=' + course.value.id)
}

function requireLogin() {
  if (!authState.token) {
    showLoginPrompt(route.fullPath)
    return false
  }
  return true
}

async function buy() {
  if (!requireLogin()) return
  busy.value = true
  msg.value = ''
  try {
    if (!course.value.price) {
      // 免费课：直接报名
      await api.enrollFree(course.value.id)
      ok.value = true
      msg.value = '报名成功，已加入我的课表'
    } else {
      // 付费课：进入确认订单页
      router.push('/order?courseIds=' + course.value.id)
    }
  } catch (e) {
    ok.value = false
    msg.value = '操作失败：' + e.message
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    course.value = await api.courseDetail(route.params.id)
    if (authState.token) {
      try {
        const mine = await api.myLesson(route.params.id)
        owned.value = !!(mine && (mine.id || mine.courseId))
      } catch (e) {
        owned.value = false
      }
    }
  } catch (e) {
    error.value = '课程加载失败：' + e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail { display: grid; grid-template-columns: 1fr; gap: 16px; }
.head { display: grid; grid-template-columns: 300px 1fr; gap: 20px; padding: 18px; }
.cover-big {
  height: 180px; border-radius: 12px; color: #fff; font-weight: 600; padding: 16px;
  display: flex; align-items: flex-end;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6 60%, #60a5fa);
}
.info h1 { font-size: 20px; }
.actions { display: flex; gap: 10px; margin-top: 16px; }
.chapter { font-size: 13px; font-weight: 600; color: #334155; margin: 10px 0 6px; }
.sec { display: flex; align-items: center; gap: 8px; padding: 7px 0; font-size: 13px; border-bottom: 1px dashed var(--line); cursor: pointer; }
.sec.locked { opacity: .72; }
.sec:hover { background: #f8fafc; }
.trial-tag {
  font-size: 10px; color: #b45309; background: #fef3c7;
  border: 1px solid #fde68a; border-radius: 4px; padding: 1px 6px; margin-right: 6px;
}
.lock { margin-right: 6px; font-size: 11px; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; }
</style>
