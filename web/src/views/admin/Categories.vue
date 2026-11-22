<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <select v-model="level" class="adm-sel">
        <option value="">全部层级</option>
        <option :value="1">一级分类</option>
        <option :value="2">二级分类</option>
        <option :value="3">三级分类</option>
      </select>
      <input v-model="keyword" class="adm-input" placeholder="分类名称" style="width:180px" @keyup.enter="render" />
      <button class="btn primary adm-btn-sm" @click="render">筛选</button>
      <button class="btn ghost adm-btn-sm" @click="openCreate">+ 新增分类</button>
      <span class="muted" style="font-size:12px;margin-left:auto">
        共 {{ all.length }} 个分类 · 一级 {{ level1Count }} 个（最多显示 60 条）
      </span>
    </div>

    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>分类</th><th>ID</th><th>上级</th><th>层级</th><th>课程数</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in rows" :key="c.id">
            <td>{{ c.name }}</td>
            <td class="adm-mono">{{ c.id }}</td>
            <td class="adm-mono">{{ c.parentId === '0' ? '—' : c.parentId }}</td>
            <td>{{ c.level || '-' }}</td>
            <td>{{ c.courseNum ?? '-' }}</td>
            <td><span class="adm-badge" :class="c.status === 1 ? 'green' : 'red'">{{ c.statusDesc || (c.status === 1 ? '启用' : '禁用') }}</span></td>
            <td style="white-space:nowrap">
              <button class="btn ghost adm-btn-sm" @click="openEdit(c)">编辑</button>
              <button class="btn ghost adm-btn-sm" style="margin-left:6px" @click="toggle(c)">
                {{ c.status === 1 ? '禁用' : '启用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!rows.length" class="adm-empty">没有分类</div>
      <div class="adm-pager"><span class="muted" style="font-size:12px">显示前 {{ rows.length }} 条</span></div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>

    <div v-if="showForm" class="adm-modal" @click.self="showForm = false">
      <div class="adm-dialog" style="width:520px">
        <h3>{{ editingId ? '编辑分类' : '新增分类' }}</h3>
        <div class="adm-form">
          <div v-if="!editingId" class="adm-field">
            <label>上级分类（留空为一级分类）</label>
            <select v-model="form.parentId" class="adm-sel">
              <option value="0">（一级分类）</option>
              <option v-for="c in parents" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="adm-field">
            <label>分类名称</label>
            <input v-model="form.name" class="adm-input" placeholder="例如：会计实操" />
          </div>
          <div class="adm-field">
            <label>排序（越小越靠前）</label>
            <input v-model.number="form.index" class="adm-input" type="number" min="1" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const all = ref([])
const level = ref('')
const keyword = ref('')
const level1Count = computed(() => all.value.filter((c) => String(c.parentId) === '0').length)
const parents = computed(() => all.value.filter((c) => Number(c.level) <= 2))
const showForm = ref(false)
const saving = ref(false)
const editingId = ref('')
const form = reactive({ parentId: '0', name: '', index: 1 })
const msg = ref('')
const ok = ref(true)

const rows = computed(() => {
  let list = all.value
  if (level.value !== '') {
    list = list.filter((c) => Number(c.level) === Number(level.value))
  }
  if (keyword.value.trim()) {
    const k = keyword.value.trim()
    list = list.filter((c) => String(c.name || '').includes(k))
  }
  return list.slice(0, 60)
})

function render() { /* computed 自动刷新 */ }

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

async function reload() {
  all.value = (await api.adminCategoryTree()) || []
}

function openCreate() {
  editingId.value = ''
  Object.assign(form, { parentId: '0', name: '', index: 1 })
  showForm.value = true
}

function openEdit(c) {
  editingId.value = c.id
  Object.assign(form, { parentId: String(c.parentId || '0'), name: c.name || '', index: c.index || 1 })
  showForm.value = true
}

async function save() {
  if (!form.name.trim()) {
    flash('分类名称不能为空', false)
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.updateCategory({ id: editingId.value, name: form.name.trim(), index: form.index || 1 })
      flash('分类已保存')
    } else {
      await api.createCategory({
        parentId: form.parentId === '0' ? 0 : form.parentId,
        name: form.name.trim(),
        index: form.index || 1
      })
      flash('分类已新增')
    }
    showForm.value = false
    await reload()
  } catch (e) {
    flash('保存失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}

async function toggle(c) {
  try {
    await api.toggleCategory({ id: c.id, status: c.status === 1 ? 0 : 1 })
    flash(c.status === 1 ? '已禁用' : '已启用')
    await reload()
  } catch (e) {
    flash('操作失败：' + e.message, false)
  }
}

onMounted(async () => {
  try {
    await reload()
  } catch (e) {
    all.value = []
  }
})
</script>
