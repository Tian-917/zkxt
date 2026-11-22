<template>
  <div class="adm-content">
    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>讲师</th><th>ID</th><th>手机号</th><th>职称 / 简介</th><th>课程数</th><th>出题数</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="t in list" :key="t.id">
            <td>{{ t.name }}</td>
            <td class="adm-mono">{{ t.id }}</td>
            <td class="adm-mono">{{ t.cellPhone }}</td>
            <td class="muted" style="font-size:12px;max-width:320px">
              {{ t.job }}<br /><span style="font-size:11px">{{ t.intro }}</span>
            </td>
            <td>{{ t.courseAmount ?? 0 }}</td>
            <td>{{ t.examQuestionAmount ?? 0 }}</td>
            <td><span class="adm-badge" :class="t.status === 1 ? 'green' : 'red'">{{ t.status === 1 ? '启用' : '禁用' }}</span></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有讲师</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">共 {{ total }} 位 · 第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
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
const query = reactive({ pageNo: 1, pageSize: 8 })

async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminTeacherPage({ pageNo, pageSize: query.pageSize })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    list.value = []
  }
}

onMounted(() => load(1))
</script>
