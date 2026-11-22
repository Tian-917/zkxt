<template>
  <div class="admin">
    <!-- 未登录 / 非员工：跳转到独立的管理端登录页 -->
    <div v-if="!isStaff" class="gate">
      <div class="gate-card" style="text-align:center">
        <p class="muted" style="font-size:13px">正在跳转管理端登录…</p>
        <router-link to="/admin/login" class="muted" style="font-size:12px">如果没有自动跳转，点这里</router-link>
      </div>
    </div>

    <template v-else>
      <aside class="side">
        <div class="side-logo"><i>职</i> 职考云</div>
        <nav>
          <button v-for="m in menus" :key="m.key" class="menu" :class="{ on: tab === m.key }" @click="switchTab(m.key)">
            <span class="ico"><Icon :name="m.icon" :size="16" /></span>{{ m.label }}
          </button>
        </nav>
        <div class="side-foot">
          <div class="muted" style="font-size:12px">{{ authState.user ? authState.user.name : '' }}（员工）</div>
          <button class="btn ghost" style="padding:5px 10px;font-size:12px;margin-top:8px" @click="logout">退出</button>
        </div>
      </aside>

      <section class="main">
        <header class="topbar">
          <b>{{ currentMenu.label }}</b>
          <span class="muted" style="font-size:12px">{{ currentMenu.desc }}</span>
        </header>
        <component :is="currentComponent" :key="tab" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authState, clearAuth } from '../auth'
import Dashboard from './admin/Dashboard.vue'
import Courses from './admin/Courses.vue'
import Refunds from './admin/Refunds.vue'
import Students from './admin/Students.vue'
import Questions from './admin/Questions.vue'
import Contents from './admin/Contents.vue'
import Categories from './admin/Categories.vue'
import Teachers from './admin/Teachers.vue'
import Coupons from './admin/Coupons.vue'
import Notices from './admin/Notices.vue'
import Icon from '../components/Icon.vue'

const router = useRouter()

const menus = [
  { key: 'dashboard', label: '数据概览', icon: 'chart-column', desc: '平台运营数据概览', comp: Dashboard },
  { key: 'courses', label: '课程管理', icon: 'book-open', desc: '课程上下架与销量统计', comp: Courses },
  { key: 'questions', label: '题库管理', icon: 'file-question', desc: '题目查询与维护', comp: Questions },
  { key: 'categories', label: '分类管理', icon: 'folder-tree', desc: '课程分类维护', comp: Categories },
  { key: 'teachers', label: '讲师管理', icon: 'graduation-cap', desc: '讲师信息维护', comp: Teachers },
  { key: 'contents', label: '内容审核', icon: 'scan-eye', desc: '笔记、提问与回答审核', comp: Contents },
  { key: 'coupons', label: '优惠券', icon: 'ticket', desc: '优惠券列表与发放', comp: Coupons },
  { key: 'refunds', label: '退款审批', icon: 'undo-2', desc: '退款申请与审批', comp: Refunds },
  { key: 'students', label: '学员管理', icon: 'users', desc: '学员信息查询', comp: Students },
  { key: 'notices', label: '通知任务', icon: 'message-square', desc: '站内通知与定时发送', comp: Notices }
]

const tab = ref('dashboard')
const currentMenu = computed(() => menus.find((m) => m.key === tab.value) || menus[0])
const currentComponent = computed(() => currentMenu.value.comp)
// 员工 token 里 roleId=1
const isStaff = computed(() => !!(authState.user && Number(authState.user.roleId) === 1))

// 非员工/未登录：跳转到独立的管理端登录页（学员端登录页不提供员工入口）
function gotoAdminLogin() {
  if (!isStaff.value) {
    router.replace('/admin/login')
  }
}

onMounted(gotoAdminLogin)
watch(isStaff, (v) => {
  if (!v) router.replace('/admin/login')
})

function logout() {
  clearAuth()
  router.replace('/admin/login')
}

function switchTab(key) {
  tab.value = key
}
</script>

<style scoped>
.admin { display: grid; grid-template-columns: 208px 1fr; min-height: 100vh; background: #f5f7fb; }
.gate {
  grid-column: 1 / -1; min-height: 100vh; display: grid; place-items: center;
  background: linear-gradient(135deg, #0f172a, #1e3a8a 60%, #0f172a);
}
.gate-card { width: 340px; background: #fff; border-radius: 16px; padding: 26px; }
.gate-logo { font-weight: 700; font-size: 17px; display: flex; align-items: center; gap: 8px; }
.gate-logo i {
  width: 28px; height: 28px; border-radius: 8px; color: #fff; font-style: normal;
  display: grid; place-items: center; background: linear-gradient(135deg, #2563eb, #4f46e5);
}
.side { background: #0f172a; color: #cbd5e1; padding: 18px 14px; display: flex; flex-direction: column; }
.side-logo { color: #fff; font-weight: 700; display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.side-logo i {
  width: 26px; height: 26px; border-radius: 7px; font-style: normal; display: grid; place-items: center;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}
.menu {
  display: flex; align-items: center; gap: 8px; width: 100%; text-align: left;
  background: transparent; border: none; color: #cbd5e1; padding: 9px 12px;
  border-radius: 9px; font-size: 13px; cursor: pointer; margin-bottom: 3px;
}
.menu:hover { background: rgba(255, 255, 255, .06); }
.menu.on { background: #1d4ed8; color: #fff; font-weight: 600; }
.ico { width: 16px; text-align: center; }
.side-foot { margin-top: auto; padding: 12px; border-top: 1px solid rgba(255, 255, 255, .12); }
.main { padding: 0 0 24px; }
.topbar { display: flex; align-items: baseline; gap: 10px; padding: 16px 24px; background: #fff; border-bottom: 1px solid var(--line); }
</style>
