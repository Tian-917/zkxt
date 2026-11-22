<template>
  <div class="adm-login-wrap">
    <div class="adm-login-card">
      <div class="adm-brand">
        <i>职</i>
        <div>
          <b>职考云</b>
          <span>管理后台</span>
        </div>
      </div>
      <p class="sub">营销中心 · 教务管理 · 数据分析</p>

      <form @submit.prevent="doLogin">
        <div class="fld">
          <span class="ico">👤</span>
          <input v-model.trim="form.username" class="adm-input" placeholder="员工账号" autocomplete="username" />
        </div>
        <div class="fld">
          <span class="ico">🔒</span>
          <input v-model="form.password" type="password" class="adm-input" placeholder="密码"
                 autocomplete="current-password" @keyup.enter="doLogin" />
        </div>

        <label class="row-check">
          <input v-model="form.rememberMe" type="checkbox" />
          7 天免登录
        </label>

        <button class="btn-login" :disabled="busy">{{ busy ? '登录中…' : '登 录' }}</button>
      </form>

      <div v-if="msg" class="notice bad" style="margin-top:14px">{{ msg }}</div>

      <div class="foot">
        <router-link to="/login">← 返回学员端</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { decodeJwt, setAuth } from '../auth'

const router = useRouter()
const form = reactive({ username: 'admin', password: '123456', rememberMe: true })
const busy = ref(false)
const msg = ref('')

async function doLogin() {
  msg.value = ''
  if (!form.username || !form.password) {
    msg.value = '请输入员工账号和密码'
    return
  }
  busy.value = true
  try {
    // 管理端专用登录接口，只允许员工账号（isStaff 校验在后端）
    const token = await api.adminLogin({
      type: 1,
      username: form.username,
      password: form.password,
      rememberMe: form.rememberMe
    })
    const payload = decodeJwt(token) || {}
    const user = payload.user || {}
    if (Number(user.roleId) !== 1) {
      msg.value = '该账号不是员工账号，请从学员端登录'
      return
    }
    setAuth(token, { userId: user.userId, roleId: user.roleId, name: form.username })
    try {
      const profile = await api.me(user.userId)
      if (profile && profile.name) setAuth(token, { userId: user.userId, roleId: user.roleId, name: profile.name })
    } catch (e) {
      /* 拿不到资料不影响登录 */
    }
    router.replace('/admin')
  } catch (e) {
    msg.value = e.message || '登录失败'
  } finally {
    busy.value = false
  }
}
</script>

<style>
.adm-login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 16px;
  background:
    radial-gradient(circle at 85% 15%, rgba(249, 115, 22, 0.18), transparent 45%),
    radial-gradient(circle at 15% 85%, rgba(29, 78, 216, 0.22), transparent 45%),
    #0f172a;
}
.adm-login-card {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 18px;
  padding: 32px;
  box-shadow: 0 30px 70px -30px rgba(0, 0, 0, 0.55);
}
.adm-brand { display: flex; align-items: center; gap: 12px; }
.adm-brand i {
  width: 42px; height: 42px; border-radius: 12px; display: grid; place-items: center;
  font-style: normal; color: #fff; font-size: 20px; font-weight: 700;
  background: linear-gradient(135deg, #f97316, #ea580c);
}
.adm-brand b { display: block; font-size: 19px; color: #0f172a; }
.adm-brand span { font-size: 12px; color: #f97316; letter-spacing: 2px; }
.adm-login-card .sub { font-size: 12px; color: #94a3b8; margin: 10px 0 22px; }
.adm-login-card .fld {
  display: flex; align-items: center; gap: 8px; margin-bottom: 14px;
  border: 1px solid var(--line); border-radius: 10px; padding: 0 12px; background: #f8fafc;
}
.adm-login-card .fld .ico { font-size: 13px; opacity: .55; }
.adm-login-card .fld .adm-input { border: none; background: transparent; padding: 11px 0; flex: 1; }
.adm-login-card .fld:focus-within { border-color: #fdba74; background: #fff; }
.adm-login-card .row-check {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #475569; margin: 4px 0 18px;
}
.btn-login {
  width: 100%; border: none; border-radius: 999px; padding: 12px; cursor: pointer;
  font-size: 15px; color: #fff; letter-spacing: 2px;
  background: linear-gradient(135deg, #fb923c, #ea580c);
}
.btn-login:disabled { opacity: .7; cursor: default; }
.adm-login-card .foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 20px; font-size: 12px;
}
.adm-login-card .foot a { color: var(--brand); }
</style>
