import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import Learn from './views/Learn.vue'
import Exam from './views/Exam.vue'
import Cert from './views/Cert.vue'
import Login from './views/Login.vue'
import Wrong from './views/Wrong.vue'
import Inbox from './views/Inbox.vue'
import Search from './views/Search.vue'
import Course from './views/Course.vue'
import Order from './views/Order.vue'
import Orders from './views/Orders.vue'
import Profile from './views/Profile.vue'
import Cart from './views/Cart.vue'
import Register from './views/Register.vue'
import Forgot from './views/Forgot.vue'
import AdminLogin from './views/AdminLogin.vue'
import Refund from './views/Refund.vue'
import Admin from './views/Admin.vue'
import { isLoggedIn } from './auth'
import { showLoginPrompt } from './authPrompt'
import './style.css'
import './admin.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login, meta: { public: true } },
    { path: '/', component: Home, meta: { public: true } },
    { path: '/learn/:courseId', component: Learn, meta: { public: true } },
    { path: '/exam/:courseId/:sectionId', component: Exam },
    { path: '/cert', component: Cert },
    { path: '/wrong', component: Wrong },
    { path: '/inbox', component: Inbox },
    { path: '/search', component: Search, meta: { public: true } },
    { path: '/course/:id', component: Course, meta: { public: true } },
    { path: '/order', component: Order },
    { path: '/orders', component: Orders },
    { path: '/profile', component: Profile },
    { path: '/cart', component: Cart },
    { path: '/register', component: Register, meta: { public: true } },
    { path: '/forgot', component: Forgot, meta: { public: true } },
    { path: '/refund', component: Refund },
    // 管理端自带员工登录门禁，不需要学员登录态
    { path: '/admin/login', component: AdminLogin, meta: { public: true } },
    { path: '/admin', component: Admin, meta: { public: true } }
  ]
})

// 未登录一律去登录页；登录页和管理端放行
router.beforeEach((to) => {
  // 未登录访问需登录页：先弹提示
  if (!to.meta.public && !isLoggedIn()) {
    showLoginPrompt(to.fullPath)
  // 已在页面内就留在原地，首次访问回首页
    if (!router.currentRoute.value.name) return { path: '/' }
    return false
  }
  if (to.path === '/login' && isLoggedIn()) {
    return { path: '/' }
  }
  return true
})

createApp(App).use(router).mount('#app')
