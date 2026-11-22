<template>
  <div class="adm-content">
    <!-- 筛选 -->
    <div class="adm-panel adm-filter">
      <div class="f-item">
        <span class="f-label">优惠券类型</span>
        <select v-model="query.type" class="adm-sel">
          <option value="">请选择</option>
          <option v-for="t in typeOptions" :key="t.v" :value="t.v">{{ t.label }}</option>
        </select>
      </div>
      <div class="f-item">
        <span class="f-label">优惠券状态</span>
        <select v-model="query.status" class="adm-sel">
          <option value="">请选择</option>
          <option v-for="s in statusOptions" :key="s.v" :value="s.v">{{ s.label }}</option>
        </select>
      </div>
      <div class="f-item">
        <span class="f-label">优惠券名称</span>
        <input v-model="query.name" class="adm-input" placeholder="请输入" style="width:200px" @keyup.enter="load(1)" />
      </div>
      <div class="f-actions">
        <button class="btn primary" @click="load(1)">搜索</button>
        <button class="btn ghost" @click="reset">重置</button>
      </div>
    </div>

    <!-- 工具条 -->
    <div class="adm-panel toolbar">
      <button class="btn primary" @click="openCreate">+ 新增优惠券</button>
      <span class="muted" style="font-size:12px;margin-left:auto">共 {{ total }} 张</span>
    </div>

    <!-- 列表 -->
    <div class="adm-panel">
      <table class="adm-tbl">
        <thead>
          <tr>
            <th style="width:52px">序号</th>
            <th>优惠券名称</th>
            <th>优惠券规则</th>
            <th>使用范围</th>
            <th>推广方式</th>
            <th>已用 / 发放</th>
            <th>状态</th>
            <th>创建时间</th>
            <th style="width:250px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(c, i) in list" :key="c.id">
            <td class="adm-mono">{{ (query.pageNo - 1) * query.pageSize + i + 1 }}</td>
            <td>{{ c.name }}</td>
            <td>{{ ruleText(c) }}</td>
            <td>{{ c.specific ? '指定课程分类' : '全部课程分类' }}</td>
            <td>{{ c.obtainWay === 2 ? '指定发放' : '手动领取' }}</td>
            <td>{{ c.usedNum ?? 0 }} / {{ c.issueNum ?? 0 }} / {{ c.totalNum ?? 0 }}</td>
            <td><span class="adm-badge" :class="statusCls(c.status)">{{ statusLabel(c.status) }}</span></td>
            <td class="adm-mono">{{ (c.createTime || '').slice(0, 10) }}</td>
            <td style="white-space:nowrap">
              <button v-if="!canEdit(c)" class="link" @click="openView(c)">查看</button>
              <button v-if="canEdit(c)" class="link" @click="openEdit(c)">编辑</button>
              <button v-if="c.status === 1" class="link" @click="openIssue(c)">发放</button>
              <button v-if="c.status === 2 || c.status === 3" class="link" @click="pause(c)">暂停</button>
              <button class="link" :class="{ dim: c.obtainWay !== 2 }" @click="c.obtainWay === 2 && openCodes(c)">查看兑换码</button>
              <button class="link danger" @click="remove(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有优惠券</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>

    <!-- 新增 / 编辑 -->
    <div v-if="showForm" class="adm-modal" @click.self="showForm = false">
      <div class="adm-dialog" style="width:760px">
        <h3>{{ readonly ? '查看优惠券' : (editingId ? '编辑优惠券' : '新增优惠券') }}</h3>

        <p class="seg-title">优惠券基本信息</p>
        <div class="adm-form">
          <div class="adm-field">
            <label><i class="req">*</i> 优惠券名称</label>
            <div class="with-count">
              <input v-model="form.name" class="adm-input" maxlength="20" :disabled="readonly" placeholder="例如：新年大酬宾" />
              <span class="count">{{ form.name.length }}/20</span>
            </div>
          </div>

          <div class="adm-field">
            <label><i class="req">*</i> 使用范围</label>
            <div class="radios">
              <label><input v-model="form.specific" type="radio" :value="false" :disabled="readonly" /> 不限定</label>
              <label><input v-model="form.specific" type="radio" :value="true" :disabled="readonly" /> 指定课程分类</label>
            </div>
          </div>
          <div v-if="form.specific" class="adm-field">
            <label>指定分类</label>
            <div class="scope-row">
              <select v-model="scopePick" class="adm-sel" :disabled="readonly">
                <option value="">选择分类</option>
                <option v-for="c in cates" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <button v-if="!readonly" class="btn ghost adm-btn-sm" :disabled="!scopePick" @click="addScope">添加</button>
            </div>
            <div class="chips">
              <span v-for="id in form.scopes" :key="id" class="chip">
                {{ cateName(id) }}
                <b v-if="!readonly" @click="removeScope(id)">×</b>
              </span>
              <span v-if="!form.scopes.length" class="muted" style="font-size:12px">还没有选择分类</span>
            </div>
          </div>

          <div class="adm-field">
            <label><i class="req">*</i> 优惠券类型</label>
            <div class="radios">
              <label v-for="t in typeOptions" :key="t.v">
                <input v-model.number="form.discountType" type="radio" :value="t.v" :disabled="readonly" /> {{ t.label }}
              </label>
            </div>
          </div>

          <div class="adm-field">
            <label>规则</label>
            <div class="rule-row">
              <template v-if="form.discountType === 1 || form.discountType === 2 || form.discountType === 4">
                <span>满</span>
                <input v-model.number="form.threshold" class="adm-input" type="number" min="1" style="width:110px" :disabled="readonly" />
                <span>元</span>
              </template>
              <template v-if="form.discountType === 2">
                <span>打</span>
                <input v-model.number="form.value" class="adm-input" type="number" min="1" max="99" style="width:90px" :disabled="readonly" />
                <span>折</span>
              </template>
              <template v-else>
                <span>减</span>
                <input v-model.number="form.value" class="adm-input" type="number" min="1" style="width:110px" :disabled="readonly" />
                <span>元</span>
              </template>
              <template v-if="form.discountType === 1 || form.discountType === 2">
                <span>不超过</span>
                <input v-model.number="form.maxDiscount" class="adm-input" type="number" min="0" style="width:110px" :disabled="readonly" />
                <span>元</span>
              </template>
            </div>
            <p class="muted" style="font-size:12px;margin-top:6px">{{ ruleText(previewCoupon) }}</p>
          </div>
        </div>

        <p class="seg-title">优惠券推广方式</p>
        <div class="adm-form">
          <div class="adm-field">
            <label><i class="req">*</i> 推广方式</label>
            <div class="radios">
              <label><input v-model.number="form.obtainWay" type="radio" :value="1" :disabled="readonly" /> 手动领取</label>
              <label><input v-model.number="form.obtainWay" type="radio" :value="2" :disabled="readonly" /> 指定发放</label>
            </div>
            <p class="muted" style="font-size:12px;margin-top:4px">
              {{ form.obtainWay === 2 ? '指定发放：发放时自动生成兑换码，学员凭码兑换' : '手动领取：学员在领券中心自行领取' }}
            </p>
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label><i class="req">*</i> 发放数量</label>
              <input v-model.number="form.totalNum" class="adm-input" type="number" min="1" max="5000" :disabled="readonly" />
            </div>
            <div class="adm-field">
              <label><i class="req">*</i> 每人限领</label>
              <input v-model.number="form.userLimit" class="adm-input" type="number" min="1" max="10" :disabled="readonly" />
            </div>
          </div>
        </div>

        <div class="adm-actions">
          <button class="btn ghost" @click="showForm = false">{{ readonly ? '关闭' : '取消' }}</button>
          <button v-if="!readonly" class="btn primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 发放 -->
    <div v-if="showIssue" class="adm-modal" @click.self="showIssue = false">
      <div class="adm-dialog" style="width:620px">
        <h3>设置领用/使用期限</h3>
        <p class="muted" style="font-size:12px;margin-bottom:12px">{{ issueTarget.name }} · 共 {{ issueTarget.totalNum }} 张</p>
        <div class="adm-form">
          <div class="adm-field">
            <label><i class="req">*</i> 发放方式</label>
            <div class="radios">
              <label><input v-model="issueForm.mode" type="radio" value="now" /> 立刻发放</label>
              <label><input v-model="issueForm.mode" type="radio" value="plan" /> 定时发放</label>
            </div>
          </div>
          <div v-if="issueForm.mode === 'plan'" class="adm-field">
            <label><i class="req">*</i> 发放开始时间</label>
            <input v-model="issueForm.issueBeginTime" class="adm-input" type="datetime-local" />
          </div>
          <div class="adm-field">
            <label><i class="req">*</i> 领用期限</label>
            <input v-model="issueForm.issueEndTime" class="adm-input" type="datetime-local" />
            <p class="muted" style="font-size:12px;margin-top:4px">超过这个时间学员不能再领取/兑换</p>
          </div>
          <div class="adm-field">
            <label><i class="req">*</i> 使用期限方式</label>
            <div class="radios">
              <label><input v-model="issueForm.termMode" type="radio" value="days" /> 固定天数</label>
              <label><input v-model="issueForm.termMode" type="radio" value="range" /> 固定时间段</label>
            </div>
          </div>
          <div v-if="issueForm.termMode === 'days'" class="adm-field">
            <label><i class="req">*</i> 使用期限</label>
            <div class="rule-row">
              <input v-model.number="issueForm.termDays" class="adm-input" type="number" min="1" style="width:120px" />
              <span>天（领取后开始计算）</span>
            </div>
          </div>
          <div v-else class="adm-row2">
            <div class="adm-field">
              <label>生效时间</label>
              <input v-model="issueForm.termBeginTime" class="adm-input" type="datetime-local" />
            </div>
            <div class="adm-field">
              <label>失效时间</label>
              <input v-model="issueForm.termEndTime" class="adm-input" type="datetime-local" />
            </div>
          </div>
        </div>
        <div class="adm-actions">
          <button class="btn ghost" @click="showIssue = false">取消</button>
          <button class="btn primary" :disabled="issuing" @click="doIssue">{{ issuing ? '提交中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 兑换码 -->
    <div v-if="showCodes" class="adm-modal" @click.self="showCodes = false">
      <div class="adm-dialog" style="width:640px">
        <h3>兑换码 · {{ codeTarget.name }}</h3>
        <p class="muted" style="font-size:12px;margin-bottom:10px">
          仅「指定发放」的券在首次发放时自动生成；学员在「我的优惠券 → 立即兑换」使用。
        </p>
        <div class="adm-filter" style="padding:0 0 10px">
          <select v-model.number="codeStatus" class="adm-sel" @change="loadCodes">
            <option :value="1">待兑换</option>
            <option :value="2">已兑换</option>
            <option :value="3">活动已结束</option>
          </select>
          <button class="btn ghost adm-btn-sm" :disabled="!codeList.length" @click="copyCodes">复制全部</button>
          <span class="muted" style="font-size:12px;margin-left:auto">共 {{ codeTotal }} 个</span>
        </div>
        <div style="max-height:280px;overflow:auto">
          <table class="adm-tbl">
            <thead><tr><th>兑换码</th><th>ID</th></tr></thead>
            <tbody>
              <tr v-for="c in codeList" :key="c.id">
                <td class="adm-mono">{{ c.code }}</td>
                <td class="adm-mono">{{ c.id }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!codeList.length" class="adm-empty">没有兑换码（该券不是「指定发放」，或还没发放）</div>
        </div>
        <div class="adm-actions">
          <button class="btn ghost" @click="showCodes = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const typeOptions = [
  { v: 1, label: '每满减' },
  { v: 2, label: '折扣' },
  { v: 3, label: '无门槛' },
  { v: 4, label: '满减' }
]
const statusOptions = [
  { v: 1, label: '待发放' },
  { v: 2, label: '未开始' },
  { v: 3, label: '发放中' },
  { v: 4, label: '发放结束' },
  { v: 5, label: '暂停' }
]

const list = ref([])
const total = ref(0)
const pages = ref(1)
const query = reactive({ pageNo: 1, pageSize: 8, type: '', status: '', name: '' })
const msg = ref('')
const ok = ref(true)

const showForm = ref(false)
const readonly = ref(false)
const editingId = ref('')
const saving = ref(false)
const cates = ref([])
const scopePick = ref('')
const form = reactive({
  name: '', specific: false, scopes: [], discountType: 4,
  threshold: 100, value: 10, maxDiscount: 0, totalNum: 100, userLimit: 1, obtainWay: 1
})

const showIssue = ref(false)
const issuing = ref(false)
const issueTarget = ref({})
const issueForm = reactive({
  mode: 'now', issueBeginTime: '', issueEndTime: '',
  termMode: 'days', termDays: 30, termBeginTime: '', termEndTime: ''
})

// 表单里的金额是「元」，ruleText 需要「分」，这里做一次换算用于实时预览
const previewCoupon = computed(() => ({
  discountType: form.discountType,
  thresholdAmount: Math.round(Number(form.threshold || 0) * 100),
  discountValue: form.discountType === 2 ? Number(form.value) : Math.round(Number(form.value || 0) * 100),
  maxDiscountAmount: Math.round(Number(form.maxDiscount || 0) * 100)
}))

const showCodes = ref(false)
const codeTarget = ref({})
const codeList = ref([])
const codeTotal = ref(0)
const codeStatus = ref(1)

const yuan = (v) => (Number(v || 0) / 100).toFixed(2).replace(/\.00$/, '')
const statusLabel = (s) => ({ 1: '待发放', 2: '未开始', 3: '发放中', 4: '发放结束', 5: '暂停' }[s] || '-')
const statusCls = (s) => ({ 1: 'warn', 2: '', 3: 'green', 4: '', 5: 'red' }[s] || '')
const canEdit = (c) => c.status === 1 || c.status === 2

function ruleText(c) {
  const t = Number(c.discountType)
  const th = Number(c.thresholdAmount ?? c.threshold ?? 0)
  const v = Number(c.discountValue ?? c.value ?? 0)
  const mx = Number(c.maxDiscountAmount ?? c.maxDiscount ?? 0)
  const cap = mx > 0 ? `，不超过${yuan(mx)}元` : ''
  // 门槛为 0 时一律按「无门槛」展示，避免出现「满0元减10元」
  if (t === 3 || th <= 0) return `无门槛减${yuan(v)}元`
  if (t === 1) return `每满${yuan(th)}元减${yuan(v)}元${cap}`
  if (t === 2) return `满${yuan(th)}元打${(v / 10).toFixed(1).replace(/\.0$/, '')}折${cap}`
  return `满${yuan(th)}元减${yuan(v)}元${cap}`
}

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3200)
}

function toBackend(v) {
  if (!v) return null
  const s = String(v).replace('T', ' ')
  return s.length === 16 ? s + ':00' : s
}

function toLocal(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

function reset() {
  query.type = ''
  query.status = ''
  query.name = ''
  load(1)
}

async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminCouponPage({
      pageNo,
      pageSize: query.pageSize,
      type: query.type === '' ? undefined : query.type,
      status: query.status === '' ? undefined : query.status,
      name: query.name || undefined
    })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    flash('加载失败：' + e.message, false)
  }
}

async function ensureCates() {
  if (cates.value.length) return
  try {
    const all = (await api.adminCategoryTree()) || []
    cates.value = all.filter((c) => Number(c.level) <= 2)
  } catch (e) {
    cates.value = []
  }
}

const cateName = (id) => (cates.value.find((c) => String(c.id) === String(id)) || {}).name || id

function addScope() {
  if (scopePick.value && !form.scopes.includes(scopePick.value)) {
    form.scopes.push(scopePick.value)
  }
  scopePick.value = ''
}

function removeScope(id) {
  form.scopes = form.scopes.filter((x) => x !== id)
}

function fillForm(c) {
  // 详情接口的 scopes 是对象列表（CouponScopeVO），列表接口没有 scopes
  const scopeIds = (c.scopes || []).map((x) => String(x && typeof x === 'object' ? (x.bizId ?? x.id) : x)).filter((x) => x && x !== 'undefined')
  Object.assign(form, {
    name: c.name || '',
    specific: c.specific != null ? !!c.specific : scopeIds.length > 0,
    scopes: scopeIds,
    discountType: Number(c.discountType) || 4,
    threshold: Number(c.thresholdAmount || 0) / 100 || 100,
    value: Number(c.discountType) === 2 ? Number(c.discountValue || 0) : Number(c.discountValue || 0) / 100,
    maxDiscount: Number(c.maxDiscountAmount || 0) / 100,
    totalNum: c.totalNum || 100,
    userLimit: c.userLimit || 1,
    obtainWay: Number(c.obtainWay) === 2 ? 2 : 1
  })
}

async function openCreate() {
  editingId.value = ''
  readonly.value = false
  showForm.value = true
  scopePick.value = ''
  Object.assign(form, {
    name: '', specific: false, scopes: [], discountType: 4,
    threshold: 100, value: 10, maxDiscount: 0, totalNum: 100, userLimit: 1, obtainWay: 1
  })
  await ensureCates()
}

async function openEdit(c) {
  await ensureCates()
  readonly.value = false
  editingId.value = c.id
  showForm.value = true
  try {
    const d = await api.couponDetail(c.id)
    fillForm(d && d.id ? d : c)
  } catch (e) {
    fillForm(c)
  }
}

async function openView(c) {
  await openEdit(c)
  readonly.value = true
}

async function save() {
  if (form.name.trim().length < 4 || form.name.trim().length > 20) return flash('名称长度需 4~20 字', false)
  if (form.specific && !form.scopes.length) return flash('指定课程分类时请至少选一个分类', false)
  const dto = {
    name: form.name.trim(),
    specific: form.specific,
    scopes: form.specific ? form.scopes : [],
    discountType: form.discountType,
    thresholdAmount: form.discountType === 3 ? 0 : Math.round(Number(form.threshold || 0) * 100),
    discountValue: form.discountType === 2 ? Number(form.value) : Math.round(Number(form.value || 0) * 100),
    maxDiscountAmount: Math.round(Number(form.maxDiscount || 0) * 100),
    totalNum: form.totalNum,
    userLimit: form.userLimit,
    obtainWay: form.obtainWay
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.updateCoupon(editingId.value, dto)
      flash('优惠券已保存')
    } else {
      await api.createCoupon(dto)
      flash('优惠券已创建，去「发放」才会生效')
    }
    showForm.value = false
    await load(editingId.value ? query.pageNo : 1)
  } catch (e) {
    flash('保存失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}

function openIssue(c) {
  issueTarget.value = c
  const now = new Date()
  Object.assign(issueForm, {
    mode: 'now',
    issueBeginTime: toLocal(new Date(now.getTime() + 30 * 1000)),
    issueEndTime: toLocal(new Date(now.getTime() + 7 * 24 * 3600 * 1000)),
    termMode: 'days',
    termDays: 30,
    termBeginTime: '',
    termEndTime: ''
  })
  showIssue.value = true
}

async function doIssue() {
  issuing.value = true
  try {
    const begin = issueForm.mode === 'now' ? toLocal(new Date(Date.now() + 30 * 1000)) : issueForm.issueBeginTime
    await api.issueCoupon(issueTarget.value.id, {
      issueBeginTime: toBackend(begin),
      issueEndTime: toBackend(issueForm.issueEndTime),
      termDays: issueForm.termMode === 'days' ? issueForm.termDays || 1 : null,
      termBeginTime: issueForm.termMode === 'range' ? toBackend(issueForm.termBeginTime) : null,
      termEndTime: issueForm.termMode === 'range' ? toBackend(issueForm.termEndTime) : null
    })
    showIssue.value = false
    flash('已提交发放，到点后自动切换为「发放中」')
    await load(query.pageNo)
  } catch (e) {
    flash('发放失败：' + e.message, false)
  } finally {
    issuing.value = false
  }
}

async function pause(c) {
  try {
    await api.pauseCoupon(c.id)
    flash('已暂停发放')
    await load(query.pageNo)
  } catch (e) {
    flash('暂停失败：' + e.message, false)
  }
}

async function remove(c) {
  if (!window.confirm(`确定删除「${c.name}」？`)) return
  try {
    await api.deleteCoupon(c.id)
    flash('已删除')
    await load(query.pageNo)
  } catch (e) {
    flash('删除失败：' + e.message, false)
  }
}

async function openCodes(c) {
  codeTarget.value = c
  codeStatus.value = 1
  showCodes.value = true
  await loadCodes()
}

async function loadCodes() {
  try {
    const data = await api.exchangeCodes({
      pageNo: 1, pageSize: 50, status: codeStatus.value, couponId: codeTarget.value.id
    })
    codeList.value = (data && data.list) || []
    codeTotal.value = Number((data && data.total) || 0)
  } catch (e) {
    codeList.value = []
    codeTotal.value = 0
  }
}

function copyCodes() {
  navigator.clipboard?.writeText(codeList.value.map((x) => x.code).join('\n'))
  flash('已复制 ' + codeList.value.length + ' 个兑换码')
}

onMounted(() => load(1))
</script>

<style>
.f-item { display: flex; align-items: center; gap: 8px; }
.f-label { font-size: 13px; color: #475569; white-space: nowrap; }
.f-item .adm-sel { min-width: 150px; }
.f-actions { margin-left: auto; display: flex; gap: 10px; }
.toolbar { display: flex; align-items: center; padding: 12px 16px; }
.adm-tbl .link {
  border: none; background: transparent; padding: 0; margin-right: 10px;
  color: #2563eb; font-size: 13px; cursor: pointer;
}
.adm-tbl .link.danger { color: #b91c1c; }
.adm-tbl .link.dim { color: #cbd5e1; cursor: not-allowed; }
.seg-title { font-size: 14px; font-weight: 600; margin: 6px 0 12px; color: #1e293b; }
.with-count { position: relative; }
.with-count .count { position: absolute; right: 10px; top: 9px; font-size: 12px; color: #94a3b8; }
.radios { display: flex; flex-wrap: wrap; gap: 18px; font-size: 13px; color: #334155; }
.radios label { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.req { color: #e11d48; font-style: normal; margin-right: 2px; }
.rule-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
.scope-row { display: flex; gap: 8px; align-items: center; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.chip {
  display: inline-flex; align-items: center; gap: 6px; padding: 3px 9px;
  background: #eef2ff; color: #1d4ed8; border-radius: 999px; font-size: 12px;
}
.chip b { cursor: pointer; }
</style>
