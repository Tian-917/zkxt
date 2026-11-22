<template>
  <main class="wrap">
    <PageHeader title="消息中心">
<div class="tabs" style="margin-left:auto">
        <button class="tab" :class="{ on: filter === '' }" @click="switchFilter('')">全部</button>
        <button class="tab" :class="{ on: filter === false }" @click="switchFilter(false)">
          未读<span v-if="unreadCount" class="dot">{{ unreadCount }}</span>
        </button>
        <button class="tab" :class="{ on: filter === true }" @click="switchFilter(true)">已读</button>
      </div>
      </PageHeader>

    <div class="panel">
      <div class="bar">
        <span class="muted">
          共 {{ total }} 条<span v-if="unreadCount"> · 未读 <b style="color:#b91c1c">{{ unreadCount }}</b></span>
        </span>
        <button class="btn ghost" style="padding:6px 12px;font-size:12px;margin-left:auto"
                :disabled="!unreadCount" @click="readAll">全部已读</button>
      </div>

      <div v-if="loading" class="center muted">加载中…</div>
      <div v-else-if="error" class="notice bad">{{ error }}</div>
      <div v-else-if="!list.length" class="center muted" style="padding:40px 0">
        没有消息
      </div>

      <article v-for="m in list" :key="m.id" class="msg" :class="{ unread: !m.isRead }">
        <div class="msg-head">
          <span class="tag" :class="m.type === 3 ? 'sys' : 'pri'">{{ typeName(m.type) }}</span>
          <b class="ttl">{{ m.title || '站内信' }}</b>
          <span v-if="!m.isRead" class="new">未读</span>
          <span class="time">{{ fmt(m.pushTime) }}</span>
        </div>
        <p class="body">{{ m.content }}</p>
        <div class="msg-foot">
          <button v-if="!m.isRead" class="btn ghost" style="padding:4px 10px;font-size:12px"
                  @click="read(m.id)">标记已读</button>
        </div>
      </article>

      <div v-if="list.length && list.length < total" class="center" style="padding:14px 0">
        <button class="btn ghost" @click="loadMore">加载更多（{{ list.length }}/{{ total }}）</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import { refreshUnread, unread as unreadRef } from '../store'

const list = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const filter = ref(false)     // '' 全部 / false 未读 / true 已读
const loading = ref(false)
const error = ref('')

const unreadCount = computed(() => unreadRef.value)

const typeName = (t) => (t === 3 ? '系统' : '私信')

function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

async function fetchPage(reset) {
  loading.value = true
  error.value = ''
  try {
    const data = await api.inboxes({
      pageNo: pageNo.value,
      pageSize,
      isRead: filter.value === '' ? undefined : filter.value
    })
    const items = (data && data.list) || []
    list.value = reset ? items : list.value.concat(items)
    total.value = (data && data.total) || 0
  } catch (e) {
    error.value = '加载失败：' + e.message
  } finally {
    loading.value = false
  }
}

function switchFilter(f) {
  filter.value = f
  pageNo.value = 1
  fetchPage(true)
}

function loadMore() {
  pageNo.value += 1
  fetchPage(false)
}

async function read(id) {
  await api.markRead(id)
  await Promise.all([fetchPage(true), refreshUnread()])
}

async function readAll() {
  await api.markAllRead()
  await Promise.all([fetchPage(true), refreshUnread()])
}

onMounted(async () => {
  await fetchPage(true)
  await refreshUnread()
})
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
.dot {
  display: inline-block;
  margin-left: 5px;
  background: #ef4444;
  color: #fff;
  border-radius: 999px;
  padding: 0 5px;
  font-size: 10px;
  line-height: 15px;
}
.bar { display: flex; align-items: center; padding-bottom: 12px; border-bottom: 1px solid var(--line); }
.msg { padding: 14px 2px; border-bottom: 1px solid var(--line); }
.msg:last-child { border-bottom: none; }
.msg.unread { background: linear-gradient(90deg, #f8fbff, transparent 60%); }
.msg-head { display: flex; align-items: center; gap: 8px; }
.ttl { font-size: 14px; color: #1e293b; font-weight: 600; }
.new {
  background: #fef2f2; color: #b91c1c; font-size: 10px;
  border-radius: 6px; padding: 1px 6px;
}
.time { margin-left: auto; font-size: 12px; color: #94a3b8; }
.body { margin: 8px 0 0; font-size: 13px; color: #475569; line-height: 1.7; }
.msg-foot { margin-top: 8px; }
.tag { font-size: 11px; border-radius: 6px; padding: 2px 7px; }
.tag.pri { background: #eef2ff; color: #4338ca; }
.tag.sys { background: #ecfdf5; color: #059669; }
</style>
