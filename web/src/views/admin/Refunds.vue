<template>
  <div class="adm-content">
    <div class="adm-panel adm-filter">
      <select v-model="query.refundStatus" class="adm-sel" @change="load(1)">
        <option value="">全部状态</option>
        <option :value="1">待审批</option>
        <option :value="3">同意退款</option>
        <option :value="4">拒绝退款</option>
        <option :value="5">退款成功</option>
      </select>
      <span class="muted" style="font-size:12px">共 {{ total }} 条</span>
      <button class="btn ghost adm-btn-sm" style="margin-left:auto" @click="load(query.pageNo)">刷新</button>
    </div>

    <div class="adm-panel">
      <table class="adm-tbl">
        <thead><tr><th>退款单</th><th>学员</th><th>手机号</th><th>金额</th><th>状态</th><th>申请时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="adm-mono">{{ r.id }}</td>
            <td>{{ r.proposerName || '-' }}</td>
            <td class="adm-mono">{{ r.proposerMobile || '-' }}</td>
            <td>{{ (Number(r.refundAmount || 0) / 100).toFixed(2) }} 元</td>
            <td><span class="adm-badge" :class="cls(r.status)">{{ r.refundStatusDesc }}</span></td>
            <td class="muted">{{ fmt(r.createTime) }}</td>
            <td>
              <template v-if="r.status === 1">
                <button class="btn primary adm-btn-sm" @click="approve(r, 1)">同意</button>
                <button class="btn ghost adm-btn-sm" style="margin-left:6px" @click="approve(r, 2)">拒绝</button>
              </template>
              <span v-else class="muted" style="font-size:12px">已处理</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="adm-empty">没有符合条件的退款单</div>
      <div class="adm-pager">
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo <= 1" @click="load(query.pageNo - 1)">上一页</button>
        <span class="muted" style="font-size:12px">第 {{ query.pageNo }} / {{ pages || 1 }} 页</span>
        <button class="btn ghost adm-btn-sm" :disabled="query.pageNo >= pages" @click="load(query.pageNo + 1)">下一页</button>
      </div>
    </div>
    <div v-if="msg" class="notice" :class="ok ? 'ok' : 'bad'">{{ msg }}</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../../api'

const list = ref([])
const total = ref(0)
const pages = ref(1)
const query = reactive({ pageNo: 1, pageSize: 8, refundStatus: 1 })
const msg = ref('')
const ok = ref(true)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
const cls = (s) => (s === 5 ? 'green' : (s === 1 ? 'warn' : (s === 4 ? 'red' : '')))

function flash(t, good = true) {
  msg.value = t
  ok.value = good
  setTimeout(() => { msg.value = '' }, 3000)
}

async function load(pageNo) {
  query.pageNo = pageNo
  try {
    const data = await api.adminRefundPage({
      pageNo,
      pageSize: query.pageSize,
      refundStatus: query.refundStatus === '' ? undefined : query.refundStatus
    })
    list.value = (data && data.list) || []
    total.value = Number((data && data.total) || 0)
    pages.value = Number((data && data.pages) || 1)
  } catch (e) {
    flash('加载失败：' + e.message, false)
  }
}

async function approve(r, approveType) {
  try {
    await api.approveRefund({
      id: r.id,
      approveType,
      approveOpinion: approveType === 1 ? '同意退款' : '不符合退款条件',
      remark: ''
    })
    flash(approveType === 1 ? '已同意，退款处理中' : '已拒绝该退款申请')
    await load(query.pageNo)
  } catch (e) {
    flash('审批失败：' + e.message, false)
  }
}

onMounted(() => load(1))
</script>
