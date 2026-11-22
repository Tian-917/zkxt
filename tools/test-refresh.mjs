// 验证 token 自动续期：
// 1) 真实登录（拿到 HttpOnly refresh cookie）
// 2) 把 localStorage 里的 access token 改成垃圾值
// 3) 打开首页 -> 请求 401 -> 自动 refresh -> 重放 -> 页面正常
import { writeFileSync } from 'node:fs'

const [, , out] = process.argv
const CDP = 'http://127.0.0.1:9333'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function main() {
  const target = await (await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  ws.addEventListener('message', (ev) => {
    const m = JSON.parse(ev.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) }
  })
  const send = (method, params = {}) =>
    new Promise((resolve) => { const mid = ++id; pending.set(mid, resolve); ws.send(JSON.stringify({ id: mid, method, params })) })
  const evaluate = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value

  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await send('Runtime.enable')

  // 1) 打开登录页（保证同源），用页面上下文真实登录
  await send('Page.navigate', { url: 'http://127.0.0.1:5173/login' })
  await sleep(2500)

  const login = await evaluate(`
    fetch('/api/as/accounts/login', {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 1, username: 'jack', password: '123456', rememberMe: true })
    }).then(r => r.json()).then(b => (b && b.data ? 'OK' : JSON.stringify(b))).catch(e => 'ERR:' + e)
  `)
  console.log('1) 登录:', login ? String(login).slice(0, 12) : login)

  const cookie = await evaluate(`fetch('/api/us/users/2', { credentials: 'include' }).then(r => r.status)`)
  console.log('   cookie 可用性（带 token 前应 401）:', cookie)

  // 2) 写一个假的 access token，保留真实 refresh cookie
  await evaluate(`
    localStorage.setItem('zk_token', 'garbage.invalid.token');
    localStorage.setItem('zk_user', JSON.stringify({ userId: 2, roleId: 2, name: '张三' }));
    'done'
  `)

  // 3) 打开首页：首屏请求必然 401 -> 触发刷新 -> 重放
  await send('Page.navigate', { url: 'http://127.0.0.1:5173/' })
  await sleep(6000)

  const result = await evaluate(`
    (() => {
      const cards = document.querySelectorAll('.card').length;
      const token = localStorage.getItem('zk_token') || '';
      return JSON.stringify({ cards, refreshed: token !== 'garbage.invalid.token', tokenLen: token.length, url: location.pathname });
    })()
  `)
  console.log('2) 首页结果:', result)

  if (out) {
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    writeFileSync(out, Buffer.from(shot.result.data, 'base64'))
    console.log('saved', out)
  }
  ws.close()
}

main().catch((e) => { console.error('FAIL', e); process.exit(1) })
