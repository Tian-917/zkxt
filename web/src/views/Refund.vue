<template>
  <main class="wrap">
    <PageHeader title="退款" />

    <div v-if="orderDetailId" class="panel">
      <h3 style="font-size:15px;margin-bottom:12px">申请退款</h3>
      <div v-if="formMsg" class="notice" :class="formOk ? 'ok' : 'bad'">{{ formMsg }}</div>
      <label class="muted" style="font-size:12px">退款原因</label>
      <select v-model="reason" class="sel" style="width:100%;margin:8px 0 12px">
        <option v-for="r in reasons" :key="r" :value="r">{{ r }}</option>
      </select>
      <label class="muted" style="font-size:12px">补充说明</label>
      <textarea v-model="desc" class="input area" rows="3" style="margin:8px 0 12px"
                placeholder="说明一下具体情况，便于审核" />
      <button class="btn primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : '提交退款申请' }}
      </button>
    </div>

    <div class="panel">
      <div class="row" style="justify-content:space-between;margin-bottom:10px">
        <b style="font-size:14px">我的退款申请</b>
        <button class="btn ghost" style="padding:6px 12px;font-size:12px" @click="load">刷新</button>
      </div>
      <div v-if="loading" class="muted">加载中…</div>
      <div v-else-if="!list.length" class="muted" style="font-size:13px">还没有退款申请</div>
      <div v-for="r in list" :key="r.id" class="refund-line">
        <div style="flex:1">
          <div style="font-size:13px;color:#1e293b">退款单 {{ r.id }}</div>
          <div class="muted" style="font-size:11px;margin-top:2px">
            金额 ¥{{ ((r.refundAmount || 0) / 100).toFixed(2) }} · {{ fmt(r.createTime) }}
          </div>
        </div>
        <span class="badge" :class="r.status === 2 ? 'green' : (r.status === 3 ? 'red' : '')">
          {{ r.refundStatusDesc || statusText(r.status) }}
        </span>
        <button v-if="r.status === 1" class="btn ghost" style="padding:5px 11px;font-size:12px"
                @click="cancelRefund(r)">撤销申请</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const orderDetailId = ref(route.query.orderDetailId || '')
const orderId = ref(route.query.orderId || '')

const reasons = ['课程内容与描述不符', '重复购买', '误购', '考前计划变更', '其他原因']
const reason = ref(reasons[0])
const desc = ref('')
const submitting = ref(false)
const formMsg = ref('')
const formOk = ref(false)

const list = ref([])
const loading = ref(true)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
async function cancelRefund(r) {
  if (!window.confirm('确定撤销这笔退款申请？')) return
  try {
    await api.cancelRefund({ id: r.id, orderDetailId: r.orderDetailId })
    await load()
  } catch (e) {
    window.alert('撤销失败：' + e.message)
  }
}

const statusText = (s) => ({ 1: '待审核', 2: '退款成功', 3: '已拒绝', 4: '已取消' }[s] || '处理中')

async function submit() {
  submitting.value = true
  formMsg.value = ''
  try {
    await api.applyRefund({
      orderDetailId: orderDetailId.value,
      refundReason: reason.value,
      questionDesc: desc.value.trim() || reason.value
    })
    formOk.value = true
    formMsg.value = '退款申请已提交，等待审核'
    desc.value = ''
    await load()
  } catch (e) {
    formOk.value = false
    formMsg.value = '提交失败：' + e.message
  } finally {
    submitting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await api.myRefunds({ pageNo: 1, pageSize: 50 })
// 按创建时间倒序
    list.value = ((data && data.list) || []).slice().sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.sel {
  border: 1px solid var(--line); border-radius: 8px; padding: 8px 10px;
  font-size: 13px; color: #334155; background: #fff; outline: none;
}
.area { width: 100%; resize: vertical; }
.refund-line { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px dashed var(--line); }
.refund-line:last-child { border-bottom: none; }
.badge.green { background: #ecfdf5; color: #059669; }
.badge.red { background: #fef2f2; color: #b91c1c; }
</style>
