<template>
  <template v-if="showChrome">
    <header class="nav">
      <div class="wrap">
        <div class="logo"><i>职</i>职考云</div>
        <nav class="links">
          <router-link to="/"><Icon name="house" :size="15" />首页</router-link>
          <router-link to="/search"><Icon name="search" :size="15" />搜索</router-link>
          <router-link to="/cart"><Icon name="shopping-cart" :size="15" />购物车</router-link>
          <router-link to="/wrong"><Icon name="notebook-pen" :size="15" />错题本</router-link>
          <router-link to="/cert"><Icon name="award" :size="15" />证书</router-link>
          <router-link to="/orders"><Icon name="receipt" :size="15" />订单</router-link>
          <router-link to="/inbox" class="inbox-link"><Icon name="message-square" :size="15" />
            消息<span v-if="unread" class="navdot">{{ unread > 99 ? '99+' : unread }}</span>
          </router-link>
        </nav>
        <div class="user">
          <template v-if="authState.user">
            <router-link to="/profile" style="color:#334155">{{ authState.user.name }}</router-link>
            <div class="avatar">{{ avatarText }}</div>
            <button class="btn ghost" style="padding:6px 12px;font-size:12px" @click="logout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login"><button class="btn primary" style="padding:6px 14px;font-size:12px">登录</button></router-link>
            <router-link to="/register" style="color:#94a3b8;font-size:12px">注册</router-link>
          </template>
        </div>
      </div>
    </header>
  </template>

  <router-view />
  <LoginPrompt />

  <footer v-if="showChrome" style="padding: 40px 0; text-align: center; color:#94a3b8; font-size:12px">
    职考云 · 学 · 练 · 考 · 证 一体化考证平台
  </footer>
</template>

<script setup>
import Icon from './components/Icon.vue'
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState, clearAuth, setAuth } from './auth'
import { api } from './api'
import { refreshUnread, unread } from './store'
import LoginPrompt from './components/LoginPrompt.vue'

const route = useRoute()
const router = useRouter()

const FULL_PAGE = ['/login', '/register', '/forgot']
const showChrome = computed(() => !FULL_PAGE.includes(route.path) && !route.path.startsWith('/admin'))
const avatarText = computed(() => (authState.user && authState.user.name ? authState.user.name[0] : '游'))

onMounted(async () => {
  // 每次进入应用校验一次会话：token 过期会自动续期，顺带刷新用户名
  const user = authState.user
  if (!authState.token || !user || !user.userId) return
  try {
    const profile = await api.me(user.userId)
    if (profile && profile.name && profile.name !== user.name) {
      setAuth(authState.token, { ...user, name: profile.name })
    }
  } catch (e) {
    /* 401 已由 api 层处理（续期失败会跳登录），这里静默 */
  }
  await refreshUnread()
})

// 切到消息页后未读数会变，离开时同步一下导航红点
watch(() => route.path, (p) => {
  if (p !== '/inbox') refreshUnread()
})

async function logout() {
  try {
    await api.logout()
  } catch (e) {
    /* 接口失败也要清本地登录态 */
  }
  clearAuth()
  router.replace('/login')
}
</script>

<style scoped>
.navdot {
  display: inline-block;
  margin-left: 4px;
  min-width: 15px;
  padding: 0 4px;
  background: #ef4444;
  color: #fff;
  border-radius: 999px;
  font-size: 10px;
  line-height: 15px;
  text-align: center;
  vertical-align: 1px;
}
</style>
