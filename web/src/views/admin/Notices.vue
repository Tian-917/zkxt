<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <select v-model="finished" class="adm-sel" @change="load(1)">
        <option value="">全部任务</option>
        <option value="false">未完成</option>
        <option value="true">已完成</option>
      </select>
      <input v-model="keyword" class="adm-input" placeholder="任务标题" style="width:200px" @keyup.enter="load(1)" />
      <button class="btn primary adm-btn-sm" @click="load(1)">查询</button>
      <button class="btn ghost adm-btn-sm" @click="openCreate">+ 新建通知任务</button>
      <span class="muted" style="font-size:12px;margin-left:auto">共 {{ total }} 条</span>
    </div>

    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>任务</th><th>ID</th><th>通知类型</th><th>推送时间</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="t in list" :key="t.id">
            <td style="max-width:420px">{{ t.name || t.title || t.noticeContent || ('任务 ' + t.id) }}</td>
            <td class="adm-mono">{{ t.id }}</td>
            <td>{{ typeText(t.type) }}</td>
            <td class="muted">{{ fmt(t.pushTime) }}</td>
            <td><span class="adm-badge" :class="t.finished ? 'green' : 'warn'">{{ t.finished ? '已完成' : '待推送' }}</span></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有通知任务</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>

    <div v-if="showForm" class="adm-modal" @click.self="showForm = false">
      <div class="adm-dialog" style="width:560px">
        <h3>新建通知任务</h3>
        <div class="adm-form">
          <div class="adm-field">
            <label>通知模板</label>
            <select v-model="form.templateId" class="adm-sel">
              <option value="">选择模板</option>
              <option v-for="t in templates" :key="t.id" :value="t.id">{{ t.name }}（{{ t.title || t.code }}）</option>
            </select>
          </div>
          <div class="adm-field">
            <label>任务名称</label>
            <input v-model="form.name" class="adm-input" placeholder="例如：双十一优惠提醒" />
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>推送时间</label>
              <input v-model="form.pushTime" class="adm-input" type="datetime-local" />
            </div>
            <div class="adm-field">
              <label>过期时间（可选）</label>
              <input v-model="form.expireTime" class="adm-input" type="datetime-local" />
            </div>
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>最多重试次数</label>
              <input v-model.number="form.maxTimes" class="adm-input" type="number" min="1" max="10" />
            </div>
            <div class="adm-field">
              <label>重试间隔（秒）</label>
              <input v-model.number="form.interval" class="adm-input" type="number" min="10" />
            </div>
          </div>
        </div>
        <div class="adm-actions">
          <button class="btn ghost" @click="showForm = false">取消</button>
          <button class="btn primary" :disabled="saving" @click="create">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const list = ref([])
const total = ref(0)
const pages = ref(1)
const finished = ref('')
const keyword = ref('')
const query = reactive({ pageNo: 1, pageSize: 8 })

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')
const typeText = (t) => ({ 1: '系统通知', 2: '课程通知', 3: '优惠活动', 4: '私信' }[t] || '-')


const showForm = ref(false)
const saving = ref(false)
const templates = ref([])
const msg = ref('')
const ok = ref(true)
const form = reactive({ templateId: '', name: '', pushTime: '', expireTime: '', maxTimes: 3, interval: 60 })

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

// datetime-local 给的是 2026-11-20T19:00，后端 LocalDateTime 要 "yyyy-MM-dd HH:mm:ss"
function toBackendTime(v) {
  if (!v) return null
  const s = String(v).replace('T', ' ')
  return s.length === 16 ? s + ':00' : s
}

// 默认推送时间 = 5 分钟后，避免用户不填导致任务没有推送时间
function defaultPushTime() {
  const d = new Date(Date.now() + 5 * 60 * 1000)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

async function openCreate() {
  showForm.value = true
  form.pushTime = defaultPushTime()
  try {
    const data = await api.noticeTemplates({ pageNo: 1, pageSize: 50 })
    templates.value = (data && data.list) || []
  } catch (e) {
    templates.value = []
  }
}

async function create() {
  if (!form.templateId || !form.name.trim()) {
    flash('请选择模板并填写任务名称', false)
    return
  }
  saving.value = true
  try {
    await api.createNoticeTask({
      templateId: form.templateId,
      name: form.name.trim(),
      partial: false,
      pushTime: toBackendTime(form.pushTime),
      maxTimes: form.maxTimes,
      interval: form.interval,
      expireTime: toBackendTime(form.expireTime)
    })
    showForm.value = false
    flash('通知任务已创建')
    await load(1)
  } catch (e) {
    flash('创建失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}
async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminNoticePage({
      pageNo,
      pageSize: query.pageSize,
      finished: finished.value === '' ? undefined : finished.value === 'true',
      keyword: keyword.value || undefined
    })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    list.value = []
  }
}

onMounted(() => load(1))
</script>
