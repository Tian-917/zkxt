<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand"><i>职</i><span>职考云</span></div>
      <p class="muted" style="margin:6px 0 22px">登录后学习、做题、领证</p>

      <form @submit.prevent="submit">
        <label class="muted">账号（用户名 / 手机号）</label>
        <input v-model.trim="form.account" class="input" placeholder="请输入账号" style="margin:6px 0 14px" />

        <label class="muted">密码</label>
        <input v-model="form.password" type="password" class="input" placeholder="请输入密码"
               style="margin:6px 0 8px" @keyup.enter="submit" />

        <label class="row-check">
          <input type="checkbox" v-model="form.rememberMe" />
          7 天免密登录
        </label>

        <button class="btn primary" style="width:100%" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <div v-if="error" class="notice bad" style="margin-top:14px">{{ error }}</div>
      <div v-if="expired" class="notice" style="margin-top:14px;background:#fffbeb;color:#b45309;border:1px solid #fde68a">
        登录已过期，请重新登录
      </div>

      <div class="links">
        <router-link to="/register">去注册</router-link>
        <span class="muted">·</span>
        <router-link to="/forgot">找回密码</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { clearAuth, decodeJwt, setAuth } from '../auth'

const router = useRouter()
const route = useRoute()
const form = reactive({ account: '', password: '', rememberMe: true })
const loading = ref(false)
const error = ref('')
const expired = ref(false)

onMounted(() => {
  expired.value = route.query.expired === '1'
  if (route.query.account) form.account = String(route.query.account)
})

async function submit() {
  error.value = ''
  if (!form.account || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const dto = { type: 1, password: form.password, rememberMe: form.rememberMe }
// 含 @ 或纯数字按手机号传
    if (/^\d{11}$/.test(form.account)) dto.cellPhone = form.account
    else dto.username = form.account

    const token = await api.login(dto)
    const payload = decodeJwt(token) || {}
    const user = payload.user || {}

    // 员工账号不走学员端：引导到独立的管理端登录页
    if (Number(user.roleId) === 1) {
      clearAuth()
      error.value = '员工账号请从管理后台登录'
      setTimeout(() => router.replace('/admin/login'), 800)
      return
    }

    setAuth(token, { userId: user.userId, roleId: user.roleId, name: dto.username || dto.cellPhone })
    try {
      const profile = await api.me(user.userId)
      if (profile && profile.name) {
        setAuth(token, { userId: user.userId, roleId: user.roleId, name: profile.name })
      }
    } catch (e) {
      /* 拿不到资料不影响登录 */
    }
    router.replace(route.query.redirect || '/')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
