// 双端登录分离检查：学员站 /login 与 管理后台 /admin/login 是两套入口
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

const target = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
const jsErrors = []
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 160))
  if (m.method === 'Page.javascriptDialogOpening') ws.send(JSON.stringify({ id: ++id, method: 'Page.handleJavaScriptDialog', params: { accept: true } }))
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await ev('document.body.innerText')) || ''
const url = async () => (await ev('location.pathname')) || ''
const shot = async (name) => {
  const s = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(s.result.data, 'base64'))
}
const press = (label) => ev(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)
const type = async (sel, text) => {
  await ev(`document.querySelector(${JSON.stringify(sel)}).focus()`)
  await send('Input.insertText', { text })
  await sleep(200)
}

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable')
await send('Runtime.enable')

const results = []
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}

// 1) 学员站登录页：没有员工入口、有注册/找回密码
await send('Page.navigate', { url: BASE + '/login' })
await sleep(1500)
await ev('localStorage.clear()')
await send('Page.navigate', { url: BASE + '/login' })   // 整页重载，authState 从空存储初始化
await sleep(2500)
let t = await body()
const studentOk = t.includes('去注册') && t.includes('找回密码') && !t.includes('短信登录') && !t.includes('演示账号')
pass('学员站登录页（无短信登录/无演示提示）', studentOk)

// 2) 未登录直接访问 /admin → 自动跳到 /admin/login
await send('Page.navigate', { url: BASE + '/admin' })
await sleep(3500)
const path = await url()
t = await body()
const adminLoginOk = path === '/admin/login' && t.includes('管理后台') && t.includes('返回学员端') && !t.includes('演示员工账号')
pass('未登录访问 /admin 跳管理端登录', adminLoginOk, 'path=' + path)
await shot('19-admin-login')

// 3) 管理端登录页没有「短信登录 / 去注册 / 演示账号」
const hasSms = t.includes('短信登录') || t.includes('去注册') || t.includes('演示账号')
pass('管理端登录页与学员端不同', !hasSms)
// 4) 管理端登录（预填 admin/123456）→ 进入后台
await press('登 录')
await sleep(5000)
const p2 = await url()
let t2 = await body()
const dashboardOk = p2.startsWith('/admin') && t2.includes('数据概览')
pass('管理端登录进入后台', dashboardOk, 'path=' + p2)
await shot('20-admin-after-login')

// 5) 两个登录接口的隔离（服务端保证）：员工账号不能走学员端接口，反之亦然
const GW = 'http://127.0.0.1:10010'
async function loginAt(path, body) {
  const r = await fetch(GW + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  return await r.json()
}
const staffOnStudent = await loginAt('/as/accounts/login', { type: 1, username: 'admin', password: '123456', rememberMe: true })
const studentOnStaff = await loginAt('/as/accounts/admin/login', { type: 1, username: 'jack', password: '123456', rememberMe: true })
const staffOk = await loginAt('/as/accounts/admin/login', { type: 1, username: 'admin', password: '123456', rememberMe: true })
pass('学员端接口拒绝员工账号', staffOnStudent.code !== 200 && String(staffOnStudent.msg).includes('非学生端用户'), `msg=${staffOnStudent.msg}`)
pass('管理端接口拒绝学员账号', studentOnStaff.code !== 200, `msg=${studentOnStaff.msg}`)
pass('管理端接口放行员工账号', staffOk.code === 200 && !!staffOk.data)
console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`双端登录：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)