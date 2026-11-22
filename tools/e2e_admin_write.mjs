// 管理端写操作 E2E：题库/分类/优惠券/通知任务 通过真实表单新增
// 注意：Vue 的 v-model 对合成 input 事件不敏感，文本字段必须用 CDP 真实键盘输入
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\admin'
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
const pass = (name, ok, extra = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  ' + extra : ''}`)
}
const press = (label) => evalJs(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(label)}));
  if (b) { b.click(); return 'ok' } return 'no'
})()`)
const body = async () => (await evalJs('document.body.innerText')) || ''
/** 聚焦某个元素后用真实键盘输入 */
const type = async (expr, text) => {
  await evalJs(`(${expr}).focus()`)
  await send('Input.insertText', { text })
  await sleep(250)
}

// 提交后先在 1.2s 内抓提示（3 秒自动消失），再看列表是否出现
async function afterSubmit(toastMarker, listMarker) {
  await sleep(1200)
  const t1 = await body()
  const toast = t1.includes(toastMarker)
  await sleep(2400)
  const listed = listMarker ? (await body()).includes(listMarker) : false
  return { ok: toast || listed, detail: `toast=${toast} list=${listed}` }
}

// 登录
await send('Page.navigate', { url: BASE + '/admin' })
await sleep(3200)
await press('登录')
await sleep(4500)

// ---------- 1. 新增题目 ----------
await press('题库管理')
await sleep(3000)
await press('新增题目')
await sleep(1800)
await type(`document.querySelector('.adm-dialog textarea')`, 'E2E题目：会计要素包括哪些（ ）')
await type(`[...document.querySelectorAll('.adm-dialog input')][1]`, 'ABC')
await type(`[...document.querySelectorAll('.adm-dialog input')][2]`, '会计要素为资产、负债、所有者权益、收入、费用、利润')
await evalJs(`(() => {
  const sels = [...document.querySelectorAll('.adm-dialog select')];
  const sel = sels[sels.length - 1];
  sel.selectedIndex = 1;
  sel.dispatchEvent(new Event('change', { bubbles: true }));
  return 'ok';
})()`)
await sleep(400)
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(x => x.textContent.includes('保存'))?.click()`)
let r = await afterSubmit('题目已新增', 'E2E题目')
pass('新增题目', r.ok, r.detail)

// ---------- 2. 新增分类 ----------
await press('分类管理')
await sleep(3000)
await press('新增分类')
await sleep(1200)
await type(`document.querySelector('.adm-dialog input.adm-input')`, 'E2E自测分类')
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(x => x.textContent.includes('保存'))?.click()`)
r = await afterSubmit('分类已新增', 'E2E自测分类')
pass('新增分类', r.ok, r.detail)

// ---------- 3. 新建优惠券 ----------
await press('优惠券')
await sleep(3000)
await press('新建优惠券')
await sleep(1200)
await type(`document.querySelector('.adm-dialog input.adm-input')`, 'E2E自测优惠券')
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(x => x.textContent.includes('保存'))?.click()`)
r = await afterSubmit('优惠券已创建', 'E2E自测优惠券')
pass('新建优惠券', r.ok, r.detail)

// ---------- 4. 新建通知任务 ----------
await press('通知任务')
await sleep(3000)
await press('新建通知任务')
await sleep(2200)
const tplOk = await evalJs(`(() => {
  const sels = [...document.querySelectorAll('.adm-dialog select')];
  if (!sels.length || sels[0].options.length < 2) return 'no-template';
  sels[0].selectedIndex = 1;
  sels[0].dispatchEvent(new Event('change', { bubbles: true }));
  return 'ok';
})()`)
await type(`document.querySelector('.adm-dialog input[placeholder*="优惠提醒"]')`, 'E2E自测通知')
await sleep(200)
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(x => x.textContent.includes('保存'))?.click()`)
r = await afterSubmit('通知任务已创建', 'E2E自测通知')
pass('新建通知任务', r.ok, `${r.detail} tpl=${tplOk}`)

// ---------- 5. 编辑/发放/新建课程 入口与弹层 ----------
await press('题库管理')
await sleep(2500)
const editBtn = await evalJs(`[...document.querySelectorAll('.adm-tbl button')].some(b => b.textContent.trim() === '编辑')`)
await evalJs(`[...document.querySelectorAll('.adm-tbl button')].find(b => b.textContent.trim() === '编辑')?.click()`)
await sleep(1800)
const editDlg = await evalJs(`(() => {
  const d = document.querySelector('.adm-dialog');
  if (!d) return 'no';
  return d.textContent.includes('编辑题目') && !!d.querySelector('textarea')?.value;
})()`)
pass('编辑题目弹层（含回填）', !!editBtn && editDlg === true, `btn=${editBtn} dlg=${editDlg}`)
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(b => b.textContent.includes('取消'))?.click()`)

await press('优惠券')
await sleep(2500)
const couponBtns = await evalJs(`JSON.stringify([...document.querySelectorAll('.adm-tbl button')].map(b=>b.textContent.trim()).slice(0,6))`)
const hasIssue = couponBtns.includes('发放') && couponBtns.includes('编辑') && couponBtns.includes('查看兑换码')
pass('优惠券 编辑/发放/暂停 入口', hasIssue, couponBtns)
await press('发放')
await sleep(1500)
const issueDlg = await evalJs(`(() => {
  const d = document.querySelector('.adm-dialog');
  return !!d && (d.textContent.includes('设置领用/使用期限') || d.textContent.includes('发放优惠券'));
})()`)
pass('发放弹窗', issueDlg === true)
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(b => b.textContent.includes('取消'))?.click()`)

await press('分类管理')
await sleep(2500)
const cateBtns = await evalJs(`JSON.stringify([...document.querySelectorAll('.adm-tbl button')].map(b=>b.textContent.trim()).slice(0,4))`)
const hasCateOps = cateBtns.includes('编辑') && (cateBtns.includes('禁用') || cateBtns.includes('启用'))
pass('分类 编辑/启停 入口', hasCateOps, cateBtns)

await press('课程管理')
await sleep(2500)
await press('新建课程')
await sleep(1500)
const courseDlg = await evalJs(`(() => {
  const d = document.querySelector('.adm-dialog');
  if (!d) return 'no';
  return d.textContent.includes('新建课程') && d.querySelectorAll('select').length >= 2;
})()`)
pass('新建课程弹层', courseDlg === true)
await evalJs(`[...document.querySelectorAll('.adm-dialog button')].find(b => b.textContent.includes('取消'))?.click()`)
await sleep(500)

const shot = await send('Page.captureScreenshot', { format: 'png' })
writeFileSync(`${OUT}\\写操作结果.png`, Buffer.from(shot.result.data, 'base64'))

console.log('\n===== 汇总 =====')
const failed = results.filter((x) => !x.ok)
console.log(`写操作：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
