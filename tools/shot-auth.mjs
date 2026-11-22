// 用 Chrome DevTools Protocol 注入登录态后截图（无头模式带 localStorage 的可靠做法）
// 用法：node tools/shot-auth.mjs <token> <url> <out.png>
import { writeFileSync } from 'node:fs'

const [, , token, url, out] = process.argv
const CDP = 'http://127.0.0.1:9333'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function main() {
  // 新建标签页
  const target = await (await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()

  ws.addEventListener('message', (ev) => {
    const msg = JSON.parse(ev.data)
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg)
      pending.delete(msg.id)
    }
  })

  const send = (method, params = {}) =>
    new Promise((resolve) => {
      const mid = ++id
      pending.set(mid, resolve)
      ws.send(JSON.stringify({ id: mid, method, params }))
    })

  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await send('Runtime.enable')

  // 先打开同源页面，才能写 localStorage
  await send('Page.navigate', { url: 'http://127.0.0.1:5173/login' })
  await sleep(2500)

  if (token !== '-') {
    await send('Runtime.evaluate', {
      expression: `localStorage.setItem('zk_token', ${JSON.stringify(token)});
                   localStorage.setItem('zk_user', JSON.stringify({userId: 2, roleId: 2, name: '张三'}));
                   'ok'`
    })
  }

  // 再跳转到目标页
  await send('Page.navigate', { url })
  await sleep(4500)

  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(out, Buffer.from(shot.result.data, 'base64'))
  console.log('saved', out)
  ws.close()
}

main().catch((e) => {
  console.error('FAIL', e)
  process.exit(1)
})
