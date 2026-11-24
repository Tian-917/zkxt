// 管理端优惠券页 UI 检查（对齐原版营销中心）
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const GW = 'http://127.0.0.1:10010'
const OUT = 'D:\\Java\\zkxt\\docs\\mockup\\e2e2'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
mkdirSync(OUT, { recursive: true })

const admin = (await (await fetch(GW + '/as/accounts/admin/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ type: 1, username: 'admin', password: '123456', rememberMe: true })
})).json()).data

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

await send('Page.navigate', { url: BASE + '/login' })
await sleep(2000)
await ev(`localStorage.setItem('zk_token', ${JSON.stringify(admin)});
          localStorage.setItem('zk_user', JSON.stringify({userId:1, roleId:1, name:'admin'})); 'ok'`)
await send('Page.navigate', { url: BASE + '/admin' })
await sleep(4000)
await press('优惠券')
await sleep(3500)

// 1) 表头与中文枚举
let t = await body()
const headers = ['序号', '优惠券名称', '优惠券规则', '使用范围', '推广方式', '状态', '创建时间', '操作']
const headerOk = headers.every((h) => t.includes(h))
pass('表头对齐原版', headerOk, headerOk ? '' : '缺: ' + headers.filter((h) => !t.includes(h)).join('/'))

const hasChinese = t.includes('手动领取') || t.includes('指定发放')
const rawNumbers = await ev(`(() => {
  const cells = [...document.querySelectorAll('.adm-tbl tbody tr td:nth-child(7)')];
  return cells.map(c => c.innerText.trim()).join(',')
})()`)
const statusOk = hasChinese && !/^(1|2|3|4|5)(,|$)/.test(rawNumbers)
pass('推广方式/状态中文化', statusOk, `status列=[${rawNumbers}]`)

// 2) 规则文案
const ruleSample = await ev(`(() => {
  const c = document.querySelector('.adm-tbl tbody tr td:nth-child(3)');
  return c ? c.innerText.trim() : 'none'
})()`)
pass('规则文案', /(满|每满|无门槛)/.test(ruleSample), ruleSample)
await shot('16-admin-coupons')

// 3) 新增表单（两段式）
await press('新增优惠券')
await sleep(1800)
t = await body()
const formOk = t.includes('优惠券基本信息') && t.includes('优惠券推广方式') && t.includes('发放数量') && t.includes('每人限领')
pass('新增表单两段式', formOk)
await shot('17-coupon-form')
await ev(`[...document.querySelectorAll('.adm-dialog button')].find(b => b.textContent.includes('取消'))?.click()`)
await sleep(600)

// 4) 发放弹窗（领用/使用期限）
const issueOpened = await press('发放')
await sleep(1500)
t = await body()
const issueOk = t.includes('设置领用/使用期限') && t.includes('发放方式') && t.includes('领用期限') && t.includes('使用期限方式')
pass('发放弹窗', issueOpened === 'ok' && issueOk)
await shot('18-coupon-issue')
await ev(`[...document.querySelectorAll('.adm-dialog button')].find(b => b.textContent.includes('取消'))?.click()`)
await sleep(600)

// 5) 查看兑换码（指定发放的券才有）
const codesOpened = await ev(`(() => {
  const rows = [...document.querySelectorAll('.adm-tbl tbody tr')];
  for (const r of rows) {
    if (r.innerText.includes('指定发放')) {
      const b = [...r.querySelectorAll('button')].find(x => x.textContent.includes('查看兑换码'));
      if (b) { b.click(); return 'ok' }
    }
  }
  return 'no'
})()`)
await sleep(2000)
t = await body()
pass('查看兑换码', codesOpened === 'ok' ? t.includes('兑换码 ·') : true, codesOpened)

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`优惠券管理 UI：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
