<template>
  <main class="wrap">
    <PageHeader title="课程搜索" />

    <div class="panel search-bar">
      <input v-model="keyword" class="input" placeholder="搜课程名，例如：会计 / 教资 / 建造师" @keyup.enter="doSearch" />
      <button class="btn primary" @click="doSearch">搜索</button>
      <div class="chips">
        <button class="chip" :class="{ on: !query.free }" @click="setFree(null)">全部</button>
        <button class="chip" :class="{ on: query.free === true }" @click="setFree(true)">免费</button>
        <button
          v-for="c in topCates" :key="c.id"
          class="chip" :class="{ on: String(query.categoryIdLv1) === String(c.id) }"
          @click="setCate(c.id)">{{ c.name }}</button>
      </div>
    </div>

    <div v-if="loading" class="center muted">搜索中…</div>
    <div v-else-if="error" class="notice bad">{{ error }}</div>
    <template v-else>
      <p class="muted" style="margin:12px 0">
        共 {{ total }} 门课程<span v-if="keyword"> · 关键词「{{ keyword }}」</span>
      </p>
      <div class="grid">
        <article v-for="(c, i) in list" :key="c.id" class="card" @click="open(c.id)">
          <div class="cover" :class="'c' + (i % 3)">
            {{ clean(c.name) }}
          </div>
          <div class="body">
            <h3>{{ clean(c.name) }}</h3>
            <div class="meta">
              <span>{{ c.teacher || '职考云讲师' }}</span>
              <span>{{ c.sections || 0 }} 节</span>
              <span>{{ c.sold || 0 }} 人在学</span>
            </div>
            <div class="price">
              <b>{{ c.price ? fen(c.price) : '免费' }}</b>
              <span class="go">查看详情 →</span>
            </div>
          </div>
        </article>
      </div>
      <div v-if="!list.length" class="center muted" style="padding:40px 0">没有匹配的课程</div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, fen } from '../api'

const route = useRoute()
const router = useRouter()

const keyword = ref(String(route.query.keyword || ''))
const query = ref({ free: null, categoryIdLv1: String(route.query.categoryIdLv1 || ''), categoryIdLv2: String(route.query.categoryIdLv2 || '') })
const list = ref([])
const total = ref(0)
const topCates = ref([])
const loading = ref(true)
const error = ref('')

async function doSearch() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.searchCourses({
      keyword: keyword.value || undefined,
      free: query.value.free === null ? undefined : query.value.free,
      categoryIdLv1: query.value.categoryIdLv1 || undefined,
    categoryIdLv2: query.value.categoryIdLv2 || undefined,
      pageNo: 1,
      pageSize: 12
    })
    list.value = (data && data.list) || []
    total.value = (data && data.total) || 0
    // URL 上保留筛选条件，便于刷新/分享
    const q = {}
    if (keyword.value) q.keyword = keyword.value
    if (query.value.categoryIdLv1) q.categoryIdLv1 = query.value.categoryIdLv1
    if (query.value.categoryIdLv2) q.categoryIdLv2 = query.value.categoryIdLv2
    router.replace({ path: '/search', query: q })
  } catch (e) {
    error.value = '搜索失败：' + e.message
  } finally {
    loading.value = false
  }
}

function setFree(v) {
  query.value.free = v
  doSearch()
}

function setCate(id) {
  query.value.categoryIdLv2 = ''
  query.value.categoryIdLv1 = query.value.categoryIdLv1 === id ? '' : id
  doSearch()
}

function open(id) {
  router.push('/course/' + id)
}

/** ES 高亮会返回 <em> 标签，当纯文本展示时要剥掉 */
function clean(s) {
  return String(s || '').replace(/<\/?em>/g, '')
}

onMounted(async () => {
  try {
    const all = (await api.categories()) || []
    // 只保留一级分类（parentId = 0），否则会把上百个子分类全铺出来
    topCates.value = all.filter((c) => String(c.parentId) === '0')
  } catch (e) {
    topCates.value = []
  }
  await doSearch()
})
</script>

<style scoped>
.search-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 16px; }
.search-bar .input { width: 360px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-left: auto; }
.chip {
  border: 1px solid var(--line); background: #fff; color: #475569;
  border-radius: 999px; padding: 5px 12px; font-size: 12px; cursor: pointer;
}
.chip.on { border-color: #93c5fd; background: #eef2ff; color: var(--brand); font-weight: 600; }
.card { cursor: pointer; }
</style>
