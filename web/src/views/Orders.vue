<template>
  <main class="wrap">
    <PageHeader title="我的订单" />

    <div v-if="loading" class="center muted">加载中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>
    <div v-else-if="!list.length" class="notice">还没有订单，去首页挑一门课吧</div>

    <section v-for="o in list" :key="o.id" class="panel" style="margin-bottom:12px">
      <div class="top">
        <span class="muted">订单号 {{ o.id }}</span>
        <span class="badge" :class="o.status === 2 || o.status === 4 ? 'green' : ''">{{ o.statusDesc || statusText(o.status) }}</span>
        <span class="muted" style="margin-left:auto">{{ fmt(o.createTime) }}</span>
      </div>
      <div v-for="d in o.details || []" :key="d.id" class="item">
        <span>{{ d.name || d.courseName || ('课程 ' + d.courseId) }}</span>
        <div style="display:flex;align-items:center;gap:10px">
          <b>{{ fen(d.price || 0) }}</b>
          <button v-if="paid(o) && (o.details || []).length > 1" class="btn ghost" style="padding:3px 9px;font-size:11px"
                  @click="goLearnCourse(d)">去学习</button>
          <button v-if="d.canRefund" class="btn ghost" style="padding:3px 9px;font-size:11px"
                  @click="goRefund(o, d)">申请退款</button>
        </div>
      </div>
      <div class="foot">
        <span class="muted">共 {{ fen(o.realAmount || 0) }}（原价 {{ fen(o.totalAmount || 0) }}）</span>
        <button v-if="o.status === 1" class="btn ghost" style="margin-left:auto"
                @click="cancelOrder(o)">取消订单</button>
        <button v-if="o.status === 1" class="btn primary"
                @click="goPay(o)">去支付</button>
        <!-- 单课已支付订单保留底部按钮；多课订单每门课单独有「去学习」 -->
        <button v-else-if="paid(o) && (o.details || []).length <= 1" class="btn ghost" style="margin-left:auto"
                @click="goLearn(o)">去学习</button>
      </div>
    </section>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, fen } from '../api'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const error = ref('')

async function cancelOrder(o) {
  if (!window.confirm('确定取消这笔订单？')) return
  try {
    await api.cancelOrder(o.id)
    await load()
  } catch (e) {
    window.alert('取消失败：' + e.message)
  }
}

const statusText = (s) => ({ 1: '待支付', 2: '已支付', 3: '已关闭', 4: '已完成', 5: '已取消' }[s] || '未知')
// 已支付/已完成才允许去学习
const paid = (o) => o.status === 2 || o.status === 4
const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

function courseIdsOf(o) {
  return (o.details || []).map((d) => d.courseId).join(',')
}

function goPay(o) {
  router.push('/order?courseIds=' + courseIdsOf(o))
}

function goRefund(o, d) {
  router.push(`/refund?orderDetailId=${d.id}&orderId=${o.id}`)
}

function goLearn(o) {
  const first = (o.details || [])[0]
  if (first) router.push('/learn/' + first.courseId)
}

// 一笔订单买多门课时，每门课单独去学习
function goLearnCourse(d) {
  if (d && d.courseId) router.push('/learn/' + d.courseId)
}

onMounted(async () => {
  try {
    const data = await api.myOrders({ pageNo: 1, pageSize: 10 })
    list.value = (data && data.list) || []
  } catch (e) {
    error.value = '订单加载失败：' + e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.top { display: flex; align-items: center; gap: 10px; padding-bottom: 10px; border-bottom: 1px solid var(--line); font-size: 12px; }
.item { display: flex; justify-content: space-between; padding: 9px 0; font-size: 13px; border-bottom: 1px dashed var(--line); }
.item b { color: #e11d48; }
.foot { display: flex; align-items: center; padding-top: 10px; font-size: 12px; }
.badge.green { background: #ecfdf5; color: #059669; }
</style>
