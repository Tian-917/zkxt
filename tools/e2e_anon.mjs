// 匿名浏览检查：不登录也能看首页/搜索/课程详情，但买课要登录
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

const t = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json()
const ws = new WebSocket(t.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
const jsErrors = []
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 160))
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value
const body = async () => (await ev('document.body.innerText')) || ''
const path = async () => (await ev('location.pathname + location.search')) || ''
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

// 清空登录态
await send('Page.navigate', { url: BASE + '/login' })
await sleep(1500)
await ev('localStorage.clear()')
await send('Page.navigate', { url: 'about:blank' })   // 强制换文档，保证按访客身份重载
await sleep(600)
await send('Page.navigate', { url: BASE + '/' })
await sleep(4000)

// 1) 首页可匿名浏览
let b = await body()
const p1 = await path()
pass('未登录可看首页', p1 === '/' && b.includes('精品课包') && b.includes('全部课程分类'), `path=${p1}`)
pass('未登录导航显示登录/注册', b.includes('登录') && b.includes('注册'))
await shot('24-anon-home')

// 2) 搜索可匿名
await send('Page.navigate', { url: BASE + '/search?keyword=会计' })
await sleep(3500)
b = await body()
const p2 = await path()
pass('未登录可搜索', p2.startsWith('/search') && b.includes('课程搜索'), `path=${p2}`)

// 3) 课程详情可匿名（首页「查看详情」即跳这里）
await send('Page.navigate', { url: BASE + '/' })
await sleep(3500)
const cardLink = await ev(`(() => {
  const c = [...document.querySelectorAll('.card')].find(x => x.textContent.includes('查看详情'));
  if (c) { c.click(); return 'ok' } return 'no'
})()`)
await sleep(3000)
b = await body()
const p3 = await path()
const detailOk = cardLink === 'ok' && p3.startsWith('/course') && b.includes('课程详情')
pass('首页卡片跳课程详情（非登录页）', detailOk, `path=${p3}`)
await shot('25-anon-course')

// 4) 点购买 → 弹「需要登录」，不直接跳
await press('立即购买')
await sleep(1500)
b = await body()
const promptShown = b.includes('登录后才能继续') && b.includes('去登录') && b.includes('暂时不登录')
pass('点购买弹登录提示', promptShown)
await shot('26-login-prompt')

// 5) 暂时不登录 → 留在当前页
await press('暂时不登录')
await sleep(1200)
const p5 = await path()
b = await body()
pass('暂时不登录留在原页', p5.startsWith('/course') && !b.includes('登录后才能继续'), `path=${p5}`)

// 6) 再次触发 → 去登录，带 redirect
await press('立即购买')
await sleep(1200)
await press('去登录')
await sleep(2500)
const p6 = await path()
pass('去登录带 redirect', p6.startsWith('/login') && p6.includes('redirect'), `path=${p6}`)

// 7) 直接访问受保护页 → 回首页并弹窗（不白屏）
await send('Page.navigate', { url: BASE + '/profile' })
await sleep(3000)
const p7 = await path()
b = await body()
pass('直接访问个人中心→首页+弹窗', (p7 === '/' || p7.startsWith('/?')) && b.includes('登录后才能继续'), `path=${p7}`)

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`匿名浏览：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
