<template>
  <main class="wrap">
    <PageHeader title="我的购物车">
<router-link to="/search" class="muted" style="margin-left:auto;font-size:12px">继续选课 →</router-link>
      </PageHeader>

    <div v-if="loading" class="center muted" style="padding:50px 0">加载中…</div>
    <div v-else-if="msg" class="notice bad">{{ msg }}</div>
    <div v-else-if="!list.length" class="panel center" style="padding:50px">
      <div style="font-size:34px">🛒</div>
      <p class="muted" style="margin-top:10px">购物车是空的，去挑几门课吧</p>
      <router-link to="/search"><button class="btn primary" style="margin-top:12px">去选课</button></router-link>
    </div>

    <template v-else>
      <div class="panel">
        <table class="cart-tbl">
          <thead>
            <tr>
              <th style="width:44px">
                <input type="checkbox" :checked="allChecked" @change="toggleAll($event.target.checked)" />
              </th>
              <th>课程名称</th>
              <th style="width:120px">单价(元)</th>
              <th style="width:100px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in list" :key="c.id">
              <td><input v-model="checked" type="checkbox" :value="c.id" /></td>
              <td>
                <div class="course-cell">
                  <div class="cover">{{ (c.courseName || '课')[0] }}</div>
                  <div>
                    <div class="cname">{{ c.courseName }}</div>
                    <div class="muted" style="font-size:11px;margin-top:3px">
                      {{ c.expired ? '课程已下架' : '有效期' + (c.courseValidDate ? '至 ' + String(c.courseValidDate).slice(0, 10) : '长期') }}
                    </div>
                  </div>
                </div>
              </td>
              <td class="price">¥{{ ((c.nowPrice ?? c.price) / 100).toFixed(2) }}</td>
              <td><button class="link-del" @click="removeOne(c)">删除</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel bar">
        <label class="muted" style="display:flex;align-items:center;gap:8px;font-size:13px">
          <input type="checkbox" :checked="allChecked" @change="toggleAll($event.target.checked)" /> 全选
        </label>
        <button class="btn ghost" style="margin-left:14px" :disabled="!checked.length" @click="removeChecked">
          删除
        </button>
        <div class="total">
          合计：<b>¥{{ total.toFixed(2) }}</b>
        </div>
        <button class="btn primary" style="min-width:120px" :disabled="!checked.length" @click="checkout">
          去下单
        </button>
      </div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const list = ref([])
const checked = ref([])
const loading = ref(true)
const msg = ref('')

const allChecked = computed(() => list.value.length > 0 && checked.value.length === list.value.length)
const total = computed(() => {
  const sum = list.value
    .filter((c) => checked.value.includes(c.id))
    .reduce((s, c) => s + Number(c.nowPrice ?? c.price ?? 0), 0)
  return sum / 100
})

function toggleAll(on) {
  checked.value = on ? list.value.map((c) => c.id) : []
}

async function load() {
  loading.value = true
  try {
    list.value = (await api.cart()) || []
    checked.value = list.value.filter((c) => !c.expired).map((c) => c.id)
  } catch (e) {
    msg.value = '购物车加载失败：' + e.message
  } finally {
    loading.value = false
  }
}

async function removeOne(c) {
  try {
    await api.removeCart(c.id)
    await load()
  } catch (e) {
    msg.value = '删除失败：' + e.message
  }
}

async function removeChecked() {
  try {
    await api.removeCarts(checked.value)
    checked.value = []
    await load()
  } catch (e) {
    msg.value = '删除失败：' + e.message
  }
}

function checkout() {
  const ids = list.value.filter((c) => checked.value.includes(c.id)).map((c) => c.courseId)
  if (!ids.length) return
  router.push('/order?courseIds=' + ids.join(','))
}

onMounted(load)
</script>

<style scoped>
.cart-tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.cart-tbl th { text-align: left; color: #64748b; font-size: 12px; font-weight: 500; padding: 10px 8px; border-bottom: 1px solid var(--line); }
.cart-tbl td { padding: 14px 8px; border-bottom: 1px dashed var(--line); }
.course-cell { display: flex; align-items: center; gap: 12px; }
.cover {
  width: 64px; height: 40px; border-radius: 8px; color: #fff; font-weight: 600;
  display: grid; place-items: center; background: linear-gradient(135deg, #1d4ed8, #60a5fa); flex: none;
}
.cname { font-size: 13px; color: #1e293b; font-weight: 600; }
.price { color: #e11d48; font-weight: 600; }
.link-del { border: none; background: transparent; color: #94a3b8; cursor: pointer; font-size: 12px; }
.link-del:hover { color: #b91c1c; }
.bar { display: flex; align-items: center; gap: 10px; position: sticky; bottom: 12px; }
.total { margin-left: auto; font-size: 13px; color: #475569; }
.total b { color: #e11d48; font-size: 20px; }
</style>
