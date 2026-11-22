<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <input v-model="query.keyword" class="adm-input" placeholder="课程名称" style="width:200px" @keyup.enter="load(1)" />
      <select v-model="query.status" class="adm-sel">
        <option value="">全部状态</option>
        <option :value="1">待上架</option>
        <option :value="2">已上架</option>
        <option :value="3">已下架</option>
        <option :value="4">已完结</option>
      </select>
      <button class="btn primary adm-btn-sm" @click="load(1)">查询</button>
      <button class="btn ghost adm-btn-sm" @click="openCreate">+ 新建课程</button>
      <span class="muted" style="font-size:12px;margin-left:auto">
        共 {{ total }} 门 · 上架 {{ stat.onSaleNum ?? '-' }} · 下架 {{ stat.offShelfNum ?? '-' }} · 录播 {{ stat.recordNum ?? '-' }}
      </span>
    </div>

    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>课程</th><th>ID</th><th>价格</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in list" :key="c.id">
            <td>{{ c.name }}</td>
            <td class="adm-mono">{{ c.id }}</td>
            <td>{{ (Number(c.price || 0) / 100).toFixed(0) }} 元</td>
            <td><span class="adm-badge" :class="cls(c.status)">{{ text(c.status) }}</span></td>
            <td style="white-space:nowrap">
              <button v-if="c.status === 2" class="btn ghost adm-btn-sm" @click="shelf(c, false)">下架</button>
              <button v-else class="btn ghost adm-btn-sm" @click="shelf(c, true)">上架</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有课程</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>

    <div v-if="showForm" class="adm-modal" @click.self="showForm = false">
      <div class="adm-dialog">
        <h3>新建课程</h3>
        <div class="adm-form">
          <div class="adm-field">
            <label>课程名称</label>
            <input v-model="form.name" class="adm-input" placeholder="例如：初级会计 · 冲刺押题班" />
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>三级分类</label>
              <select v-model="form.thirdCateId" class="adm-sel">
                <option value="">请选择</option>
                <option v-for="c in leafCates" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="adm-field">
              <label>有效期（月，0 表示永久）</label>
              <input v-model.number="form.validDuration" class="adm-input" type="number" min="0" />
            </div>
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>是否免费</label>
              <select v-model="form.free" class="adm-sel">
                <option :value="false">付费</option>
                <option :value="true">免费</option>
              </select>
            </div>
            <div class="adm-field">
              <label>价格（元）</label>
              <input v-model.number="form.price" class="adm-input" type="number" min="0" :disabled="form.free" />
            </div>
          </div>
          <div class="adm-field">
            <label>购买截止时间</label>
            <input v-model="form.purchaseEndTime" class="adm-input" type="datetime-local" />
          </div>
          <div class="adm-field">
            <label>封面地址</label>
            <input v-model="form.coverUrl" class="adm-input" placeholder="/img-tx/default-cover-url.jpg" />
          </div>
          <div class="adm-field">
            <label>课程介绍</label>
            <textarea v-model="form.introduce" class="adm-textarea" rows="2" />
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>适用人群</label>
              <input v-model="form.usePeople" class="adm-input" placeholder="零基础学员" />
            </div>
            <div class="adm-field">
              <label>详情</label>
              <input v-model="form.detail" class="adm-input" placeholder="课程详情说明" />
            </div>
          </div>
          <label class="muted" style="display:flex;align-items:center;gap:6px;font-size:12px">
            <input v-model="withCata" type="checkbox" /> 同时创建一章一节（否则上架会因缺少目录失败）
          </label>
        </div>
        <div class="adm-actions">
          <button class="btn ghost" @click="showForm = false">取消</button>
          <button class="btn primary" :disabled="saving" @click="create">{{ saving ? '创建中…' : '创建' }}</button>
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
const stat = ref({})
const query = reactive({ pageNo: 1, pageSize: 8, keyword: '', status: '' })
const msg = ref('')
const ok = ref(true)

const showForm = ref(false)
const saving = ref(false)
const withCata = ref(true)
const leafCates = ref([])
const form = reactive({
  name: '', thirdCateId: '', validDuration: 12, free: false, price: 399,
  purchaseEndTime: '', coverUrl: '/img-tx/default-cover-url.jpg',
  introduce: '', usePeople: '零基础学员', detail: ''
})

const text = (s) => ({ 1: '待上架', 2: '已上架', 3: '已下架', 4: '已完结' }[s] || '未知')
const cls = (s) => (s === 2 ? 'green' : (s === 3 ? 'warn' : ''))

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3500)
}

function toBackendTime(v) {
  if (!v) return null
  const s = String(v).replace('T', ' ')
  return s.length === 16 ? s + ':00' : s
}

function defaultEnd() {
  const d = new Date(Date.now() + 90 * 24 * 3600 * 1000)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminCoursePage({
      pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status === '' ? undefined : query.status
    })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    flash('加载失败：' + e.message, false)
  }
}

async function shelf(c, up) {
  try {
    if (up) await api.upShelf(c.id)
    else await api.downShelf(c.id)
    flash(`${c.name} 已${up ? '上架' : '下架'}`)
    await load(query.pageNo)
  } catch (e) {
    flash('操作失败：' + e.message, false)
  }
}

async function openCreate() {
  showForm.value = true
  Object.assign(form, {
    name: '', thirdCateId: '', validDuration: 12, free: false, price: 399,
    purchaseEndTime: defaultEnd(), coverUrl: '/img-tx/default-cover-url.jpg',
    introduce: '系统化备考，配套题库与模考', usePeople: '零基础学员', detail: '包含课程、题库、模考与证书'
  })
  if (!leafCates.value.length) {
    try {
      const all = (await api.adminCategoryTree()) || []
      leafCates.value = all.filter((c) => Number(c.level) === 3)
    } catch (e) { leafCates.value = [] }
  }
}

async function create() {
  if (!form.name.trim() || !form.thirdCateId) {
    flash('课程名称和三级分类必填', false)
    return
  }
  saving.value = true
  try {
    const res = await api.createCourse({
      name: form.name.trim(),
      thirdCateId: form.thirdCateId,
      coverUrl: form.coverUrl || '/img-tx/default-cover-url.jpg',
      free: form.free,
      price: form.free ? null : Math.round(Number(form.price || 0) * 100),
      purchaseEndTime: toBackendTime(form.purchaseEndTime),
      introduce: form.introduce,
      usePeople: form.usePeople,
      detail: form.detail,
      validDuration: form.validDuration || 0
    })
    const newId = res && res.id
    if (withCata.value && newId) {
      await api.saveCatalogue(newId, [{
        type: 1,
        name: '第1章 入门',
        index: 1,
        sections: [{ type: 2, name: '1.1 课程导学', index: 1 }]
      }])
    }
    showForm.value = false
    flash(`课程已创建为草稿${withCata.value ? '（含 1 章 1 节）' : ''}；上架前需补齐目录、视频、题目与讲师`)
    await load(1)
  } catch (e) {
    flash('创建失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    stat.value = (await api.courseStatistics()) || {}
  } catch (e) { /* 统计失败不影响列表 */ }
  await load(1)
})
</script>
