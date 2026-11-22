<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand"><i>职</i><span>职考云</span></div>
      <p class="muted" style="margin:6px 0 18px">注册学员账号，开始学 · 练 · 考 · 证</p>

      <form @submit.prevent="submit">
        <label class="muted">手机号</label>
        <input v-model.trim="form.phone" class="input" maxlength="11" placeholder="11 位手机号" style="margin:6px 0 14px" />

        <label class="muted">验证码</label>
        <div class="code-row">
          <input v-model.trim="form.code" class="input" maxlength="6" placeholder="6 位数字" />
          <button type="button" class="btn ghost code-btn" :disabled="counting || !phoneOk" @click="sendCode">
            {{ counting ? countdown + 's 后重发' : '获取验证码' }}
          </button>
        </div>

        <label class="muted" style="display:block;margin:14px 0 6px">密码（6~20 位）</label>
        <input v-model="form.password" type="password" class="input" placeholder="设置登录密码" />
        <input v-model="form.password2" type="password" class="input" placeholder="再输一次" style="margin-top:8px" />

        <button class="btn primary" style="width:100%;margin-top:18px" :disabled="loading || done">
          {{ loading ? '注册中…' : (done ? '注册成功' : '注 册') }}
        </button>
      </form>

      <div v-if="error" class="notice bad" style="margin-top:14px">{{ error }}</div>
      <div v-if="tip" class="notice" style="margin-top:14px;background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe">{{ tip }}</div>

      <div v-if="done" style="margin-top:16px;text-align:center">
        <router-link :to="{ path: '/login', query: { account: form.phone } }">
          <button class="btn primary" style="width:100%">去登录</button>
        </router-link>
      </div>

      <div class="links">
        <span class="muted">已有账号？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { api } from '../api'

const form = reactive({ phone: '', code: '', password: '', password2: '' })
const loading = ref(false)
const done = ref(false)
const error = ref('')
const tip = ref('')
const counting = ref(false)
const countdown = ref(0)
let timer = null

const phoneOk = computed(() => /^1\d{10}$/.test(form.phone))

async function sendCode() {
  error.value = ''
  tip.value = ''
  try {
    await api.sendCode(form.phone)
    tip.value = '验证码已发送，5 分钟内有效'
    counting.value = true
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(timer)
        counting.value = false
      }
    }, 1000)
  } catch (e) {
    error.value = '发送失败：' + e.message
  }
}

async function submit() {
  error.value = ''
  if (!phoneOk.value) return (error.value = '请输入正确的 11 位手机号')
  if (!form.code) return (error.value = '请输入验证码')
  if (form.password.length < 6 || form.password.length > 20) return (error.value = '密码长度需 6~20 位')
  if (form.password !== form.password2) return (error.value = '两次输入的密码不一致')

  loading.value = true
  try {
    await api.register({ cellPhone: form.phone, password: form.password, code: form.code })
    done.value = true
    tip.value = '注册成功，请用刚设置的密码登录'
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
