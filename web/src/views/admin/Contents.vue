<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <div class="adm-subtabs">
        <button class="adm-subtab" :class="{ on: kind === 'notes' }" @click="switchKind('notes')">笔记 {{ counts.notes }}</button>
        <button class="adm-subtab" :class="{ on: kind === 'qa' }" @click="switchKind('qa')">提问{{ counts.qa ? " " + counts.qa : "" }}</button>
        <button class="adm-subtab" :class="{ on: kind === 'replies' }" @click="switchKind('replies')">回答</button>
      </div>
      <input v-if="kind === 'notes'" v-model="noteQuery.name" class="adm-input" placeholder="作者名" style="width:150px" @keyup.enter="load(1)" />
      <input v-if="kind === 'qa'" v-model="qaQuery.courseName" class="adm-input" placeholder="课程名" style="width:150px" @keyup.enter="load(1)" />
      <select v-model="hiddenFilter" class="adm-sel">
        <option value="">全部</option>
        <option value="false">显示中</option>
        <option value="true">已隐藏</option>
      </select>
      <button class="btn primary adm-btn-sm" @click="load(1)">查询</button>
      <span class="muted" style="font-size:12px;margin-left:auto">共 {{ total }} 条</span>
    </div>

    <div class="adm-panel">
      <!-- 笔记 -->
      <table v-if="kind === 'notes'" class="adm-tbl">
        <thead><tr><th>内容</th><th>课程 / 小节</th><th>作者</th><th>时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="n in notes" :key="n.id">
            <td style="max-width:380px">{{ n.content }}</td>
            <td class="muted" style="font-size:12px">{{ n.courseName }}<br />{{ n.sectionName }}</td>
            <td>{{ n.authorName }}</td>
            <td class="muted">{{ fmt(n.createTime) }}</td>
            <td><span class="adm-badge" :class="n.hidden ? 'red' : 'green'">{{ n.hidden ? '已隐藏' : '正常' }}</span></td>
            <td><button class="btn ghost adm-btn-sm" @click="toggleNote(n)">{{ n.hidden ? '恢复' : '隐藏' }}</button></td>
          </tr>
        </tbody>
      </table>

      <!-- 提问 -->
      <table v-if="kind === 'qa'" class="adm-tbl">
        <thead><tr><th>标题</th><th>课程 / 小节</th><th>提问人</th><th>时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="q in qaList" :key="q.id">
            <td style="max-width:380px">{{ q.title }}</td>
            <td class="muted" style="font-size:12px">{{ q.courseName }}<br />{{ q.sectionName }}</td>
            <td>{{ q.userName }}</td>
            <td class="muted">{{ fmt(q.createTime) }}</td>
            <td><span class="adm-badge" :class="q.hidden ? 'red' : 'green'">{{ q.hidden ? '已隐藏' : '正常' }}</span></td>
            <td>
              <button class="btn primary adm-btn-sm" @click="seeReplies(q)">查看回答</button>
              <button class="btn ghost adm-btn-sm" style="margin-left:6px" @click="toggleQa(q)">{{ q.hidden ? '恢复' : '隐藏' }}</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 回答 -->
      <div v-if="kind === 'replies' && !replyQuestionId" class="adm-empty">
        回答需要指定问题：请到「提问」页签点某条问题的「查看回答」
      </div>
      <table v-if="kind === 'replies' && replyQuestionId" class="adm-tbl">
        <thead><tr><th>内容</th><th>回答人</th><th>时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in replies" :key="r.id">
            <td style="max-width:520px">{{ r.content }}</td>
            <td>{{ r.anonymity ? '匿名' : r.userName }}</td>
            <td class="muted">{{ fmt(r.createTime) }}</td>
            <td><span class="adm-badge" :class="r.hidden ? 'red' : 'green'">{{ r.hidden ? '已隐藏' : '正常' }}</span></td>
            <td><button class="btn ghost adm-btn-sm" @click="toggleReply(r)">{{ r.hidden ? '恢复' : '隐藏' }}</button></td>
          </tr>
        </tbody>
      </table>

      <div v-if="!currentList.length" class="adm-empty">没有数据</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="pageNo <= 1" @click="load(pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="pageNo >= pages" @click="load(pageNo + 1)">下一页</button>
      </div>
    </div>
    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const kind = ref('notes')
const replyQuestionId = ref('')
const notes = ref([])
const qaList = ref([])
const replies = ref([])
const total = ref(0)
const pages = ref(1)
const pageNo = ref(1)
const hiddenFilter = ref('')
const noteQuery = reactive({ name: '' })
const qaQuery = reactive({ courseName: '' })
const counts = reactive({ notes: 0, qa: 0, replies: 0 })
const msg = ref('')
const ok = ref(true)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
const currentList = computed(() => `${kind.value}List`)

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

function hiddenParam() {
  return hiddenFilter.value === '' ? undefined : hiddenFilter.value === 'true'
}

async function load(page = 1) {
  pageNo.value = page
  try {
    if (kind.value === 'notes') {
      const data = await api.adminNotePage({ pageNo: page, pageSize: 8, name: noteQuery.name || undefined, hidden: hiddenParam() })
      notes.value = (data && data.list) || []
      total.value = Number((data && data.total) || 0)
      pages.value = Number((data && data.pages) || 1)
      counts.notes = total.value
    } else if (kind.value === 'qa') {
      const data = await api.adminQaPage({ pageNo: page, pageSize: 8, courseName: qaQuery.courseName || undefined })
      qaList.value = (data && data.list) || []
      total.value = Number((data && data.total) || 0)
      pages.value = Number((data && data.pages) || 1)
      counts.qa = total.value
    } else {
      if (!replyQuestionId.value) { replies.value = []; total.value = 0; pages.value = 1; return }
      const data = await api.adminReplyPage({ pageNo: page, pageSize: 8, questionId: replyQuestionId.value })
      replies.value = (data && data.list) || []
      total.value = Number((data && data.total) || 0)
      pages.value = Number((data && data.pages) || 1)
      counts.replies = total.value
    }
  } catch (e) {
    flash('加载失败：' + e.message, false)
  }
}

function seeReplies(q) {
  replyQuestionId.value = q.id
  kind.value = 'replies'
  load(1)
}

function switchKind(k) {
  kind.value = k
  load(1)
}

async function toggleNote(n) {
  try {
    await api.hideNote(n.id, !n.hidden)
    flash(n.hidden ? '已恢复显示' : '已隐藏')
    await load(pageNo.value)
  } catch (e) {
    flash('操作失败：' + e.message, false)
  }
}

async function toggleQa(q) {
  try {
    await api.hideQa(q.id, !q.hidden)
    flash(q.hidden ? '已恢复显示' : '已隐藏')
    await load(pageNo.value)
  } catch (e) {
    flash('操作失败：' + e.message, false)
  }
}

async function toggleReply(r) {
  try {
    await api.hideReply(r.id, !r.hidden)
    flash(r.hidden ? '已恢复显示' : '已隐藏')
    await load(pageNo.value)
  } catch (e) {
    flash('操作失败：' + e.message, false)
  }
}

onMounted(async () => {
  await load(1)
// 预取提问总数
  try {
    const qa = await api.adminQaPage({ pageNo: 1, pageSize: 1 })
    counts.qa = Number((qa && qa.total) || 0)
  } catch (e) { /* 忽略 */ }
})
</script>
