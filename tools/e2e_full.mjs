// 全站前后端联调：逐页访问 + 断言 + 截图 + 收集 JS 报错
// 覆盖：登录 → 首页 → 搜索 → 课程详情 → 下单支付 → 学习 → 考试 → 错题本 → 证书 → 订单 → 消息 → 个人中心
import { mkdirSync, writeFileSync } from 'node:fs'

const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const OUT = 'D:\\Java\\tianji\\docs\\mockup\\e2e'
const COURSE_FOR_BUY = '1549025085494521857'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

mkdirSync(OUT, { recursive: true })

async function main() {
  const target = await (await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const jsErrors = []

  ws.addEventListener('message', (ev) => {
    const m = JSON.parse(ev.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return }
    if (m.method === 'Runtime.exceptionThrown') {
      jsErrors.push('exception: ' + (m.params.exceptionDetails?.exception?.description || '').slice(0, 160))
    }
    if (m.method === 'Runtime.consoleAPICalled' && m.params.type === 'error') {
      jsErrors.push('console: ' + (m.params.args || []).map((a) => a.value || a.description || '').join(' ').slice(0, 160))
    }
  })

  const send = (method, params = {}) =>
    new Promise((resolve) => { const mid = ++id; pending.set(mid, resolve); ws.send(JSON.stringify({ id: mid, method, params })) })
  const evaluate = async (expr) => {
    const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })
    return r.result?.result?.value
  }

  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await send('Runtime.enable')

  // ---- 登录 ----
  await send('Page.navigate', { url: BASE + '/login' })
  await sleep(2500)
  const login = await evaluate(`
    fetch('/api/as/accounts/login', {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 1, username: 'jack', password: '123456', rememberMe: true })
    }).then(r => r.json()).then(b => {
      if (!b.data) return 'FAIL';
      localStorage.setItem('zk_token', b.data);
      localStorage.setItem('zk_user', JSON.stringify({ userId: 2, roleId: 2, name: '张三' }));
      return 'OK';
    }).catch(e => 'ERR ' + e)
  `)
  console.log('登录:', login)

  const results = []

  async function visit(name, path, markers, wait = 3500) {
    jsErrors.length = 0
    await send('Page.navigate', { url: BASE + path })
    await sleep(wait)
    const text = (await evaluate('document.body.innerText')) || ''
    const missing = markers.filter((m) => !text.includes(m))
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    const file = `${OUT}\\${name}.png`
    writeFileSync(file, Buffer.from(shot.result.data, 'base64'))
    const pass = missing.length === 0
    results.push({ name, path, pass, missing, jsErrors: [...jsErrors] })
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${name.padEnd(10)} ${path}${missing.length ? '  缺: ' + missing.join(' / ') : ''}`)
    return text
  }

  await visit('01-home', '/', ['精品课包', '全部课程分类'])
  await visit('02-search', '/search?keyword=会计', ['课程搜索', '共'])
  await visit('03-course', '/course/1', ['课程详情', '课程目录'])
  await visit('04-learn', '/learn/1', ['课包目录', '演示倍速'])
  await visit('05-exam', '/exam/1/3', ['交卷'])
  await visit('06-wrong', '/wrong', ['我的错题本'])
  await visit('07-cert', '/cert', ['证书中心', '我的证书'])
  await visit('08-inbox', '/inbox', ['消息中心'])
  await visit('09-orders', '/orders', ['我的订单'])
  await visit('10-profile', '/profile', ['个人中心', '我的课程', '我的考试'])
  await visit('13-refund', '/refund', ['退款', '我的退款申请'])

  // ---- 下单 → 支付 → 成功 ----
  jsErrors.length = 0
  await send('Page.navigate', { url: `${BASE}/order?courseIds=${COURSE_FOR_BUY}` })
  await sleep(4000)
  let text = (await evaluate('document.body.innerText')) || ''
  const confirmOk = text.includes('确认订单') && text.includes('课程清单')
  const couponOk = text.includes('优惠券')
  await evaluate(`(() => {
    const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('提交订单'));
    if (b) { b.click(); return 'clicked' } return 'notfound'
  })()`)
  await sleep(3500)
  text = (await evaluate('document.body.innerText')) || ''
  const payStepOk = text.includes('选择支付方式')
  await evaluate(`(() => {
    const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('确认支付'));
    if (b) { b.click(); return 'clicked' } return 'notfound'
  })()`)
  await sleep(6000)
  text = (await evaluate('document.body.innerText')) || ''
  const paid = text.includes('支付成功')
  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${OUT}\\11-order-paid.png`, Buffer.from(shot.result.data, 'base64'))
  const orderPass = confirmOk && couponOk && payStepOk && paid
  results.push({
    name: '14-order', path: '/order',
    pass: orderPass,
    missing: [confirmOk ? '' : '确认页', couponOk ? '' : '优惠券区', payStepOk ? '' : '支付方式', paid ? '' : '支付成功'].filter(Boolean),
    jsErrors: [...jsErrors]
  })
  console.log(`${orderPass ? 'PASS' : 'FAIL'}  14-order   confirm=${confirmOk} coupon=${couponOk} pay=${payStepOk} success=${paid}`)

  // ---- 支付后课表应出现该课程 ----
  await send('Page.navigate', { url: BASE + '/profile' })
  await sleep(4000)
  const profileText = (await evaluate('document.body.innerText')) || ''
  const enrolled = profileText.includes('法考')
  results.push({ name: '15-enrolled', path: '/profile', pass: enrolled, missing: enrolled ? [] : ['课表里没有新买的课'], jsErrors: [] })
  console.log(`${enrolled ? 'PASS' : 'FAIL'}  12-enrolled 新购买课程出现在课表`)

  // ---- 汇总 ----
  console.log('\n===== 汇总 =====')
  const failed = results.filter((r) => !r.pass)
  console.log(`页面/流程：${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length}`)
  const errs = results.flatMap((r) => r.jsErrors.map((e) => `${r.name}: ${e}`))
  console.log(`前端 JS 报错：${errs.length} 条`)
  errs.slice(0, 10).forEach((e) => console.log('  ' + e))
  writeFileSync(`${OUT}\\report.json`, JSON.stringify(results, null, 2), 'utf-8')
  console.log('报告:', `${OUT}\\report.json`)
  ws.close()
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => { console.error('E2E FAIL', e); process.exit(2) })
