<template>
  <main class="wrap">
    <PageHeader title="课包目录" />

    <div v-if="loading" class="center muted">加载中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>
    <div v-else class="layout">
      <div v-if="!owned" class="trial-bar">
        <span class="tb-tag">试看中</span>
        <span>前 {{ TRIAL_COUNT }} 节可免费学习，购买后可解锁全部 {{ totalSections }} 节</span>
        <button class="btn primary" style="margin-left:auto;padding:6px 14px;font-size:12px"
                @click="$router.push('/order?courseIds=' + route.params.courseId)">购买课包</button>
      </div>
      <aside class="panel">
        <div v-for="ch in chapters" :key="ch.id">
          <div class="chapter">{{ ch.name }}</div>
          <div
            v-for="s in ch.sections" :key="s.id"
            class="lesson" :class="{ active: current && current.id === s.id, locked: !canLearn(s) }"
            @click="selectSection(s)">
            <span class="dot" :class="{ done: isFinished(s.id) }"></span>
            <span style="flex:1">{{ s.name }}</span>
            <span v-if="isTrialSection(s)" class="trial-tag">试看</span>
            <span v-else-if="!owned" class="lock">🔒</span>
            <span v-if="isFinished(s.id)" style="color:#059669;font-size:11px">已学完</span>
            <span class="muted">{{ Math.round((s.mediaDuration || 0) / 60) }}′</span>
          </div>
        </div>
      </aside>

      <section class="panel">
        <template v-if="current">
          <h3 style="font-size:16px;margin-bottom:6px">{{ current.name }}</h3>
          <p class="muted" style="margin-bottom:14px">
            视频时长 {{ Math.round((current.mediaDuration || 0) / 60) }} 分钟 ·
            看满 80%（{{ Math.ceil((current.mediaDuration || 0) * 0.8 / 60) }} 分钟）即计为学完
          </p>

          <div class="stage">
            <div class="stage-ttl">
              {{ playing ? '正在播放' : '已暂停' }}
              <small>{{ clock(moment) }} / {{ clock(duration) }}</small>
            </div>
            <div class="stage-play" @click="toggle">
              <span v-if="playing" class="pause"><i></i><i></i></span>
              <span v-else class="tri"></span>
            </div>
            <div class="stage-bar"><i :style="{ width: percent + '%' }"></i></div>
          </div>

          <div class="row" style="margin-top:14px">
            <button class="btn primary" @click="toggle">{{ playing ? '暂停' : '播放' }}</button>
            <div class="speeds">
              <span class="muted">播放倍速</span>
              <button v-for="s in speeds" :key="s" class="speed" :class="{ on: speed === s }"
                      @click="speed = s">{{ s }}x</button>
            </div>
            <span class="badge" :class="{ green: finished }">{{ finished ? '已学完' : '学习中' }}</span>
            <span class="muted" style="margin-left:auto">已观看 {{ clock(moment) }} / {{ percent }}%</span>
          </div>

          <div class="row">
            <button class="btn ghost" @click="startExam">做本节练习</button>
            <span class="muted">学完本节可做练习题，交卷后错题自动收进错题本</span>
          </div>

          <div v-if="!owned" class="trial-note">
            <span>🔒 购买课包后可参与笔记与问答、提交练习、累计学习进度</span>
            <button class="btn primary" style="padding:5px 12px;font-size:12px"
                    @click="$router.push('/order?courseIds=' + route.params.courseId)">购买解锁</button>
          </div>
          <div v-else class="lesson-tabs">
            <button class="ltab" :class="{ on: lessonTab === 'notes' }" @click="lessonTab = 'notes'">笔记</button>
            <button class="ltab" :class="{ on: lessonTab === 'qa' }" @click="lessonTab = 'qa'">问答</button>
          </div>
          <div v-if="owned" class="lesson-panel">
            <NotesPanel
              v-if="lessonTab === 'notes'"
              :key="'n-' + current.id"
              :course-id="route.params.courseId"
              :chapter-id="currentChapterId"
              :section-id="current.id" />
            <QaPanel
              v-else
              :key="'q-' + current.id"
              :course-id="route.params.courseId"
              :chapter-id="currentChapterId"
              :section-id="current.id" />
          </div>

          <div v-if="reportHint" class="notice" :class="finished ? 'ok' : ''" style="margin-top:12px">
            {{ reportHint }}
          </div>
          <div v-if="!lessonId" class="notice" style="margin-top:12px;background:#fffbeb;color:#b45309;border:1px solid #fde68a">
            当前账号未报名该课包，无法上报学习进度（先在小节上点一次"报名"或换用已报名账号）
          </div>
        </template>
        <div v-else class="center muted">从左侧选择一个考点开始</div>
      </section>
    </div>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { authState } from '../auth'
import { showLoginPrompt } from '../authPrompt'
import NotesPanel from './learn/NotesPanel.vue'
import QaPanel from './learn/QaPanel.vue'

const route = useRoute()
const router = useRouter()

const chapters = ref([])
const current = ref(null)
const loading = ref(true)
const error = ref('')
const lessonId = ref(null)
const TRIAL_COUNT = 2
const owned = ref(false)            // 是否已购买该课包
const records = ref({})            // sectionId -> { moment, finished }
const moment = ref(0)
const playing = ref(false)
const speed = ref(60)              // 默认 60 倍速：35 分钟的课 35 秒看完
const speeds = [1, 10, 60]
const lessonTab = ref('notes')      // 小节下的 tab：笔记 / 问答
// 前 N 节可试看（按目录顺序）
const trialIds = computed(() => {
  const ids = []
  for (const ch of chapters.value) {
    for (const s of (ch.sections || [])) {
      if (ids.length < TRIAL_COUNT) ids.push(String(s.id))
    }
  }
  return ids
})
const totalSections = computed(() =>
  chapters.value.reduce((n, ch) => n + (ch.sections || []).length, 0)
)
// 试看口径以课程目录的 trailer 字段为准（后端 course_catalogue.trailer）；
// 老课程若一条 trailer 都没标，则退回"前 N 节"的兜底规则
const hasTrailerFlag = computed(() =>
  chapters.value.some((ch) => (ch.sections || []).some((x) => x.trailer === true))
)
const isTrialSection = (sec) => {
  if (owned.value) return false
  return hasTrailerFlag.value ? sec.trailer === true : trialIds.value.includes(String(sec.id))
}
const isTrial = (id) => {
  const sec = findSection(id)
  return sec ? isTrialSection(sec) : false
}
const canLearn = (sec) => owned.value || isTrialSection(sec)

function findSection(id) {
  for (const ch of chapters.value) {
    const hit = (ch.sections || []).find((x) => String(x.id) === String(id))
    if (hit) return hit
  }
  return null
}

const currentChapterId = computed(() => {
  if (!current.value) return ''
  const ch = chapters.value.find((c) => (c.sections || []).some((x) => x.id === current.value.id))
  return ch ? ch.id : ''
})
const reportHint = ref('')

let timer = null
let lastReportedMoment = -999

const duration = computed(() => (current.value && current.value.mediaDuration) || 0)
const percent = computed(() =>
  duration.value ? Math.min(100, Math.round((moment.value / duration.value) * 100)) : 0
)
const finished = computed(() => !!(current.value && records.value[current.value.id]?.finished))

function isFinished(sectionId) {
  return !!(records.value[sectionId] && records.value[sectionId].finished)
}

function clock(seconds) {
  const s = Math.max(0, Math.round(seconds || 0))
  const m = Math.floor(s / 60)
  const r = s % 60
  return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`
}

function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function loadProgress() {
  try {
    const data = await api.courseProgress(route.params.courseId)
    if (data) {
      lessonId.value = data.id
      const map = {}
      ;(data.records || []).forEach((r) => {
        map[r.sectionId] = { moment: r.moment || 0, finished: !!r.finished }
      })
      records.value = map
    }
  } catch (e) {
    lessonId.value = null
  }
}

onMounted(async () => {
  try {
    const data = await api.catas(route.params.courseId)
    chapters.value = data || []
    if (authState.token) {
      try {
        const mine = await api.myLesson(route.params.courseId)
        owned.value = !!(mine && (mine.id || mine.courseId))
      } catch (e) {
        owned.value = false
      }
    }
    const first = chapters.value.find((c) => c.sections && c.sections.length)
    if (first) selectSection(first.sections[0])
    if (owned.value) await loadProgress()
  } catch (e) {
    error.value = '目录加载失败：' + e.message
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => stop())

function selectSection(section) {
  if (!canLearn(section)) {
    if (!authState.token) {
      showLoginPrompt(route.fullPath)
    } else {
      router.push('/order?courseIds=' + route.params.courseId)
    }
    return
  }
  stop()
  current.value = section
  const rec = records.value[section.id]
  // 已学完的节直接把进度拉满，避免显示"0% + 已学完"的矛盾状态
  const total = section.mediaDuration || 0
  moment.value = rec ? (rec.finished ? total : rec.moment || 0) : 0
  lastReportedMoment = -999
  reportHint.value = ''
}

function toggle() {
  if (playing.value) stop()
  else play()
}

function play() {
  if (!duration.value) return
  if (finished.value) {
    reportHint.value = '本节已学完，无需重复上报'
    return
  }
  playing.value = true
  timer = setInterval(tick, 1000)
}

function stop() {
  playing.value = false
  if (timer) clearInterval(timer)
  timer = null
}

function tick() {
  moment.value = Math.min(duration.value, moment.value + speed.value)
  // 每推进 15 秒（按视频时间）上报一次，避免每秒一个请求
  if (moment.value - lastReportedMoment >= 15 || moment.value >= duration.value) {
    report(false)
  }
}

async function report(isFinal) {
  const section = current.value
  if (!section || !lessonId.value) return
  // 试看模式不写学习进度（游客/未购买没有学习记录）
  if (!owned.value) return
  lastReportedMoment = moment.value
  const payload = {
    sectionType: 1,
    lessonId: lessonId.value,
    sectionId: section.id,
    duration: duration.value,
    moment: isFinal ? duration.value : moment.value,
    // 后端 Jackson 的 LocalDateTime 用的是 "yyyy-MM-dd HH:mm:ss"，不是 ISO 的带 T 格式
    commitTime: nowStr()
  }
  try {
    await api.reportRecord(payload)
    const rec = records.value[section.id] || {}
    records.value = {
      ...records.value,
      [section.id]: { moment: payload.moment, finished: rec.finished }
    }
    // 到达 80% 后端会置为学完，这里再拉一次确认
    if (payload.moment * 1.25 >= duration.value) {
      await loadProgress()
      if (isFinished(section.id)) {
        stop()
        moment.value = duration.value   // 完成后进度条拉满，避免停在 82%
        reportHint.value = '本节已学完：课表进度 +1，学习积分消息已发出，证书学时同步增加'
      }
    }
  } catch (e) {
    reportHint.value = '进度上报失败：' + e.message
  }
}

// 进度到 80% 时自动补一次"最终上报"，保证后端判定学完
watch(moment, (val) => {
  if (duration.value && val * 1.25 >= duration.value && !finished.value) {
    report(true)
  }
})

function startExam() {
  if (current.value) {
    router.push(`/exam/${route.params.courseId}/${current.value.id}`)
  }
}
</script>

<style scoped>
.trial-bar {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  font-size: 13px;
  color: #92400e;
}
.tb-tag {
  background: #f59e0b;
  color: #fff;
  font-size: 11px;
  border-radius: 4px;
  padding: 2px 8px;
}
.trial-note {
  display: flex; align-items: center; gap: 10px; margin-top: 16px; padding: 12px 14px;
  background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 10px;
  font-size: 12px; color: #64748b;
}
.trial-tag {
  font-size: 10px;
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: 4px;
  padding: 1px 6px;
  margin-right: 6px;
}
.lock { margin-right: 6px; font-size: 11px; opacity: .7; }
.lesson.locked { opacity: .72; }

.stage {
  position: relative;
  height: 300px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0b1220, #16233c 60%, #0b1220);
  display: grid;
  place-items: center;
  overflow: hidden;
}
.stage-ttl {
  position: absolute;
  left: 16px;
  top: 14px;
  color: #e2e8f0;
  font-size: 13px;
}
.stage-ttl small {
  display: block;
  color: #94a3b8;
  margin-top: 3px;
  font-size: 12px;
}
.stage-play {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  display: grid;
  place-items: center;
  cursor: pointer;
}
.tri {
  width: 0;
  height: 0;
  border-left: 18px solid #1e3a8a;
  border-top: 11px solid transparent;
  border-bottom: 11px solid transparent;
  margin-left: 5px;
}
.pause {
  display: flex;
  gap: 5px;
}
.pause i {
  width: 5px;
  height: 18px;
  background: #1e3a8a;
  border-radius: 1px;
}
.stage-bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 5px;
  background: rgba(255, 255, 255, 0.16);
}
.stage-bar i {
  display: block;
  height: 100%;
  background: #3b82f6;
  transition: width 0.4s linear;
}
.speeds {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.speed {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 7px;
  padding: 4px 9px;
  font-size: 12px;
  cursor: pointer;
  color: #475569;
}
.speed.on {
  border-color: #93c5fd;
  background: #eef2ff;
  color: var(--brand);
  font-weight: 600;
}
.dot.done {
  background: #059669;
}
.lesson-tabs { display: flex; gap: 6px; margin-top: 16px; border-bottom: 1px solid var(--line); }
.ltab {
  border: none; background: transparent; padding: 8px 14px; font-size: 13px;
  color: #64748b; cursor: pointer; border-bottom: 2px solid transparent;
}
.ltab.on { color: var(--brand); border-bottom-color: var(--brand); font-weight: 600; }
.lesson-panel { padding-top: 14px; }
.badge.green {
  background: #ecfdf5;
  color: #059669;
}
</style>
