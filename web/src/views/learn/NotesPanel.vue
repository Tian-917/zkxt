<template>
  <div class="panel-block">
    <div class="head">
      <b>课堂笔记</b>
      <span class="muted">本节共 {{ list.length }} 条</span>
      <label class="muted" style="margin-left:auto;display:flex;align-items:center;gap:6px;font-size:12px">
        <input v-model="onlyMine" type="checkbox" @change="load" /> 只看我的
      </label>
    </div>

    <div class="compose">
      <textarea v-model="content" class="input area" rows="2"
                placeholder="记点什么…比如这一节的结论、易错点" />
      <div class="compose-foot">
        <label class="muted" style="display:flex;align-items:center;gap:6px;font-size:12px">
          <input v-model="isPrivate" type="checkbox" /> 仅自己可见
        </label>
        <button class="btn primary" :disabled="saving || !content.trim()" @click="save">
          {{ saving ? '保存中…' : '发布笔记' }}
        </button>
      </div>
    </div>

    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'" style="margin-bottom:10px">{{ msg }}</div>

    <div v-if="loading" class="muted" style="font-size:13px">加载中…</div>
    <div v-else-if="!list.length" class="muted" style="font-size:13px;padding:12px 0">
      本节还没有笔记，写下第一条吧
    </div>

    <article v-for="n in list" :key="n.id" class="note">
      <div class="note-head">
        <span class="who">{{ n.authorName || '我' }}</span>
        <span v-if="n.isPrivate" class="badge">私密</span>
        <span class="muted" style="margin-left:auto;font-size:11px">{{ fmt(n.createTime) }}</span>
      </div>
      <p class="note-body">{{ n.content }}</p>
      <div class="note-foot">
        <button class="btn ghost sm" @click="toggleGather(n)">{{ n.isGathered ? '已收藏 ✓' : '收藏' }}</button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../../api'

const props = defineProps({
  courseId: { type: [String, Number], required: true },
  chapterId: { type: [String, Number], default: '' },
  sectionId: { type: [String, Number], required: true }
})

const list = ref([])
const loading = ref(false)
const onlyMine = ref(false)
const content = ref('')
const isPrivate = ref(false)
const saving = ref(false)
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
    const data = await api.notes({
      courseId: props.courseId,
      sectionId: props.sectionId,
      onlyMine: onlyMine.value
    })
    list.value = (data && data.list) || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await api.saveNote({
      content: content.value.trim(),
      noteMoment: 0,
      isPrivate: isPrivate.value,
      courseId: props.courseId,
      chapterId: props.chapterId,
      sectionId: props.sectionId
    })
    content.value = ''
    flash('笔记已发布')
    await load()
  } catch (e) {
    flash('发布失败：' + e.message, false)
  } finally {
    saving.value = false
  }
}

async function toggleGather(n) {
  try {
    if (n.isGathered) await api.ungatherNote(n.id)
    else await api.gatherNote(n.id)
    n.isGathered = !n.isGathered
  } catch (e) {
    flash('操作失败：' + e.message, false)
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
.note { border-top: 1px dashed var(--line); padding: 12px 0; }
.note-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.who { font-weight: 600; color: #1e293b; }
.note-body { margin: 8px 0; font-size: 13px; color: #475569; line-height: 1.8; white-space: pre-wrap; }
.btn.sm { padding: 3px 9px; font-size: 12px; }
</style>
