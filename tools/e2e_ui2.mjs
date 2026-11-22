// 新版 UI 检查：小节内笔记/问答、个人中心侧边栏、购物车、下单页优惠券下拉
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e2'
const COURSE_FOR_BUY = '1549025085494521857'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

const target = await (await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
const jsErrors = []
ws.addEventListener('message', (ev) => {
  const m = JSON.parse(ev.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 140))
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const evalJs = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await evalJs('document.body.innerText')) || ''

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable'); await send('Runtime.enable')

const results = []
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}
const press = (label) => evalJs(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)
const shot = async (name) => {
  const s = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(s.result.data, 'base64'))
}

// 登录
await send('Page.navigate', { url: BASE + '/login' })
await sleep(2500)
await evalJs(`fetch('/api/as/accounts/login', {method:'POST', credentials:'include',
  headers:{'Content-Type':'application/json'},
  body: JSON.stringify({type:1, username:'jack', password:'123456', rememberMe:true})})
  .then(r=>r.json()).then(b=>{ localStorage.setItem('zk_token', b.data);
    localStorage.setItem('zk_user', JSON.stringify({userId:2, roleId:2, name:'张三'})); return 'ok'})`)

// 1) 小节内的笔记 / 问答
await send('Page.navigate', { url: BASE + '/learn/1' })
await sleep(4500)
let t = await body()
const notesTab = t.includes('课堂笔记') && t.includes('发布笔记') && t.includes('问答')
pass('学习页-笔记 tab', notesTab)
await press('问答')
await sleep(2500)
t = await body()
const qaTab = t.includes('本节问答') && t.includes('我要提问')
pass('学习页-问答 tab', qaTab)
await shot('01-learn-qa')

// 1.1) 在小节内发布笔记（先切回「笔记」页签）
await press('笔记')
await sleep(1800)
const type = async (expr, text) => {
  await evalJs(`(${expr}).focus()`)
  await send('Input.insertText', { text })
  await sleep(250)
}
await type(`document.querySelector('.lesson-panel textarea')`, 'E2E小节笔记：这一节的要点')
await evalJs(`[...document.querySelectorAll('.lesson-panel button')].find(b => b.textContent.includes('发布笔记'))?.click()`)
await sleep(2500)
t = await body()
const noteOk = t.includes('E2E小节笔记') || t.includes('笔记已发布')
pass('小节内发布笔记', noteOk)

// 1.2) 在小节内提问 → 回答 → 回复某人
await press('问答')
await sleep(2500)
await press('我要提问')
await sleep(800)
await type(`document.querySelectorAll('.lesson-panel input')[0]`, 'E2E小节提问：这节的判定规则？')
await evalJs(`[...document.querySelectorAll('.lesson-panel button')].find(b => b.textContent.includes('提交问题'))?.click()`)
await sleep(2500)
t = await body()
const asked = t.includes('E2E小节提问')
pass('小节内提问', asked)
if (asked) {
  // 展开问题
  await evalJs(`(() => {
    const el = [...document.querySelectorAll('.lesson-panel .q-head')].find(x => x.textContent.includes('E2E小节提问'));
    if (el) { el.click(); return 'ok' } return 'no'
  })()`)
  await sleep(2000)
  // 回答
  await type(`document.querySelectorAll('.lesson-panel textarea')[0]`, '看到 80% 就算学完')
  await evalJs(`[...document.querySelectorAll('.lesson-panel button')].find(b => b.textContent.includes('提交回答'))?.click()`)
  await sleep(2500)
  t = await body()
  const answered = t.includes('看到 80% 就算学完')
  // 回复该回答
  const replyClicked = await evalJs(`(() => {
    const b = [...document.querySelectorAll('.lesson-panel button')].find(x => x.textContent.trim() === '回复');
    if (b) { b.click(); return 'ok' } return 'no'
  })()`)
  await sleep(800)
  await type(`document.querySelectorAll('.lesson-panel textarea')[0]`, '补充：1.25 * moment >= duration')
  await evalJs(`[...document.querySelectorAll('.lesson-panel button')].find(b => b.textContent.includes('提交回复'))?.click()`)
  await sleep(2500)
  t = await body()
  const replied = t.includes('补充：1.25') && t.includes('回复')
  pass('回答 + 回复某人', answered && replied, `answer=${answered} reply=${replyClicked}`)
  await shot('01b-learn-reply')
}
// 2) 个人中心侧边栏
await send('Page.navigate', { url: BASE + '/profile' })
await sleep(4000)
t = await body()
const menus = ['我的课程', '我的考试', '我的订单', '我的优惠券', '我的积分', '个人设置', '退出']
const profileOk = menus.every((m) => t.includes(m))
pass('个人中心侧边栏 7 项', profileOk, profileOk ? '' : '缺: ' + menus.filter((m) => !t.includes(m)).join('/'))
await shot('02-profile-lessons')

// 3) 我的优惠券
await press('我的优惠券')
await sleep(3500)
t = await body()
const couponOk = t.includes('领券中心') && (t.includes('未使用') || t.includes('立即领取'))
pass('我的优惠券 + 领券中心', couponOk)
await shot('03-profile-coupons')

// 4) 我的积分（整月签到日历）
await press('我的积分')
await sleep(3500)
t = await body()
const calOk = t.includes('年') && t.includes('本月已签到') && t.includes('日')
pass('积分页-整月签到日历', calOk)
await shot('04-profile-points')

// 5) 购物车
await send('Page.navigate', { url: BASE + '/cart' })
await sleep(3500)
t = await body()
const cartOk = t.includes('我的购物车')
pass('购物车页', cartOk)
await shot('05-cart')

// 6) 下单页优惠券下拉
await send('Page.navigate', { url: `${BASE}/order?courseIds=${COURSE_FOR_BUY}` })
await sleep(4500)
t = await body()
const orderOk = t.includes('优惠券') && t.includes('订单总额') && t.includes('应付金额')
const hasSelect = await evalJs(`!!document.querySelector('.cl-select')`)
pass('下单页-优惠券下拉+金额区', orderOk && hasSelect, `select=${hasSelect}`)
await shot('06-order')

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`UI：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 6).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
