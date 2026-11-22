<template>
  <div class="panel-block">
    <div class="head">
      <b>本节问答</b>
      <span class="muted">{{ questions.length }} 个问题</span>
      <button class="btn ghost sm" style="margin-left:auto" @click="showAsk = !showAsk">
        {{ showAsk ? '收起' : '+ 我要提问' }}
      </button>
    </div>

    <div v-if="showAsk" class="compose">
      <input v-model="title" class="input" placeholder="问题标题，例如：这一节的 80% 判定怎么算？" />
      <textarea v-model="description" class="input area" rows="2" placeholder="补充描述（可选）" />
      <div class="compose-foot">
        <label class="muted" style="display:flex;align-items:center;gap:6px;font-size:12px">
          <input v-model="anonymity" type="checkbox" /> 匿名提问
        </label>
        <button class="btn primary" :disabled="asking || !title.trim()" @click="ask">
          {{ asking ? '提交中…' : '提交问题' }}
        </button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'" style="margin-bottom:10px">{{ msg }}</div>

    <div v-if="loading" class="muted" style="font-size:13px">加载中…</div>
    <div v-else-if="!questions.length" class="muted" style="font-size:13px;padding:12px 0">
      本节还没有人提问，来问第一个
    </div>

    <article v-for="q in questions" :key="q.id" class="q">
      <div class="q-head" @click="toggle(q.id)">
        <span class="q-title">{{ q.title }}</span>
        <span class="badge">{{ q.answerTimes || 0 }} 回答</span>
        <span class="muted" style="margin-left:auto;font-size:11px">
          {{ q.anonymity ? '匿名' : (q.userName || '用户') }} · {{ fmt(q.createTime) }}
        </span>
      </div>
      <p v-if="q.description" class="q-desc">{{ q.description }}</p>

      <div v-if="openId === q.id" class="answers">
        <div v-if="!answers.length" class="muted" style="font-size:12px">还没有回答，来抢沙发</div>

        <div v-for="a in answers" :key="a.id" class="answer">
          <div class="a-head">
            <b>{{ a.anonymity ? '匿名用户' : (a.userName || '用户') }}</b>
            <span class="muted" style="font-size:11px">{{ fmt(a.createTime) }}</span>
            <button class="btn ghost sm" style="margin-left:auto" @click="toggleLike(a)">
              👍 {{ a.likedTimes || 0 }}
            </button>
            <button class="btn ghost sm" style="margin-left:6px" @click="startReply(q.id, a)">
              回复
            </button>
          </div>
          <p class="a-body">{{ a.content }}</p>

          <!-- 该回答下的评论 -->
          <div v-if="comments[a.id] && comments[a.id].length" class="comments">
            <div v-for="c in comments[a.id]" :key="c.id" class="comment">
              <span class="c-who">{{ c.anonymity ? '匿名' : (c.userName || '用户') }}</span>
              <span v-if="c.targetReplyId" class="c-target">回复 {{ c.targetUserName || '@TA' }}：</span>
              <span class="c-body">{{ c.content }}</span>
            </div>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyTo && replyTo.answerId === a.id" class="reply-box">
            <div class="muted" style="font-size:11px;margin-bottom:4px">
              回复 {{ replyTo.userName || 'TA' }}
            </div>
            <textarea v-model="replyContent" class="input area" rows="2" placeholder="写下你的回复…" />
            <div class="reply-actions">
              <button class="btn ghost sm" @click="cancelReply">取消</button>
              <button class="btn primary sm" :disabled="replying || !replyContent.trim()" @click="submitReply">
                {{ replying ? '提交中…' : '提交回复' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 回答这条问题 -->
        <div class="reply-box" style="margin-top:12px">
          <textarea v-model="answerContent" class="input area" rows="2" placeholder="写下你的回答…" />
          <div class="reply-actions">
            <button class="btn primary sm" :disabled="answering || !answerContent.trim()" @click="answer(q.id)">
              {{ answering ? '提交中…' : '提交回答' }}
            </button>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const props = defineProps({
  courseId: { type: [String, Number], required: true },
  chapterId: { type: [String, Number], default: '' },
  sectionId: { type: [String, Number], required: true }
})

const questions = ref([])
const answers = ref([])
const comments = reactive({})
const openId = ref(null)
const loading = ref(false)

const showAsk = ref(false)
const title = ref('')
const description = ref('')
const anonymity = ref(false)
const asking = ref(false)

const answerContent = ref('')
const answering = ref(false)

const replyTo = ref(null)      // { answerId, userId, userName }
const replyContent = ref('')
const replying = ref(false)

const msg = ref('')
const ok = ref(true)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

async function load() {
  loading.value = true
  try {
    const data = await api.questions({ courseId: props.courseId, sectionId: props.sectionId })
    questions.value = (data && data.list) || []
  } catch (e) {
    questions.value = []
  } finally {
    loading.value = false
  }
}

async function toggle(id) {
  if (openId.value === id) {
    openId.value = null
    return
  }
  openId.value = id
  answers.value = []
  Object.keys(comments).forEach((k) => delete comments[k])
  try {
    const data = await api.replies({ questionId: id })
    answers.value = (data && data.list) || []
    // 预取每条回答下的评论
    for (const a of answers.value) {
      loadComments(a.id)
    }
  } catch (e) {
    flash('回答加载失败：' + e.message, false)
  }
}

async function loadComments(answerId) {
  try {
    const data = await api.replies({ questionId: openId.value, answerId })
    comments[answerId] = (data && data.list) || []
  } catch (e) {
    comments[answerId] = []
  }
}

async function ask() {
  asking.value = true
  try {
    await api.saveQuestion({
      courseId: props.courseId,
      chapterId: props.chapterId,
      sectionId: props.sectionId,
      title: title.value.trim(),
      description: description.value.trim() || title.value.trim(),
      anonymity: anonymity.value
    })
    title.value = ''
    description.value = ''
    showAsk.value = false
    flash('问题已提交')
    await load()
  } catch (e) {
    flash('提问失败：' + e.message, false)
  } finally {
    asking.value = false
  }
}

async function answer(questionId) {
  answering.value = true
  try {
    await api.saveReply({
      questionId,
      content: answerContent.value.trim(),
      anonymity: false,
      isStudent: true,
      answerId: 0
    })
    answerContent.value = ''
    const data = await api.replies({ questionId })
    answers.value = (data && data.list) || []
    await load()
  } catch (e) {
    flash('回答失败：' + e.message, false)
  } finally {
    answering.value = false
  }
}

async function toggleLike(a) {
  try {
    const liked = !a.liked
    await api.likeRecord({ bizId: a.id, bizType: 'QA', liked })
    a.liked = liked
    a.likedTimes = Number(a.likedTimes || 0) + (liked ? 1 : -1)
  } catch (e) {
    flash('点赞失败：' + e.message, false)
  }
}

function startReply(questionId, a) {
  replyTo.value = { answerId: a.id, userId: a.userId, userName: a.userName }
  replyContent.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyContent.value = ''
}

async function submitReply() {
  replying.value = true
  try {
    await api.saveReply({
      questionId: openId.value,
      answerId: replyTo.value.answerId,          // 挂在这条回答下面
      content: replyContent.value.trim(),
      anonymity: false,
      isStudent: true,
      targetUserId: replyTo.value.userId,        // 回复某人
      targetReplyId: replyTo.value.answerId
    })
    const answerId = replyTo.value.answerId
    replyContent.value = ''
    replyTo.value = null
    await loadComments(answerId)
    await load()
  } catch (e) {
    flash('回复失败：' + e.message, false)
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.panel-block { padding: 4px 0; }
.head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; font-size: 14px; }
.compose { display: grid; gap: 8px; margin-bottom: 14px; }
.area { width: 100%; resize: vertical; }
.compose-foot { display: flex; align-items: center; gap: 12px; }
.compose-foot .btn { margin-left: auto; }
.q { border-top: 1px dashed var(--line); padding: 12px 0; }
.q-head { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.q-title { font-size: 13px; font-weight: 600; color: #1e293b; }
.q-desc { margin: 6px 0 0; font-size: 12px; color: #64748b; }
.answers { margin-top: 10px; padding-left: 10px; border-left: 2px solid #eef2ff; }
.answer { padding: 8px 0; }
.a-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.a-body { margin: 6px 0; font-size: 13px; color: #475569; line-height: 1.7; }
.comments { margin: 6px 0 0 8px; padding: 8px 10px; background: #f8fafc; border-radius: 8px; }
.comment { font-size: 12px; color: #475569; padding: 3px 0; line-height: 1.6; }
.c-who { color: #1d4ed8; font-weight: 600; }
.c-target { color: #64748b; }
.c-body { color: #334155; }
.reply-box { margin-top: 8px; }
.reply-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 6px; }
.btn.sm { padding: 3px 9px; font-size: 12px; }
</style>
