// 验证"模拟播放 -> 上报进度 -> 后端判定学完"
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
  const evaluate = async (expr) =>
    (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value

  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await send('Runtime.enable')

  await send('Page.navigate', { url: 'http://127.0.0.1:5173/login' })
  await sleep(2500)
  const login = await evaluate(`
    fetch('/api/as/accounts/login', {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 1, username: 'jack', password: '123456', rememberMe: true })
    }).then(r => r.json()).then(async b => {
      localStorage.setItem('zk_token', b.data);
      localStorage.setItem('zk_user', JSON.stringify({ userId: 2, roleId: 2, name: '张三' }));
      return 'OK';
    }).catch(e => 'ERR:' + e)
  `)
  console.log('登录:', login)

  await send('Page.navigate', { url: 'http://127.0.0.1:5173/learn/4' })
  await sleep(4000)

  const before = await evaluate(`JSON.stringify({
    title: document.querySelector('h3')?.textContent || '',
    percent: document.querySelector('.stage-bar i')?.style.width || '',
    playBtn: !!document.querySelector('.stage-play')
  })`)
  console.log('打开课包:', before)

  await evaluate(`document.querySelector('.stage-play').click(); 'clicked'`)
  console.log('已点击播放（60 倍速）')

  // 35 分钟的课，60 倍速下约 28 秒到 80%
  for (let i = 1; i <= 6; i++) {
    await sleep(6000)
    const state = await evaluate(`JSON.stringify({
      bar: document.querySelector('.stage-bar i')?.style.width || '',
      text: [...document.querySelectorAll('.row span')].map(s => s.textContent).find(t => t && t.includes('已观看')) || '',
      badge: document.querySelector('.badge')?.textContent || ''
    })`)
    console.log(`  +${i * 6}s ->`, state)
  }

  if (out) {
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    writeFileSync(out, Buffer.from(shot.result.data, 'base64'))
    console.log('saved', out)
  }
  ws.close()
}

main().catch((e) => { console.error('FAIL', e); process.exit(1) })
