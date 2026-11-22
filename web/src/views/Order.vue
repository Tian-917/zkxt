<template>
  <main class="wrap">
    <PageHeader title="确认订单" />

    <div v-if="loading" class="center muted" style="padding:60px 0">加载中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>

    <template v-else>
      <!-- 第 3 步：支付成功 -->
      <div v-if="step === 'done'" class="panel center" style="padding:40px">
        <div style="font-size:44px">✅</div>
        <h2 style="margin:12px 0 6px;font-size:18px">支付成功</h2>
        <p class="muted">订单 {{ orderId }} 已完成，课程已加入我的课表</p>
        <div class="row" style="justify-content:center;margin-top:18px">
          <button class="btn primary" @click="$router.push('/learn/' + courseIds[0])">去学习</button>
          <button class="btn ghost" @click="$router.push('/orders')">查看订单</button>
        </div>
      </div>

      <template v-else>
        <div class="panel">
          <h3 style="font-size:15px;margin-bottom:12px">课程清单</h3>
          <div v-for="c in courses" :key="c.courseId" class="line">
            <span>{{ c.courseName || c.name }}</span>
            <b>{{ fen(c.price) }}</b>
          </div>
          <div class="line total">
            <span>课程合计</span>
            <b>{{ fen(amount) }}</b>
          </div>
        </div>

        <div class="panel">
          <div class="coupon-line">
            <span class="cl-label">优惠券</span>
            <select v-model.number="couponIdx" class="cl-select">
              <option :value="-1">不使用优惠券</option>
              <option v-for="(c, i) in coupons" :key="i" :value="i">
                {{ (c.rules || []).join(' + ') }}（减 {{ fen(c.discountAmount) }}）
              </option>
            </select>
            <span class="muted" style="font-size:12px">
              {{ coupons.length ? '共 ' + coupons.length + ' 个可用方案' : '当前没有可用优惠券' }}
            </span>
          </div>
          <div class="amount-box">
            <div class="amt"><span>订单总额</span><b>¥{{ (amount / 100).toFixed(2) }}</b></div>
            <div class="amt"><span>优惠金额</span><b class="minus">-¥{{ (discount / 100).toFixed(2) }}</b></div>
            <div class="amt pay"><span>应付金额</span><b>¥{{ (payable / 100).toFixed(2) }}</b></div>
          </div>
        </div>

        <!-- 第 2 步：选支付渠道 -->
        <div v-if="step === 'pay'" class="panel">
          <h3 style="font-size:15px;margin-bottom:12px">选择支付方式</h3>
          <div class="channels">
            <button
              v-for="ch in channels" :key="ch.channelCode"
              class="chan" :class="{ on: channel === ch.channelCode }"
              @click="channel = ch.channelCode">
              <span class="chan-name">{{ ch.name }}</span>
              <span class="muted">{{ ch.channelCode }}</span>
            </button>
          </div>
          <div class="row" style="margin-top:16px">
            <button class="btn primary" :disabled="busy" @click="pay">
              {{ busy ? '支付中…' : '确认支付' }}
            </button>
            <span v-if="channel === 'mock'" class="muted">
              确认支付后立即开通课程
            </span>
          </div>
          <div v-if="payUrl" class="notice" style="margin-top:12px">
            支付链接：<code>{{ payUrl }}</code>
          </div>
        </div>

        <!-- 第 1 步：提交订单 -->
        <div v-else class="panel">
          <button class="btn primary" :disabled="busy" @click="submit">
            {{ busy ? '提交中…' : '提交订单' }}
          </button>
        </div>
      </template>

      <div v-if="msg" class="notice bad" style="margin-top:12px">{{ msg }}</div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, fen } from '../api'

const route = useRoute()

// 注意：课程 id 可能是 19 位雪花号，超过 JS 安全整数范围，必须按字符串传，不能 Number()
const courseIds = ref(String(route.query.courseIds || '').split(',').filter(Boolean))
const courses = ref([])
const orderId = ref(null)
const amount = ref(0)
const step = ref('confirm')          // confirm -> pay -> done
const channels = ref([])
const channel = ref('mock')
const coupons = ref([])
const couponIdx = ref(-1)
const payable = ref(0)
const payUrl = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const msg = ref('')

onMounted(async () => {
  try {
    if (!courseIds.value.length) {
      error.value = '缺少课程参数'
      return
    }
    const pre = await api.prePlaceOrder(courseIds.value)
    orderId.value = pre.orderId
    amount.value = pre.totalAmount || 0
    courses.value = (pre.courses || []).map((c) => ({
      courseId: c.courseId,
      courseName: c.name || c.courseName,
      price: c.price || 0
    }))
    payable.value = amount.value
    await loadCoupons()
    channels.value = await api.payChannels()
    const mock = channels.value.find((c) => c.channelCode === 'mock')
    if (mock) channel.value = 'mock'
    else if (channels.value.length) channel.value = channels.value[0].channelCode
  } catch (e) {
    error.value = '下单信息加载失败：' + e.message
  } finally {
    loading.value = false
  }
})

/** 按课程算可用优惠券方案（promotion 需要 cateId，所以先查课程详情） */
async function loadCoupons() {
  try {
    const courseList = []
    for (const c of courses.value) {
      const detail = await api.courseDetail(c.courseId)
      courseList.push({ id: c.courseId, cateId: detail.thirdCateId, price: c.price })
    }
    coupons.value = (await api.availableCoupons(courseList)) || []
  } catch (e) {
    coupons.value = []
  }
}

function currentCoupon() {
  const i = couponIdx.value
  return i >= 0 && coupons.value[i] ? coupons.value[i] : null
}

const discount = computed(() => {
  const c = currentCoupon()
  return (c && c.discountAmount) || 0
})

function refreshPayable() {
  const c = currentCoupon()
  payable.value = Math.max(0, amount.value - ((c && c.discountAmount) || 0))
}

watch(couponIdx, refreshPayable)

async function submit() {
  busy.value = true
  msg.value = ''
  try {
    const coupon = currentCoupon()
    const res = await api.placeOrder({
      orderId: orderId.value,
      courseIds: courseIds.value,
      couponIds: coupon ? coupon.ids : []
    })
    amount.value = res.payAmount || amount.value
    payable.value = amount.value
    step.value = 'pay'
  } catch (e) {
    msg.value = '提交订单失败：' + e.message
  } finally {
    busy.value = false
  }
}

async function pay() {
  busy.value = true
  msg.value = ''
  try {
    payUrl.value = await api.applyPay({ orderId: orderId.value, payChannelCode: channel.value })
    if (channel.value === 'mock') {
      await api.mockPay(orderId.value)
      // 轮询订单状态，等 MQ 回调把订单置为已支付
      for (let i = 0; i < 10; i++) {
        const st = await api.orderStatus(orderId.value)
        if (Number(st.status) === 2) break
        await new Promise((r) => setTimeout(r, 500))
      }
      step.value = 'done'
    } else {
      msg.value = '已生成支付链接（该渠道未接入真实商户，无法完成支付）'
    }
  } catch (e) {
    msg.value = '支付失败：' + e.message
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.line { display: flex; justify-content: space-between; padding: 9px 0; font-size: 13px; border-bottom: 1px dashed var(--line); }
.line.total { border-bottom: none; font-size: 14px; }
.line.total b { color: #e11d48; font-size: 20px; }
.channels { display: flex; gap: 12px; }
.chan {
  flex: 1; border: 1px solid var(--line); background: #fff; border-radius: 10px;
  padding: 14px; cursor: pointer; display: flex; flex-direction: column; gap: 4px; align-items: flex-start;
}
.chan.on { border-color: #93c5fd; background: #eef2ff; }
.chan-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.coupon-line { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; border-bottom: 1px dashed var(--line); }
.cl-label { font-size: 13px; color: #475569; }
.cl-select {
  flex: 1; max-width: 460px; border: 1px solid var(--line); border-radius: 8px;
  padding: 8px 10px; font-size: 13px; color: #334155; background: #fff; outline: none;
}
.amount-box { display: flex; justify-content: flex-end; gap: 28px; padding-top: 12px; }
.amt { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; font-size: 12px; color: #64748b; }
.amt b { font-size: 16px; color: #334155; }
.amt .minus { color: #059669; }
.amt.pay b { font-size: 22px; color: #e11d48; }
</style>
