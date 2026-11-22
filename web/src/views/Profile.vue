<template>
  <main class="wrap">
    <PageHeader title="个人中心">
<span>user / learning / trade / promotion</span>
      </PageHeader>

    <div class="my-layout">
      <!-- 左侧菜单（对齐原版：我的课程/我的考试/我的订单/我的优惠券/我的积分/个人设置） -->
      <aside class="my-side">
        <button v-for="m in menus" :key="m.key" class="my-menu" :class="{ on: tab === m.key }" @click="switchTab(m.key)">
          <span class="ico">{{ m.icon }}</span>
          <span>{{ m.label }}</span>
        </button>
        <button class="my-menu logout" @click="logout">
          <span class="ico">↩</span><span>退出</span>
        </button>
      </aside>

      <section class="my-main">
        <!-- 我的课程 -->
        <div v-if="tab === 'lessons'" class="my-panel">
          <div class="my-head">
            <h3>我的课程</h3>
            <span class="muted">共 {{ lessons.length }} 门</span>
            <router-link to="/search" class="muted" style="margin-left:auto;font-size:12px">去选课 →</router-link>
          </div>
          <div v-if="lessonsLoading" class="adm-empty">加载中…</div>
          <div v-else-if="!lessons.length" class="adm-empty">还没有课程，去首页挑一门吧</div>
          <div v-for="l in lessons" :key="l.id" class="lesson-row" @click="$router.push('/learn/' + l.courseId)">
            <span class="dot"></span>
            <div style="flex:1">
              <div class="lesson-name">{{ l.courseName }}</div>
              <div class="muted" style="font-size:11px;margin-top:3px">
                已学 {{ l.learnedSections || 0 }}/{{ l.sections || 0 }} 节
                <span v-if="l.latestSectionName"> · 最近：{{ l.latestSectionName }}</span>
              </div>
              <div class="progress"><i :style="{ width: pct(l) + '%' }"></i></div>
            </div>
            <span class="badge" :class="{ green: pct(l) >= 100 }">{{ pct(l) >= 100 ? '已学完' : pct(l) + '%' }}</span>
          </div>
        </div>

        <!-- 学习计划 -->
        <div v-if="tab === 'plans'" class="my-panel">
          <div class="my-head">
            <h3>学习计划</h3>
            <span class="muted">本周完成 {{ planStat.finished || 0 }}/{{ planStat.total || 0 }}</span>
            <span class="muted" style="margin-left:auto">本周积分 {{ planStat.points || 0 }}</span>
          </div>

          <div v-if="!planList.length" class="adm-empty">还没有学习计划，下面给课程设个每周频次吧</div>
          <table v-else class="adm-tbl">
            <thead><tr><th>课程</th><th>本周进度</th><th>课程进度</th><th>最近学习</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="p in planList" :key="p.courseId">
                <td>{{ p.courseName }}</td>
                <td>{{ p.weekLearnedSections || 0 }}/{{ p.weekFreq || 0 }} 节</td>
                <td>{{ p.learnedSections || 0 }}/{{ p.sections || 0 }} 节</td>
                <td>{{ fmt(p.latestLearnTime) || '-' }}</td>
                <td><button class="btn ghost adm-btn-sm" @click="$router.push('/learn/' + p.courseId)">去学习</button></td>
              </tr>
            </tbody>
          </table>

          <div class="plan-set">
            <span class="muted" style="font-size:12px">给课程设置每周学习频次</span>
            <select v-model="planForm.courseId" class="adm-sel">
              <option value="">选择课程</option>
              <option v-for="l in lessons" :key="l.courseId" :value="l.courseId">{{ l.courseName }}</option>
            </select>
            <input v-model.number="planForm.freq" class="adm-input" type="number" min="1" max="14" style="width:90px" />
            <span class="muted" style="font-size:12px">节 / 周</span>
            <button class="btn primary adm-btn-sm" :disabled="!planForm.courseId || planSaving" @click="savePlan">
              {{ planSaving ? '保存中…' : '保存计划' }}
            </button>
          </div>
          <div v-if="planMsg" class="notice ok" style="margin-top:10px">{{ planMsg }}</div>
        </div>

        <!-- 我的考试 -->
        <div v-if="tab === 'exams'" class="my-panel">
          <div class="my-head"><h3>我的考试</h3><span class="muted">共 {{ exams.length }} 次</span></div>
          <div v-if="!exams.length" class="adm-empty">还没有考试记录</div>
          <div v-for="e in exams" :key="e.id" class="exam-row">
            <div style="flex:1">
              <div class="lesson-name">{{ e.sectionName || '练习' }}</div>
              <div class="muted" style="font-size:11px;margin-top:3px">
                {{ e.courseName }} · {{ e.type === 1 ? '练习' : '考试' }} · 用时 {{ e.duration || 0 }} 秒
              </div>
            </div>
            <b :class="(e.score || 0) >= 60 ? 'pass' : 'fail'">{{ e.score ?? '-' }} 分</b>
            <span class="muted" style="font-size:11px;width:130px;text-align:right">{{ fmt(e.commitTime) }}</span>
          </div>
        </div>

        <!-- 我的订单 -->
        <div v-if="tab === 'orders'" class="my-panel">
          <div class="my-head">
            <h3>我的订单</h3><span class="muted">共 {{ orders.length }} 笔</span>
            <router-link to="/orders" class="muted" style="margin-left:auto;font-size:12px">查看全部 →</router-link>
          </div>
          <div v-if="!orders.length" class="adm-empty">还没有订单</div>
          <div v-for="o in orders" :key="o.id" class="exam-row">
            <div style="flex:1">
              <div class="lesson-name">{{ (o.details || []).map((d) => d.name).filter(Boolean).join('、') || ('订单 ' + o.id) }}</div>
              <div class="muted" style="font-size:11px;margin-top:3px">{{ fmt(o.createTime) }}</div>
            </div>
            <b class="price">¥{{ ((o.realAmount || 0) / 100).toFixed(0) }}</b>
            <span class="badge" :class="o.status === 2 || o.status === 4 ? 'green' : ''">{{ o.statusDesc }}</span>
          </div>
        </div>

        <!-- 我的优惠券 -->
        <div v-if="tab === 'coupons'" class="my-panel">
          <div class="my-head">
            <h3>我的优惠券</h3>
            <div class="subtabs">
              <button v-for="s in couponTabs" :key="s.v" class="subtab" :class="{ on: couponStatus === s.v }"
                      @click="switchCouponStatus(s.v)">{{ s.label }}</button>
            </div>
          </div>

          <div class="exchange-bar">
            <span class="muted" style="font-size:12px">有兑换码？</span>
            <input v-model.trim="exchangeInput" class="input" placeholder="输入兑换码，例如 7GF4K9P2" @keyup.enter="doExchange" />
            <button class="btn primary" style="padding:7px 16px;font-size:13px"
                    :disabled="exchanging || !exchangeInput" @click="doExchange">
              {{ exchanging ? '兑换中…' : '立即兑换' }}
            </button>
          </div>
          <div v-if="exchangeMsg" class="notice" :class="exchangeOk ? 'ok' : 'bad'" style="margin-bottom:12px">{{ exchangeMsg }}</div>

          <div v-if="!coupons.length" class="adm-empty">这个状态下没有优惠券</div>
          <div class="coupon-grid">
            <div v-for="c in coupons" :key="c.id" class="coupon-card" :class="{ disabled: couponStatus !== 1 }">
              <div class="cc-left">
                <div class="cc-amount">{{ couponAmount(c) }}</div>
                <div class="cc-cond">{{ couponCond(c) }}</div>
              </div>
              <div class="cc-right">
                <div class="cc-name">{{ c.name }}</div>
                <div class="cc-scope">{{ scopeText(c) }}</div>
                <div class="cc-term">有效期至 {{ fmt(c.termEndTime) || (c.termDays ? c.termDays + ' 天' : '-') }}</div>
              </div>
              <span class="cc-tag">{{ couponStatusText(c) }}</span>
            </div>
          </div>

          <div class="my-head" style="margin-top:22px">
            <h3>领券中心</h3>
            <span class="muted">可手动领取的券</span>
          </div>
          <div v-if="!issuing.length" class="adm-empty">当前没有可领取的券</div>
          <div class="coupon-grid">
            <div v-for="c in issuing" :key="c.id" class="coupon-card">
              <div class="cc-left">
                <div class="cc-amount">{{ couponAmount(c) }}</div>
                <div class="cc-cond">{{ couponCond(c) }}</div>
              </div>
              <div class="cc-right">
                <div class="cc-name">{{ c.name }}</div>
                <div class="cc-scope">{{ scopeText(c) }}</div>
                <div class="cc-term">{{ c.termDays ? '领取后 ' + c.termDays + ' 天有效' : '有效期至 ' + fmt(c.termEndTime) }}</div>
              </div>
              <button class="btn primary cc-btn" :disabled="c.received || !c.available" @click="receive(c)">
                {{ c.received ? '已领取' : (c.available ? '立即领取' : '不可领') }}
              </button>
            </div>
          </div>
        </div>

        <!-- 我的积分 -->
        <div v-if="tab === 'points'" class="my-panel">
          <div class="my-head">
            <h3>我的积分</h3>
            <span class="muted">今日 +{{ points }}</span>
            <button class="btn primary" style="margin-left:auto;padding:6px 14px;font-size:12px"
                    :disabled="signing || signedToday" @click="sign">
              {{ signedToday ? '今日已签到' : (signing ? '签到中…' : '立即签到') }}
            </button>
          </div>
          <div v-if="signMsg" class="notice ok" style="margin-bottom:12px">{{ signMsg }}</div>

          <!-- 整月签到日历 -->
          <div class="calendar">
            <div class="cal-head">{{ calTitle }}</div>
            <div class="cal-grid">
              <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w" class="cal-w">{{ w }}</span>
              <span v-for="n in calLeading" :key="'b' + n" class="cal-d empty"></span>
              <span v-for="d in calDays" :key="d"
                    class="cal-d" :class="{ signed: signList.includes(d), today: d === today }">{{ d }}</span>
            </div>
            <div class="cal-summary muted">本月已签到 <b>{{ signList.length }}</b> 天</div>
          </div>

          <div class="point-detail">
            <div v-for="(p, i) in pointDetail" :key="i" class="pd-row">
              <span>{{ p.type }}</span>
              <div class="pd-bar"><i :style="{ width: (p.maxPoints ? Math.min(100, (p.points / p.maxPoints) * 100) : 100) + '%' }"></i></div>
              <b>{{ p.points }}</b>
              <span class="muted" style="font-size:11px">{{ p.maxPoints ? '/ ' + p.maxPoints : '' }}</span>
            </div>
            <div v-if="!pointDetail.length" class="muted" style="font-size:12px">今日还没有积分</div>
          </div>

          <div class="my-head" style="margin-top:22px"><h3>积分榜</h3>
            <select v-model="seasonSel" class="adm-sel" style="margin-left:auto" @change="loadBoard">
              <option value="">当前赛季</option>
              <option v-for="s in seasons" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="muted" style="font-size:12px;margin-bottom:8px">
            我的排名 {{ rank }} · {{ myBoardPoints }} 分
          </div>
          <div v-for="(b, i) in boardList.slice(0, 10)" :key="i" class="rank-row">
            <span class="rk" :class="{ top: b.rank <= 3 }">{{ b.rank }}</span>
            <span style="flex:1">{{ b.name }}</span>
            <b>{{ b.points }}</b>
          </div>
          <div v-if="!boardList.length" class="adm-empty">暂无榜单数据</div>
        </div>

        <!-- 个人设置 -->
        <div v-if="tab === 'setting'" class="my-panel">
          <div class="my-head"><h3>个人设置</h3></div>
          <div class="setting-row"><span class="muted">用户名</span><b>{{ user ? user.name : '-' }}</b></div>
          <div class="setting-row"><span class="muted">手机号</span><b>{{ user ? (user.cellPhone || '-') : '-' }}</b></div>
          <div class="setting-row"><span class="muted">用户 ID</span><b>{{ user ? user.id || user.userId : '-' }}</b></div>
          <div class="setting-row"><span class="muted">角色</span><b>{{ roleText }}</b></div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { authState, clearAuth } from '../auth'
import { refreshUnread } from '../store'

const router = useRouter()

const menus = [
  { key: 'lessons', label: '我的课程', icon: '📘' },
  { key: 'plans', label: '学习计划', icon: '📅' },
  { key: 'exams', label: '我的考试', icon: '📝' },
  { key: 'orders', label: '我的订单', icon: '🧾' },
  { key: 'coupons', label: '我的优惠券', icon: '🎟' },
  { key: 'points', label: '我的积分', icon: '🪙' },
  { key: 'setting', label: '个人设置', icon: '⚙' }
]

const tab = ref('lessons')
const user = ref(authState.user || null)
const roleText = computed(() => (authState.user && Number(authState.user.roleId) === 1 ? '员工' : '学员'))

const lessons = ref([])
const planList = ref([])
const planStat = ref({ points: 0, finished: 0, total: 0 })
const planForm = reactive({ courseId: '', freq: 7 })
const planSaving = ref(false)
const planMsg = ref('')
const lessonsLoading = ref(true)
const exams = ref([])
const orders = ref([])

const coupons = ref([])
const issuing = ref([])
const couponStatus = ref(1)
const exchangeInput = ref('')
const exchanging = ref(false)
const exchangeMsg = ref('')
const exchangeOk = ref(true)
const couponTabs = [
  { v: 1, label: '未使用' },
  { v: 2, label: '已使用' },
  { v: 3, label: '已过期' }
]

const points = ref(0)
const pointDetail = ref([])
const signList = ref([])
const signing = ref(false)
const signMsg = ref('')
const seasons = ref([])
const seasonSel = ref('')
const rank = ref('-')
const myBoardPoints = ref(0)
const boardList = ref([])

const today = new Date().getDate()
const calTitle = computed(() => {
  const d = new Date()
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`
})
const calDays = computed(() => {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()
})
const calLeading = computed(() => {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1).getDay()
})
const signedToday = computed(() => signList.value.includes(today))

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 10) : '')
const pct = (l) => (l.sections ? Math.min(100, Math.round(((l.learnedSections || 0) / l.sections) * 100)) : 0)

function couponAmount(c) {
  const type = typeof c.discountType === 'object' ? c.discountType?.value : c.discountType
  if (type === 2) return (Number(c.discountValue || 0) / 10) + ' 折'
  return '¥' + (Number(c.discountValue || 0) / 100).toFixed(0)
}
function couponCond(c) {
  return c.thresholdAmount ? `满 ${(c.thresholdAmount / 100).toFixed(0)} 可用` : '无门槛'
}
function scopeText(c) {
  if (!c.specific) return '全部课程可用'
  return '限定课程可用'
}
function couponStatusText(c) {
  return { 1: '未使用', 2: '已使用', 3: '已过期' }[couponStatus.value] || ''
}

async function logout() {
  try {
    await api.logout()
  } catch (e) {
    /* 接口失败也清本地 */
  }
  clearAuth()
  router.replace('/login')
}

async function switchTab(key) {
  tab.value = key
  await loadTab()
}

async function switchCouponStatus(v) {
  couponStatus.value = v
  await loadCoupons()
}

async function loadTab() {
  try {
    if (tab.value === 'lessons') await loadLessons()
    else if (tab.value === 'plans') await Promise.all([loadLessons(), loadPlans()])
    else if (tab.value === 'exams') await loadExams()
    else if (tab.value === 'orders') await loadOrders()
    else if (tab.value === 'coupons') await loadCoupons()
    else if (tab.value === 'points') await Promise.all([loadSign(), loadPoints(), loadBoard()])
  } catch (e) {
    /* 各 loader 自己兜底 */
  }
}

async function loadLessons() {
  lessonsLoading.value = true
  try {
    const data = await api.myLessons({ pageNo: 1, pageSize: 30 })
    lessons.value = (data && data.list) || []
  } catch (e) {
    lessons.value = []
  } finally {
    lessonsLoading.value = false
  }
}

async function loadPlans() {
  try {
    const data = await api.plans({ pageNo: 1, pageSize: 30 })
    planList.value = (data && data.list) || []
    planStat.value = {
      points: (data && data.weekPoints) || 0,
      finished: (data && data.weekFinished) || 0,
      total: (data && data.weekTotalPlan) || 0
    }
  } catch (e) {
    planList.value = []
  }
}

async function savePlan() {
  planSaving.value = true
  planMsg.value = ''
  try {
    await api.savePlan(planForm.courseId, planForm.freq || 7)
    planMsg.value = '学习计划已保存'
    await loadPlans()
  } catch (e) {
    planMsg.value = '保存失败：' + e.message
  } finally {
    planSaving.value = false
  }
}

async function loadExams() {
  try {
    const data = await api.myExams({ pageNo: 1, pageSize: 20 })
    exams.value = (data && data.list) || []
  } catch (e) {
    exams.value = []
  }
}

async function loadOrders() {
  try {
    const data = await api.myOrders({ pageNo: 1, pageSize: 10 })
    orders.value = (data && data.list) || []
  } catch (e) {
    orders.value = []
  }
}

async function loadCoupons() {
  try {
    const data = await api.myCoupons({ pageNo: 1, pageSize: 20, status: couponStatus.value })
    coupons.value = (data && data.list) || []
  } catch (e) {
    coupons.value = []
  }
  try {
    issuing.value = (await api.issuingCoupons()) || []
  } catch (e) {
    issuing.value = []
  }
}

async function doExchange() {
  const code = exchangeInput.value.trim().toUpperCase()
  if (!code) return
  exchanging.value = true
  exchangeMsg.value = ''
  try {
    await api.exchangeCoupon(code)
    exchangeOk.value = true
    exchangeMsg.value = '兑换成功，券已放入「未使用」'
    exchangeInput.value = ''
    couponStatus.value = 1
    await loadCoupons()
  } catch (e) {
    exchangeOk.value = false
    exchangeMsg.value = '兑换失败：' + e.message
  } finally {
    exchanging.value = false
  }
}

async function receive(c) {
  try {
    await api.receiveCoupon(c.id)
    c.received = true
    await loadCoupons()
  } catch (e) {
    /* 重复领取等错误忽略 */
  }
}

async function loadSign() {
  try {
// 后端返回按天的 0/1 数组
    const arr = (await api.signRecords()) || []
    signList.value = arr.map((v, i) => (Number(v) === 1 ? i + 1 : 0)).filter(Boolean)
  } catch (e) {
    signList.value = []
  }
}

async function loadPoints() {
  try {
    // 返回的是分类明细 [{type, points, maxPoints}]
    const arr = (await api.pointsToday()) || []
    if (Array.isArray(arr)) {
      pointDetail.value = arr
      points.value = arr.reduce((s, x) => s + Number(x.points || 0), 0)
    } else {
      pointDetail.value = []
      points.value = Number((arr && arr.points) || 0)
    }
  } catch (e) {
    points.value = 0
    pointDetail.value = []
  }
}

async function sign() {
  signing.value = true
  try {
    const r = await api.signIn()
    signMsg.value = `签到成功，连续 ${r.signDays} 天，+${r.rewardPoints || 0} 积分`
    await Promise.all([loadSign(), loadPoints(), refreshUnread()])
  } catch (e) {
    signMsg.value = '签到失败：' + e.message
  } finally {
    signing.value = false
  }
}

async function loadBoard() {
  try {
    if (!seasons.value.length) {
      seasons.value = (await api.boardSeasons()) || []
    }
    const data = await api.board(seasonSel.value || undefined)
    rank.value = data && data.rank != null ? data.rank : '-'
    myBoardPoints.value = (data && data.points) || 0
    boardList.value = (data && data.boardList) || []
  } catch (e) {
    boardList.value = []
  }
}

onMounted(async () => {
  try {
    if (authState.user && authState.user.userId) {
      user.value = (await api.me(authState.user.userId)) || user.value
    }
  } catch (e) { /* 静默 */ }
  await loadTab()
})
</script>

<style scoped>
.my-layout { display: grid; grid-template-columns: 220px 1fr; gap: 18px; align-items: start; }
.my-side { background: #fff; border: 1px solid var(--line); border-radius: 14px; padding: 8px; }
.my-menu {
  display: flex; align-items: center; gap: 10px; width: 100%;
  border: none; background: transparent; padding: 12px 14px; border-radius: 10px;
  font-size: 13px; color: #334155; cursor: pointer; text-align: left;
}
.my-menu:hover { background: #f5f7ff; }
.my-menu.on { background: #eef2ff; color: var(--brand); font-weight: 600; }
.my-menu .ico { width: 18px; text-align: center; }
.my-menu.logout { color: #b91c1c; margin-top: 6px; border-top: 1px solid var(--line); border-radius: 0 0 10px 10px; }
.my-main { background: #fff; border: 1px solid var(--line); border-radius: 14px; padding: 20px 24px; min-height: 480px; }
.my-head { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid var(--line); margin-bottom: 14px; }
.my-head h3 { font-size: 16px; }
.lesson-row { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px dashed var(--line); cursor: pointer; }
.lesson-row:last-child { border-bottom: none; }
.lesson-name { font-size: 13px; color: #1e293b; font-weight: 600; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; }
.progress { margin-top: 6px; height: 4px; background: #eef2f7; border-radius: 3px; overflow: hidden; }
.progress i { display: block; height: 100%; background: linear-gradient(90deg, #3b82f6, #6366f1); }
.exam-row { display: flex; align-items: center; gap: 12px; padding: 11px 0; border-bottom: 1px dashed var(--line); }
.exam-row:last-child { border-bottom: none; }
.pass { color: #059669; }
.fail { color: #b91c1c; }
.price { color: #e11d48; }
.subtabs { display: flex; gap: 6px; margin-left: auto; }
.subtab { border: 1px solid var(--line); background: #fff; border-radius: 8px; padding: 4px 12px; font-size: 12px; cursor: pointer; color: #475569; }
.subtab.on { border-color: #93c5fd; background: #eef2ff; color: var(--brand); font-weight: 600; }
.exchange-bar { display: flex; align-items: center; gap: 10px; padding: 12px 14px; margin-bottom: 14px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 10px; }
.exchange-bar .input { flex: 1; max-width: 320px; }
.coupon-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.coupon-card {
  position: relative; display: flex; align-items: stretch; gap: 12px;
  border: 1px solid #f1d9a8; border-radius: 12px; overflow: hidden;
  background: linear-gradient(135deg, #fffdf7, #fff7e6);
}
.coupon-card.disabled { filter: grayscale(1); opacity: .7; }
.cc-left { width: 96px; display: grid; place-items: center; padding: 14px 0; background: rgba(245, 158, 11, .12); }
.cc-amount { font-size: 20px; font-weight: 700; color: #b45309; }
.cc-cond { font-size: 11px; color: #a16207; margin-top: 4px; }
.cc-right { flex: 1; padding: 12px 12px 12px 0; }
.cc-name { font-size: 13px; font-weight: 600; color: #7c2d12; }
.cc-scope { font-size: 11px; color: #92400e; margin-top: 4px; }
.cc-term { font-size: 11px; color: #a3a3a3; margin-top: 4px; }
.cc-tag { position: absolute; right: 8px; top: 8px; font-size: 10px; color: #b45309; }
.cc-btn { align-self: center; margin-right: 12px; padding: 5px 12px; font-size: 12px; }
.calendar { border: 1px solid var(--line); border-radius: 12px; padding: 16px; }
.cal-head { text-align: center; font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.cal-w { text-align: center; font-size: 11px; color: #94a3b8; padding-bottom: 4px; }
.cal-d { text-align: center; font-size: 12px; padding: 7px 0; border-radius: 8px; color: #475569; background: #f8fafc; }
.cal-d.empty { background: transparent; }
.cal-d.signed { background: #eef2ff; color: var(--brand); font-weight: 700; }
.cal-d.today { outline: 2px solid #fbbf24; }
.cal-summary { text-align: center; margin-top: 12px; font-size: 12px; }
.rank-row { display: flex; align-items: center; gap: 10px; padding: 7px 0; font-size: 13px; border-bottom: 1px dashed var(--line); }
.rk { width: 20px; color: #94a3b8; font-size: 12px; }
.rk.top { color: #e11d48; font-weight: 700; }
.plan-set { display: flex; align-items: center; gap: 10px; margin-top: 16px; padding-top: 14px; border-top: 1px dashed var(--line); }
.point-detail { margin-top: 16px; display: grid; gap: 8px; }
.pd-row { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #475569; }
.pd-row > span:first-child { width: 76px; }
.pd-bar { flex: 1; height: 6px; background: #eef2f7; border-radius: 4px; overflow: hidden; }
.pd-bar i { display: block; height: 100%; background: linear-gradient(90deg, #f59e0b, #f97316); }
.setting-row { display: flex; align-items: center; gap: 16px; padding: 10px 0; border-bottom: 1px dashed var(--line); font-size: 13px; }
.setting-row .muted { width: 90px; }
</style>
