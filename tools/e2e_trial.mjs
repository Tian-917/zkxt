// 分类悬停展开 + 课程试看前两节（游客视角）
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e2'
const COURSE = '7'
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

// 游客态
await send('Page.navigate', { url: BASE + '/login' })
await sleep(1500)
await ev('localStorage.clear()')
await send('Page.navigate', { url: 'about:blank' })
await sleep(500)
await send('Page.navigate', { url: BASE + '/' })
await sleep(4500)

// 1) 分类 hover 展开二级
// 逐个 hover，直到某个一级分类真的有子分类（忽略测试留下的空分类）
const hovered = await ev(`(async () => {
  const list = [...document.querySelectorAll('.cate-wrap')];
  for (const w of list) {
    w.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
    await new Promise(r => setTimeout(r, 250));
    if (document.querySelector('.cate-panel .panel-item')) return 'ok';
  }
  return 'none-with-children';
})()`)
await sleep(800)
const panelItems = await ev(`document.querySelectorAll('.cate-panel .panel-item').length`)
pass('分类悬停展开二级', hovered === 'ok' && panelItems > 0, `items=${panelItems}`)
await shot('28-cate-hover')

// 2) 点二级分类 → 搜索页带 categoryIdLv2
const clicked = await ev(`(() => {
  const a = document.querySelector('.cate-panel .panel-item');
  if (!a) return 'no';
  a.click(); return a.textContent.trim();
})()`)
await sleep(3000)
const p2 = await path()
pass('点二级分类跳搜索', p2.includes('categoryIdLv2'), `path=${p2} cate=${clicked}`)

// 3) 课程详情：前两节试看 + 其余上锁
await send('Page.navigate', { url: BASE + '/course/' + COURSE })
await sleep(3500)
const trialTags = await ev(`document.querySelectorAll('.trial-tag').length`)
const locks = await ev(`document.querySelectorAll('.sec.locked').length`)
pass('目录前两节标「试看」', trialTags === 2 && locks > 0, `trial=${trialTags} locked=${locks}`)
await shot('29-course-trial')

// 4) 点试看小节 → 进学习页（游客可进）
const opened = await ev(`(() => {
  const sec = [...document.querySelectorAll('.sec')].find(s => s.querySelector('.trial-tag'));
  if (!sec) return 'no';
  sec.click(); return 'ok';
})()`)
await sleep(3500)
const p4 = await path()
let b = await body()
const learnOk = opened === 'ok' && p4.includes('/learn/' + COURSE) && b.includes('试看中')
pass('游客可进学习页试看', learnOk, `path=${p4}`)
await shot('30-learn-trial')

// 5) 学习页里也有「试看」标记 + 锁
const t5 = await ev(`document.querySelectorAll('.lesson .trial-tag').length`)
const l5 = await ev(`document.querySelectorAll('.lesson.locked').length`)
pass('学习页试看/锁定标记', t5 === 2 && l5 > 0, `trial=${t5} locked=${l5}`)

// 6) 点锁定小节 → 弹登录提示
const lockedClicked = await ev(`(() => {
  const s = document.querySelector('.lesson.locked');
  if (!s) return 'no';
  s.click(); return 'ok';
})()`)
await sleep(1200)
b = await body()
pass('点锁定小节弹登录提示', lockedClicked === 'ok' && b.includes('登录后才能继续'))

console.log('\n===== 汇总 =====')
const failed = results.filter((r) => !r.ok)
console.log(`试看/分类：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
console.log(`前端 JS 报错：${jsErrors.length}`)
jsErrors.slice(0, 5).forEach((e) => console.log('  ' + e))
ws.close()
process.exit(failed.length ? 1 : 0)
