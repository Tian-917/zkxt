import { reactive } from 'vue'

const TOKEN_KEY = 'zk_token'
const USER_KEY = 'zk_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

// 全局登录态：登录后 App/各页面共享，避免各处自己读 localStorage
export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser()
})

export function getToken() {
  return authState.token
}

export function isLoggedIn() {
  return !!authState.token
}

export function setAuth(token, user) {
  authState.token = token || ''
  authState.user = user || null
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

export function clearAuth() {
  setAuth('', null)
}

/** 解析 JWT payload（不校验签名，只用于取 userId 展示） */
export function decodeJwt(token) {
  try {
    const part = token.split('.')[1]
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(b64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}
