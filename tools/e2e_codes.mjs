// 兑换码 UI：学员端兑换 + 管理端查看兑换码
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const GW = 'http://127.0.0.1:10010'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

async function call(m, p, b, t) {
  const c = new AbortController()
  const timer = setTimeout(() => c.abort(), 10000)
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

const admin = (await call('POST', '/as/accounts/admin/login', { type: 1, username: 'admin', password: '123456', rememberMe: true })).data
const stu = (await call('POST', '/as/accounts/login', { type: 1, username: 'jack', password: '123456', rememberMe: true })).data

// 找一张「发放兑换码」且已发放中的券，取一个未兑换的码
const page = await call('GET', '/prs/coupons/page?pageNo=1&pageSize=50', null, admin)
const list = page.data?.list || []
const val = (x) => (x && typeof x === 'object' ? x.value : x)
const target = list.find((c) => {
  const st = val(c.status)
  const ow = val(c.obtainWay)
  return ow === 2 && st === 3
}) || list.find((c) => val(c.obtainWay) === 2)
let code = null
if (target) {
  const codes = await call('GET', `/prs/codes/page?pageNo=1&pageSize=20&status=1&couponId=${target.id}`, null, admin)
  code = codes.data?.list?.[0]?.code || null
}
console.log('target coupon:', target?.name, 'code:', code)

const t = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json()
const ws = new WebSocket(t.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
const jsErrors = []
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 140))
  if (m.method === 'Page.javascriptDialogOpening') ws.send(JSON.stringify({ id: ++id, method: 'Page.handleJavaScriptDialog', params: { accept: true } }))
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await ev('document.body.innerText')) || ''
const shot = async (name) => {
  const s = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(s.result.data, 'base64'))
}
const press = (label) => ev(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable')
await send('Runtime.enable')

const results = []
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}

// ---------- 1. 学员端兑换 ----------
await send('Page.navigate', { url: BASE + '/login' })
await sleep(2500)
await ev(`localStorage.setItem('zk_token', ${JSON.stringify(stu)});
          localStorage.setItem('zk_user', JSON.stringify({userId:2, roleId:2, name:'张三'})); 'ok'`)
await send('Page.navigate', { url: BASE + '/profile' })
await sleep(4000)
await press('我的优惠券')
await sleep(3000)
await ev(`window.confirm = () => true; "ok"`)
const hasBar = await ev(`!!document.querySelector('.exchange-bar input')`)
if (hasBar && code) {
  await ev(`document.querySelector('.exchange-bar input').focus()`)
  await send('Input.insertText', { text: code })
  await sleep(300)
  await press('立即兑换')
  await sleep(3500)
}
const t1 = await body()
const exchanged = t1.includes('兑换成功')
pass('学员端兑换码兑换', hasBar && exchanged, `code=${code}`)
await shot('14-exchange-code')

// ---------- 2. 管理端查看兑换码 ----------
await send('Page.navigate', { url: BASE + '/login' })
await sleep(2000)
await ev(`localStorage.setItem('zk_token', ${JSON.stringify(admin)});
          localStorage.setItem('zk_user', JSON.stringify({userId:1, roleId:1, name:'admin'})); 'ok'`)
await send('Page.navigate', { url: BASE + '/admin' })
await sleep(4000)
await press('优惠券')
await sleep(3000)
await press('兑换码')
await sleep(2500)
const t2 = await body()
const dialogOk = t2.includes('兑换码 ·') || t2.includes('复制全部')
const codeShown = await ev(`(() => {
  const cells = [...document.querySelectorAll('.adm-dialog .adm-mono')];
  return cells.length ? cells[0].textContent.trim() : 'none'
})()`)
pass('管理端兑换码列表', dialogOk, `first=${codeShown}`)
await shot('15-admin-codes')

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`兑换码 UI：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
