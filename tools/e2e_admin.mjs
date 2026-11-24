// 管理后台 E2E：员工登录 → 10 个模块逐个打开 → 课程上下架切换
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\zkxt\\docs\\mockup\\admin'
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
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push((m.params.exceptionDetails?.exception?.description || '').slice(0, 120))
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const evalJs = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable'); await send('Runtime.enable')

const results = []
const press = async (label) => evalJs(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)

// 1) 门禁（先清掉可能残留的登录态，否则会直接进后台）
await send('Page.navigate', { url: BASE + '/login' })
await sleep(1500)
await evalJs('localStorage.clear()')
await send('Page.navigate', { url: BASE + '/admin' })
await sleep(3000)
let text = (await evalJs('document.body.innerText')) || ''
const gateOk = text.includes('管理后台') && text.includes('登录')
results.push({ name: '门禁页', pass: gateOk })
console.log(`${gateOk ? 'PASS' : 'FAIL'}  门禁页`)

// 2) 登录（独立管理端登录页，按钮文案「登 录」）
await press('登 录')
await sleep(5000)
text = (await evalJs('document.body.innerText')) || ''
const loginOk = text.includes('数据概览') && text.includes('今日访问量')
results.push({ name: '登录+概览', pass: loginOk })
console.log(`${loginOk ? 'PASS' : 'FAIL'}  登录 + 数据概览`)

const MODULES = [
  ['课程管理', ['共', '全部状态']],
  ['题库管理', ['道题', '难度']],
  ['分类管理', ['个分类', '层级']],
  ['讲师管理', ['课程数', '出题数']],
  ['内容审核', ['笔记', '提问']],
  ['优惠券', ['优惠券', '门槛']],
  ['退款审批', ['退款单', '状态']],
  ['学员管理', ['手机号', '注册时间']],
  ['通知任务', ['通知类型', '任务']]
]

for (const [label, markers] of MODULES) {
  await press(label)
  await sleep(3200)
  const t = (await evalJs('document.body.innerText')) || ''
  const ok = markers.every((m) => t.includes(m))
  results.push({ name: label, pass: ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${label}${ok ? '' : '  缺: ' + markers.filter((m) => !t.includes(m)).join('/')}`)
  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\${label}.png`, Buffer.from(shot.result.data, 'base64'))
}

// 3) 课程上下架切换（切换后复原）
await press('课程管理')
await sleep(3000)
const before = await evalJs(`document.querySelector('.adm-tbl tbody tr:first-child .adm-badge')?.textContent || ''`)
const clicked = await evalJs(`(() => { const b=document.querySelector('.adm-tbl tbody tr:first-child button'); if(b){const t=b.textContent.trim(); b.click(); return t} return 'no' })()`)
await sleep(3200)
const after = await evalJs(`document.querySelector('.adm-tbl tbody tr:first-child .adm-badge')?.textContent || ''`)
const shelfOk = before !== after && clicked !== 'no'
console.log(`${shelfOk ? 'PASS' : 'FAIL'}  课程上下架  点击[${clicked}] ${before} -> ${after}`)
results.push({ name: '课程上下架', pass: shelfOk })
await evalJs(`(() => { const b=document.querySelector('.adm-tbl tbody tr:first-child button'); if(b){b.click(); return 'ok'} return 'no' })()`)
await sleep(2500)

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.pass)
console.log(`管理端：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
