// 定向验证下单支付：每一步打印页面文本，定位卡在哪
const CDP = 'http://127.0.0.1:9333'
const BASE = 'http://127.0.0.1:5173'
const COURSE = process.argv[2] || '1549025085494521857'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const target = await (await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
let id = 0
const pending = new Map()
ws.addEventListener('message', (ev) => {
  const m = JSON.parse(ev.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) }
})
const send = (method, params = {}) =>
  new Promise((r) => { const mid = ++id; pending.set(mid, r); ws.send(JSON.stringify({ id: mid, method, params })) })
const evalJs = async (expr) => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value

await new Promise((r) => ws.addEventListener('open', r))
await send('Page.enable')
await send('Runtime.enable')

await send('Page.navigate', { url: BASE + '/login' })
await sleep(2200)
await evalJs(`fetch('/api/as/accounts/login', {method:'POST', credentials:'include',
  headers:{'Content-Type':'application/json'},
  body: JSON.stringify({type:1, username:'jack', password:'123456', rememberMe:true})})
  .then(r=>r.json()).then(b=>{localStorage.setItem('zk_token', b.data);
    localStorage.setItem('zk_user', JSON.stringify({userId:2, roleId:2, name:'张三'})); return 'ok'})`)

await send('Page.navigate', { url: `${BASE}/order?courseIds=${COURSE}` })
await sleep(4000)
let t = await evalJs('document.body.innerText')
console.log('--- 进入确认页 ---\n' + String(t).slice(0, 400))

const clicked1 = await evalJs(`(() => { const b=[...document.querySelectorAll('button')].find(x=>x.textContent.includes('提交订单')); if(b){b.click();return 'ok'} return 'no-button' })()`)
console.log('点击提交订单:', clicked1)
await sleep(3500)
t = await evalJs('document.body.innerText')
console.log('--- 提交后 ---\n' + String(t).slice(0, 400))

const clicked2 = await evalJs(`(() => { const b=[...document.querySelectorAll('button')].find(x=>x.textContent.includes('确认支付')); if(b){b.click();return 'ok'} return 'no-button' })()`)
console.log('点击确认支付:', clicked2)
await sleep(7000)
t = await evalJs('document.body.innerText')
console.log('--- 支付后 ---\n' + String(t).slice(0, 400))
ws.close()
