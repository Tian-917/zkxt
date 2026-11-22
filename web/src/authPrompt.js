import { reactive } from 'vue'

// 全局「需要登录」提示：路由守卫 / 页面动作触发，由 LoginPrompt 组件渲染
export const loginPrompt = reactive({
  visible: false,
  redirect: '/',
  title: '登录后才能继续',
  desc: '该功能需要登录账号，是否现在登录？'
})

export function showLoginPrompt(redirect = '/') {
  loginPrompt.redirect = redirect || '/'
  loginPrompt.visible = true
}

export function hideLoginPrompt() {
  loginPrompt.visible = false
}
