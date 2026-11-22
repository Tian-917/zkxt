<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <input v-model="query.keyword" class="adm-input" placeholder="题目标题关键词" style="width:240px" @keyup.enter="load(1)" />
      <select v-model="query.difficulty" class="adm-sel">
        <option value="">全部难度</option>
        <option :value="1">简单</option>
        <option :value="2">中等</option>
        <option :value="3">困难</option>
      </select>
      <button class="btn primary adm-btn-sm" @click="load(1)">查询</button>
      <button class="btn ghost adm-btn-sm" @click="openCreate">+ 新增题目</button>
      <span class="muted" style="font-size:12px;margin-left:auto">共 {{ total }} 道题</span>
    </div>

    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>题目</th><th>类型</th><th>难度</th><th>分值</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="q in list" :key="q.id">
            <td style="max-width:520px">{{ q.name }}</td>
            <td>{{ typeText(q.type) }}</td>
            <td>{{ diffText(q.difficulty) }}</td>
            <td>{{ q.score }}</td>
            <td>
              <button class="btn ghost adm-btn-sm" @click="openEdit(q)">编辑</button>
              <button class="btn ghost adm-btn-sm" style="color:#b91c1c;margin-left:6px" @click="remove(q)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有题目</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>

    <div v-if="showForm" class="adm-modal" @click.self="showForm = false">
      <div class="adm-dialog">
        <h3>{{ editingId ? '编辑题目' : '新增题目' }}</h3>
        <div class="adm-form">
          <div class="adm-field">
            <label>题干</label>
            <textarea v-model="form.name" class="adm-textarea" rows="2" placeholder="下列各项中，应计入存货采购成本的有（ ）" />
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>类型</label>
              <select v-model.number="form.type" class="adm-sel">
                <option :value="1">判断题</option>
                <option :value="2">单选题</option>
                <option :value="3">多选题</option>
                <option :value="5">简答题</option>
              </select>
            </div>
            <div class="adm-field">
              <label>难度</label>
              <select v-model.number="form.difficulty" class="adm-sel">
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
            </div>
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>分值</label>
              <input v-model.number="form.score" class="adm-input" type="number" min="1" max="100" />
            </div>
            <div class="adm-field">
              <label>所属分类（三级）</label>
              <select v-model="form.cateId" class="adm-sel">
                <option value="">请选择三级分类</option>
                <option v-for="c in leafCates" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
          </div>
          <div v-if="form.type !== 1 && form.type !== 5" class="adm-field">
            <label>选项（每行一个，不需要 A. 前缀）</label>
            <textarea v-model="optionsText" class="adm-textarea" rows="4" placeholder="买价&#10;运输途中的合理损耗&#10;入库前的挑选整理费&#10;采购人员的差旅费" />
          </div>
          <div class="adm-row2">
            <div class="adm-field">
              <label>答案</label>
              <input v-model="form.answer" class="adm-input" placeholder="如 A 或 ABC 或 true" />
            </div>
            <div class="adm-field">
              <label>解析（5~300 字）</label>
              <input v-model="form.analysis" class="adm-input" placeholder="可选" />
            </div>
          </div>
        </div>
        <div class="adm-actions">
          <button class="btn ghost" @click="showForm = false">取消</button>
          <button class="btn primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
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
const query = reactive({ pageNo: 1, pageSize: 8, keyword: '', difficulty: '' })
const msg = ref('')
const ok = ref(true)

const showForm = ref(false)
const saving = ref(false)
const editingId = ref('')
const optionsText = ref('')
const leafCates = ref([])
const cateMap = ref({})
const form = reactive({
  name: '', type: 2, difficulty: 2, score: 5, cateId: '', answer: '', analysis: ''
})

const typeText = (t) => ({ 1: '判断题', 2: '单选题', 3: '多选题', 4: '填空题', 5: '简答题' }[t] || '未知')
const diffText = (d) => ({ 1: '简单', 2: '中等', 3: '困难' }[d] || '-')

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminQuestionPage({
      pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      difficulty: query.difficulty === '' ? undefined : query.difficulty
    })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    flash('加载失败：' + e.message, false)
  }
}

// 后端要求 cateIds 三个（一二三级）
function buildCateIds() {
  if (!form.cateId) return []
  const chain = []
  let cur = cateMap.value[form.cateId]
  let guard = 0
  while (cur && guard++ < 10) {
    chain.unshift(cur.id)
    cur = cateMap.value[cur.parentId]
  }
  return chain.slice(0, 3)
}

function openCreate() {
  editingId.value = ''
  showForm.value = true
  optionsText.value = ''
  Object.assign(form, { name: '', type: 2, difficulty: 2, score: 5, cateId: '', answer: '', analysis: '' })
}

async function openEdit(q) {
  try {
    const d = await api.questionDetail(q.id)
    editingId.value = d.id
    showForm.value = true
    optionsText.value = (d.options || []).join('\n')
    Object.assign(form, {
      name: d.name || '',
      type: d.type || 2,
      difficulty: d.difficulty || 2,
      score: d.score || 5,
      cateId: d.cateId3 && d.cateId3 !== '0' ? d.cateId3 : '',
      answer: d.answer || '',
      analysis: d.analysis || ''
    })
  } catch (e) {
    flash('题目详情加载失败：' + e.message, false)
  }
}

async function save() {
  if (!form.name.trim() || !form.answer.trim()) {
    flash('题干和答案不能为空', false)
    return
  }
  if (!form.cateId) {
    flash('题目必须关联一个三级分类', false)
    return
  }
  const analysis = form.analysis.trim()
  if (analysis.length < 5 || analysis.length > 300) {
    flash('解析长度需 5~300 字（后端校验）', false)
    return
  }
  saving.value = true
  try {
    const options = (form.type === 1 || form.type === 5)
      ? []
      : optionsText.value.split('\n').map((x) => x.trim()).filter(Boolean)
    const dto = {
      name: form.name.trim(),
      type: form.type,
      difficulty: form.difficulty,
      score: form.score,
      cateIds: buildCateIds(),
      options,
      answer: form.answer.trim(),
      analysis
    }
    if (editingId.value) {
      await api.updateQuestion(editingId.value, dto)
      flash('题目已保存')
    } else {
      await api.createQuestion(dto)
      flash('题目已新增')
    }
    showForm.value = false
    await load(editingId.value ? query.pageNo : 1)
  } catch (e) {
    flash('保存失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}

async function remove(q) {
  if (!window.confirm(`确定删除题目「${q.name}」？`)) return
  try {
    await api.deleteQuestion(q.id)
    flash('已删除')
    await load(query.pageNo)
  } catch (e) {
    flash('删除失败：' + e.message, false)
  }
}

onMounted(async () => {
  await load(1)
  try {
    const all = (await api.adminCategoryTree()) || []
    cateMap.value = Object.fromEntries(all.map((c) => [c.id, c]))
    leafCates.value = all.filter((c) => Number(c.level) === 3)
  } catch (e) { leafCates.value = [] }
})
</script>
