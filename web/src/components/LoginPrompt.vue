<template>
  <div v-if="loginPrompt.visible" class="lp-mask" @click.self="onLater">
    <div class="lp-card">
      <div class="lp-icon">🔒</div>
      <h3>{{ loginPrompt.title }}</h3>
      <p class="lp-desc">{{ loginPrompt.desc }}</p>
      <div class="lp-actions">
        <button class="btn ghost" @click="onLater">暂时不登录</button>
        <button class="btn primary" @click="onLogin">去登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { hideLoginPrompt, loginPrompt } from '../authPrompt'

const router = useRouter()

function onLater() {
  hideLoginPrompt()
}

function onLogin() {
  const redirect = loginPrompt.redirect
  hideLoginPrompt()
  router.push({ path: '/login', query: { redirect } })
}
</script>

<style scoped>
.lp-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, .45);
  display: grid;
  place-items: center;
  z-index: 90;
}
.lp-card {
  width: 380px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 16px;
  padding: 28px 26px 22px;
  text-align: center;
  box-shadow: 0 30px 70px -30px rgba(15, 23, 42, .6);
}
.lp-icon {
  width: 46px;
  height: 46px;
  margin: 0 auto 12px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 22px;
  background: #eef2ff;
}
.lp-card h3 { font-size: 16px; color: #0f172a; margin-bottom: 8px; }
.lp-desc { font-size: 13px; color: #64748b; line-height: 1.7; margin-bottom: 20px; }
.lp-actions { display: flex; gap: 10px; }
.lp-actions .btn { flex: 1; }
</style>
