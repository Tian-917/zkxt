<template>
  <main class="wrap">
    <PageHeader title="章节练习" />

    <div v-if="loading" class="center muted">正在组卷…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>

    <div v-else-if="result" class="panel">
      <div class="notice ok" style="margin-bottom:16px">
        交卷完成：答对 {{ rightCount }} / {{ result.length }} 题
      </div>
      <div v-for="(d, i) in result" :key="d.question.id" style="margin-bottom:18px">
        <div class="q">第 {{ i + 1 }} 题 · {{ d.question.name }}</div>
        <div
          v-for="(o, oi) in d.question.options" :key="oi"
          class="opt"
          :class="{
            ok: letter(oi) === d.question.answer,
            bad: letter(oi) === d.answer && !d.correct
          }">
          <span class="k">{{ letter(oi) }}</span>
          <span>{{ stripPrefix(o) }}</span>
        </div>
        <div class="muted">我的答案 {{ d.answer || '未作答' }} · 正确答案 {{ d.question.answer }}</div>
        <div class="muted" style="margin-top:4px">解析：{{ d.question.analysis }}</div>
      </div>
      <div class="row">
        <button class="btn primary" @click="$router.push('/cert')">去证书中心</button>
        <button class="btn ghost" @click="$router.push('/')">返回首页</button>
      </div>
    </div>

    <div v-else class="panel">
      <div class="q">{{ current.name }}</div>
      <div
        v-for="(o, oi) in current.options" :key="oi"
        class="opt" :class="{ sel: answers[current.id] === letter(oi) }"
        @click="answers[current.id] = letter(oi)">
        <span class="k">{{ letter(oi) }}</span>
        <span>{{ stripPrefix(o) }}</span>
      </div>

      <div class="row">
        <button class="btn ghost" :disabled="idx === 0" @click="idx--">上一题</button>
        <button class="btn ghost" :disabled="idx === questions.length - 1" @click="idx++">下一题</button>
        <span class="muted">已答 {{ answeredCount }} / {{ questions.length }}</span>
        <button class="btn primary" style="margin-left:auto" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '交卷' }}
        </button>
      </div>
    </div>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const questions = ref([])
const idx = ref(0)
const answers = ref({})
const examId = ref('')
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const result = ref(null)

const current = computed(() => questions.value[idx.value] || { options: [] })
const answeredCount = computed(() => Object.keys(answers.value).length)
const rightCount = computed(() => (result.value || []).filter((d) => d.correct).length)

const letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const letter = (i) => letters[i]
// 选项自带 A. 前缀，去掉后加徽标
const stripPrefix = (text) => String(text || '').replace(/^[A-H][.、．]\s*/, '')

onMounted(async () => {
  try {
    const exam = await api.createExam({
      courseId: Number(route.params.courseId),
      sectionId: Number(route.params.sectionId),
      type: 1
    })
    examId.value = String(exam.id)
    questions.value = exam.questions || []
  } catch (e) {
    error.value = '组卷失败：' + e.message
  } finally {
    loading.value = false
  }
})

async function submit() {
  submitting.value = true
  try {
    const details = questions.value.map((q) => ({
      questionId: String(q.id),
      answer: answers.value[q.id] || ''
    }))
    result.value = await api.submitExam({ id: examId.value, examDetails: details })
  } catch (e) {
    error.value = '交卷失败：' + e.message
  } finally {
    submitting.value = false
  }
}
</script>
