import { reactive } from 'vue'

// 学员端与管理端各用一套存储，互不覆盖：
// 同一浏览器可同时保持学员和员工两个身份
const TOKEN_KEY = 'zk_token'
const USER_KEY = 'zk_user'
const ADMIN_TOKEN_KEY = 'zk_admin_token'
const ADMIN_USER_KEY = 'zk_admin_user'

function readJson(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || 'null')
  } catch {
    return null
  }
}

/** 是否在管理端路由 */
export function inAdminArea() {
  return typeof window !== 'undefined' && window.location.pathname.startsWith('/admin')
}

// 学员端登录态
export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readJson(USER_KEY)
})

// 管理端登录态
export const adminAuthState = reactive({
  token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
  user: readJson(ADMIN_USER_KEY)
})

/**
 * 取当前请求该用的 token。
 * 管理端只用自己的员工 token，绝不回退到学员 token，
 * 管理端只用自己的 token，不回退学员 token
 */
export function getToken() {
  return inAdminArea() ? adminAuthState.token : authState.token
}

export function isLoggedIn() {
  return !!authState.token
}

export function isAdminLoggedIn() {
  return !!adminAuthState.token
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

export function setAdminAuth(token, user) {
  adminAuthState.token = token || ''
  adminAuthState.user = user || null
  if (token) localStorage.setItem(ADMIN_TOKEN_KEY, token)
  else localStorage.removeItem(ADMIN_TOKEN_KEY)
  if (user) localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(ADMIN_USER_KEY)
}

export function clearAdminAuth() {
  setAdminAuth('', null)
}

/** 解析 JWT payload，只取 userId 用 */
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
