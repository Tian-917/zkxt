// 注册 / 找回密码 全链路 E2E（短信登录入口已移除）
// 验证码来源：user-service 的 debug 日志（本机没有短信网关，sendVerifyCode 会打印 code）
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const LOG = 'D:\\Java\\zkxt\\logs\\user.log'
const OUT = 'D:\\Java\\zkxt\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

const phone = '139' + String(Date.now()).slice(-8)
const password = 'zk123456'
const newPassword = 'zk654321'

const target = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json()
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
const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await ev('document.body.innerText')) || ''
const url = async () => (await ev('location.href')) || ''

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable'); await send('Runtime.enable')

const results = []
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}
const shot = async (name) => {
  const s = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(s.result.data, 'base64'))
}
const type = async (selector, text) => {
  // 先 select()：输入框可能被预填（注册后跳转会带手机号），不选全就是追加
  await ev(`(() => { const el = document.querySelector(${JSON.stringify(selector)}); el.focus(); el.select && el.select(); return 'ok' })()`)
  await send('Input.insertText', { text })
  await sleep(200)
}
const clickText = (label, sel = 'button') =>
  ev(`(() => {
    const b = [...document.querySelectorAll(${JSON.stringify(sel)})].find(x => x.textContent.includes(${JSON.stringify(label)}));
    if (b) { b.click(); return 'ok' } return 'no'
  })()`)

// 提示文案只显示 ~900ms 就跳转，用轮询抓
async function waitForText(text, timeout = 6000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const b = await body()
    if (b.includes(text)) return true
    await sleep(200)
  }
  return false
}

function lastCode() {
  const txt = readFileSync(LOG, 'utf8')
  const m = [...txt.matchAll(/发送短信验证码：(\d+)/g)]
  return m.length ? m[m.length - 1][1] : ''
}

// ---------- 1. 注册 ----------
await send('Page.navigate', { url: BASE + '/login' })
await sleep(2000)
await ev('localStorage.clear()')          // 清掉上一次用例留下的 token
await send('Page.navigate', { url: BASE + '/register' })
await sleep(3000)
await type('input[placeholder="11 位手机号"]', phone)
await clickText('获取验证码')
await sleep(2500)
const code1 = lastCode()
await type('input[placeholder="6 位数字"]', code1)
await type('input[placeholder="设置登录密码"]', password)
await type('input[placeholder="再输一次"]', password)
await clickText('注 册')
const regOk = await waitForText('注册成功')
let t = await body()
pass('注册（验证码真实校验）', regOk, `phone=${phone} code=${code1}`)
await sleep(800)
await shot('07-register')

// ---------- 3. 找回密码 ----------
await ev(`localStorage.clear()`)
await send('Page.navigate', { url: BASE + '/forgot' })
await sleep(3000)
await type('input[placeholder="注册时用的手机号"]', phone)
await clickText('获取验证码')
await sleep(2500)
const code3 = lastCode()
await type('input[placeholder="6 位数字"]', code3)
await type('input[placeholder="输入新密码"]', newPassword)
await type('input[placeholder="再输一次"]', newPassword)
await clickText('重置密码')
const resetOk = await waitForText('密码已重置')
t = await body()
pass('找回密码（验证码重置）', resetOk, `code=${code3}`)
await sleep(1500)
await shot('09-forgot')

// ---------- 4. 用新密码登录（从重置成功页的「去登录」进入，带手机号） ----------
await waitForText('去登录')
await clickText('去登录')
// 等真正落到登录页（标题出现「账号（用户名 / 手机号）」）
for (let i = 0; i < 20; i++) {
  const p = await url()
  if (p.includes('/login')) break
  await sleep(300)
}
await sleep(800)
await type('input[placeholder="请输入账号"]', phone)
await type('input[placeholder="请输入密码"]', newPassword)
await sleep(400)
await clickText('登 录')
// 轮询最多 10 秒等登录态落地
let logged4 = false
let u4 = ''
for (let i = 0; i < 20; i++) {
  await sleep(500)
  u4 = await url()
  logged4 = !u4.includes('/login') && (await ev(`!!localStorage.getItem('zk_token')`))
  if (logged4) break
}
pass('新密码登录成功', !!logged4, `url=${u4.replace(BASE, '')}`)
await shot('21-after-reset-login')
console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`登录/注册：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
