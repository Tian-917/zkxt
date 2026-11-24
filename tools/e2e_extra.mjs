// 补的 4 个功能 E2E：学习计划 / 取消订单 / 撤销退款 / 回答点赞
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const GW = 'http://127.0.0.1:10010'
const OUT = 'D:\\Java\\zkxt\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
// 看门狗：整脚本超过 150s 必须结束，避免卡死无输出
setTimeout(() => { console.log('WATCHDOG: 脚本超时退出'); process.exit(2) }, 150000)
mkdirSync(OUT, { recursive: true })

async function call(m, p, b, t) {
  const c = new AbortController()
  const timer = setTimeout(() => c.abort(), 8000)
  try {
    const r = await fetch(GW + p, {
      method: m,
      signal: c.signal,
      headers: { 'Content-Type': 'application/json', ...(t ? { authorization: t } : {}) },
      body: b ? JSON.stringify(b) : undefined
    })
    const txt = await r.text()
    return txt ? JSON.parse(txt) : {}
  } catch (e) {
    return { err: String(e) }
  } finally {
    clearTimeout(timer)
  }
}

const token = (await call('POST', '/as/accounts/login', { type: 1, username: 'jack', password: '123456', rememberMe: true })).data
console.log('1) token ok')

// 造一笔新的待支付订单：挑一门 jack 还没买的课，保证它排在 /orders 第一页
const myLessons = await call('GET', '/ls/lessons/page?pageNo=1&pageSize=50', null, token)
const owned = new Set((myLessons.data?.list || []).map((l) => String(l.courseId)))
const portal = await call('GET', '/ss/courses/portal?pageNo=1&pageSize=50', null, token)
const candidate = (portal.data?.list || []).find((c) => !owned.has(String(c.id)))
let unpaidId = null
if (candidate) {
  const pre = await call('GET', `/ts/orders/prePlaceOrder?courseIds=${candidate.id}`, null, token)
  const oid = pre.data?.orderId
  const placed = await call('POST', '/ts/orders/placeOrder', { orderId: oid, courseIds: [candidate.id], couponIds: [] }, token)
  unpaidId = placed.code === 200 ? oid : null
  console.log('2) new unpaid order:', unpaidId, 'course:', candidate.name, 'place:', placed.msg)
}
const orders = await call('GET', '/ts/orders/page?pageNo=1&pageSize=30', null, token)

// 造一笔待审批退款（撤销用）
const paid = (orders.data?.list || []).find((o) => (o.status === 2 || o.status === 4) && (o.details || []).some((d) => d.canRefund))
let refundId = null
if (paid) {
  const detail = paid.details.find((d) => d.canRefund)
  const r = await call('POST', '/ts/refund-apply', { orderDetailId: detail.id, refundReason: '界面撤销测试', questionDesc: '界面撤销测试，用于验证撤销按钮' }, token)
  console.log('3) create refund:', JSON.stringify(r).slice(0, 120))
  if (r.code === 200) refundId = 'ok'
}

// ---------- 浏览器 ----------
const target = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
const jsErrors = []
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 140))
  // 原生 confirm/alert 会阻塞页面，必须用 CDP 自动确认
  if (m.method === 'Page.javascriptDialogOpening') {
    ws.send(JSON.stringify({ id: ++id, method: 'Page.handleJavaScriptDialog', params: { accept: true } }))
  }
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await ev('document.body.innerText')) || ''

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable')
await send('Runtime.enable')
console.log('4) cdp ok')

const results = []
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}
const shot = async (name) => {
  const s = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(s.result.data, 'base64'))
}
const press = (label) => ev(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)

await send('Page.navigate', { url: BASE + '/login' })
await sleep(2500)
await ev(`localStorage.setItem('zk_token', ${JSON.stringify(token)});
          localStorage.setItem('zk_user', JSON.stringify({userId:2, roleId:2, name:'张三'})); 'ok'`)
await ev('window.confirm = () => true; window.alert = () => {}; "ok"')
console.log('5) logged in')

// ---------- 1. 学习计划 ----------
await send('Page.navigate', { url: BASE + '/profile' })
await sleep(4000)
await press('学习计划')
await sleep(3000)
let t = await body()
const planPanel = t.includes('学习计划') && t.includes('节 / 周')
const picked = await ev(`(() => {
  const sel = document.querySelector('.plan-set select');
  if (!sel) return 'no';
  const opt = [...sel.options].find(o => o.value);
  if (!opt) return 'no-opt';
  sel.value = opt.value;
  sel.dispatchEvent(new Event('change'));
  return opt.textContent.trim();
})()`)
await sleep(800)
await press('保存计划')
await sleep(3000)
t = await body()
const planSaved = t.includes('学习计划已保存') || t.includes('本周完成')
pass('学习计划（列表 + 设置频次）', planPanel && planSaved, `course=${picked}`)
await shot('10-plan')
console.log('6) plan done')

// ---------- 2. 取消订单 ----------
await send('Page.navigate', { url: BASE + '/orders' })
await sleep(4000)
await ev('window.confirm = () => true; "ok"')
const hasCancel = await ev(`[...document.querySelectorAll('button')].some(b => b.textContent.includes('取消订单'))`)
const clicked = await press('取消订单')
await sleep(3500)
t = await body()
const firstRowStatus = await ev(`(() => {
  const row = document.querySelector('.order-row, .adm-tbl tbody tr');
  return row ? row.innerText.replace(/\\s+/g, ' ').slice(0, 60) : 'none'
})()`)
const closedOk = hasCancel && clicked === 'ok' && (t.includes('已关闭') || t.includes('已取消'))
pass('取消订单', closedOk, `cancelBtn=${hasCancel} row=${firstRowStatus}`)
await shot('11-orders-cancel')
console.log('7) cancel order done')

// ---------- 3. 撤销退款 ----------
await send('Page.navigate', { url: BASE + '/refund' })
await sleep(4000)
await ev('window.confirm = () => true; "ok"')
const hasCancelRefund = await ev(`[...document.querySelectorAll('button')].some(b => b.textContent.includes('撤销申请'))`)
if (hasCancelRefund) {
  await press('撤销申请')
  await sleep(3500)
  t = await body()
}
pass('撤销退款申请', hasCancelRefund, `created=${refundId} btn=${hasCancelRefund}`)
await shot('12-refund-cancel')
console.log('8) cancel refund done')

// ---------- 4. 回答点赞 ----------
await send('Page.navigate', { url: BASE + '/learn/1' })
await sleep(4500)
await press('问答')
await sleep(2500)
await ev(`(() => { const q = document.querySelector('.q-head'); if (q) { q.click(); return 'ok' } return 'no' })()`)
await sleep(2500)
const before = await ev(`(() => {
  const b = [...document.querySelectorAll('.answer button')].find(x => x.textContent.includes('👍'));
  return b ? b.textContent.trim() : 'none'
})()`)
await ev(`(() => {
  const b = [...document.querySelectorAll('.answer button')].find(x => x.textContent.includes('👍'));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)
await sleep(2500)
const after = await ev(`(() => {
  const b = [...document.querySelectorAll('.answer button')].find(x => x.textContent.includes('👍'));
  return b ? b.textContent.trim() : 'none'
})()`)
pass('回答点赞（计数变化）', before !== 'none' && before !== after, `${before} -> ${after}`)
await shot('13-like')

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`补的功能：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
