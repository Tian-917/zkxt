<template>
  <div class="home"><div class="wrap">
    <!-- 首屏：分类导航 + 轮播 -->
    <section class="hero">
      <div class="hero-inner">
        <aside class="cate-nav" @mouseleave="hoverCate = ''">
          <div class="cate-head">全部课程分类</div>
          <div v-for="c in cates" :key="c.id" class="cate-wrap" @mouseenter="hoverCate = c.id">
            <router-link class="cate-item"
                         :to="{ path: '/search', query: { categoryIdLv1: c.id } }">
              <span>{{ c.name }}</span><i>›</i>
            </router-link>
            <!-- 二级分类浮层 -->
            <div v-if="hoverCate === c.id && childrenOf(c.id).length" class="cate-panel">
              <div class="panel-head">{{ c.name }}</div>
              <div class="panel-body">
                <router-link v-for="ch in childrenOf(c.id)" :key="ch.id" class="panel-item"
                             :to="{ path: '/search', query: { categoryIdLv2: ch.id } }">
                  {{ ch.name }}
                </router-link>
              </div>
            </div>
          </div>
          <router-link v-if="!cates.length" class="cate-item" to="/search">
            <span>全部课程</span><i>›</i>
          </router-link>
        </aside>

        <div class="banner">
          <div class="slides">
            <div v-for="(s, i) in slides" :key="i" class="slide"
                 :class="{ on: idx === i }" :style="{ background: s.bg }">
              <div class="slide-txt">
                <span class="tag">{{ s.tag }}</span>
                <h2>{{ s.title }}</h2>
                <p>{{ s.sub }}</p>
                <router-link :to="s.link">
                  <button class="btn-light">立即查看</button>
                </router-link>
              </div>
              <div class="slide-art"><Icon :name="s.art" :size="120" :stroke="1.2" /></div>
            </div>
          </div>
          <div class="dots">
            <i v-for="(s, i) in slides" :key="i" :class="{ on: idx === i }" @click="idx = i"></i>
          </div>
        </div>
      </div>
    </section>

    <!-- 能力条 -->
    <section class="stats">
      <div class="stat"><b>{{ total }}</b><span>门考证课包</span></div>
      <div class="stat"><b>{{ cates.length || 8 }}</b><span>个课程分类</span></div>
      <div class="stat"><b>学·练·考·证</b><span>全流程线上</span></div>
      <div class="stat"><b>真实可验</b><span>证书编号可查</span></div>
    </section>

    <!-- 精品课包 -->
    <section class="block">
      <div class="block-head">
        <h2>精品课包</h2>
        <span class="sub">课包 + 题库 + 模考 + 证书</span>
        <router-link class="more" to="/search">更多 ›</router-link>
      </div>
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="error" class="notice bad">{{ error }}</div>
      <div v-else class="cards">
        <article v-for="(c, i) in courses.slice(0, 8)" :key="c.id" class="card"
                 @click="$router.push('/course/' + c.id)">
          <div class="cover" :style="{ background: coverBg(i) }">
            <Icon class="cover-ico" :name="coverIcon(c, i)" :size="30" :stroke="1.6" />
            <span class="cover-name">{{ shortName(c.name) }}</span>
            <span class="cover-tag">{{ c.sections || 0 }} 节</span>
          </div>
          <div class="card-body">
            <h3>{{ c.name }}</h3>
            <div class="meta">
              <span>{{ c.teacher || '讲师待定' }}</span>
              <span>{{ c.sold || 0 }} 人在学</span>
            </div>
            <div class="card-foot">
              <b>{{ fen(c.price) }}</b>
              <span class="go">查看详情 →</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 新课推荐 -->
    <section v-if="courses.length > 8" class="block">
      <div class="block-head">
        <h2>新课推荐</h2>
        <span class="sub">刚上架的考证课包</span>
        <router-link class="more" to="/search">更多 ›</router-link>
      </div>
      <div class="cards rows">
        <article v-for="(c, i) in courses.slice(8, 14)" :key="c.id" class="card"
                 @click="$router.push('/course/' + c.id)">
          <div class="cover" :style="{ background: coverBg(i + 8) }">
            <Icon class="cover-ico" :name="coverIcon(c, i + 8)" :size="30" :stroke="1.6" />
            <span class="cover-name">{{ shortName(c.name) }}</span>
          </div>
          <div class="card-body">
            <h3>{{ c.name }}</h3>
            <div class="meta">
              <span>{{ c.teacher || '讲师待定' }}</span>
              <span>{{ c.sections || 0 }} 节</span>
            </div>
            <div class="card-foot">
              <b>{{ fen(c.price) }}</b>
              <span class="go">查看详情 →</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 访客引导 -->
    <section v-if="!authState.token" class="cta">
      <div>
        <h3>登录后可购买课包、做题、领证书</h3>
        <p>未登录也能浏览课程详情，先看看再决定</p>
      </div>
      <div class="cta-actions">
        <router-link to="/register"><button class="btn ghost">免费注册</button></router-link>
        <router-link to="/login"><button class="btn primary">立即登录</button></router-link>
      </div>
    </section>
  </div></div>
</template>

<script setup>
import Icon from '../components/Icon.vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, fen } from '../api'
import { authState } from '../auth'

const courses = ref([])
const cates = ref([])
const allCates = ref([])      // 一级 + 二级，用于 hover 展开
const hoverCate = ref('')
const loading = ref(true)
const error = ref('')
const idx = ref(0)
let timer = null

const total = computed(() => courses.value.length)

const slides = computed(() => {
  const list = courses.value.slice(0, 3)
  const bgs = [
    'linear-gradient(120deg, #1d4ed8, #3b82f6 55%, #60a5fa)',
    'linear-gradient(120deg, #047857, #10b981 55%, #34d399)',
    'linear-gradient(120deg, #6d28d9, #8b5cf6 55%, #c084fc)'
  ]
  const tags = ['热门推荐', '新课上架', '考证必备']
  if (!list.length) {
    return [{
      tag: '职考云', title: '学 · 练 · 考 · 证',
      sub: '课包 + 题库 + 模考 + 证书签发与验真',
      link: '/search', bg: bgs[0], art: 'book-open'
    }]
  }
  return list.map((c, i) => ({
    tag: tags[i % tags.length],
    title: c.name,
    sub: `${c.sections || 0} 节课时 · ${c.teacher || '讲师待定'} · ${fen(c.price)}`,
    link: '/course/' + c.id,
    bg: bgs[i % bgs.length],
    art: ['book-open', 'graduation-cap', 'notebook-pen'][i % 3]
  }))
})

function childrenOf(parentId) {
  return allCates.value.filter((c) => String(c.parentId) === String(parentId) && Number(c.level) === 2)
}

function shortName(name) {
  if (!name) return '课包'
  const p = name.split('·')
  return (p[p.length - 1] || name).trim().slice(0, 10)
}

function coverIcon(c, i) {
  const n = c.name || ''
  if (n.indexOf('会计') > -1) return 'file-question'
  if (n.indexOf('教师') > -1) return 'graduation-cap'
  if (n.indexOf('建造') > -1) return 'folder-tree'
  if (n.indexOf('英语') > -1 || n.indexOf('自考') > -1) return 'book-open'
  if (n.indexOf('软考') > -1 || n.indexOf('系统') > -1) return 'chart-column'
  return ['book-open', 'graduation-cap', 'notebook-pen', 'award'][i % 4]
}

function coverBg(i) {
  const palettes = [
    'linear-gradient(135deg, #1d4ed8, #60a5fa)',
    'linear-gradient(135deg, #0f766e, #2dd4bf)',
    'linear-gradient(135deg, #7c3aed, #c084fc)',
    'linear-gradient(135deg, #b45309, #fbbf24)',
    'linear-gradient(135deg, #be123c, #fb7185)',
    'linear-gradient(135deg, #0e7490, #22d3ee)'
  ]
  return palettes[i % palettes.length]
}

function startCarousel() {
  timer = setInterval(() => {
    if (slides.value.length > 1) idx.value = (idx.value + 1) % slides.value.length
  }, 4000)
}

onMounted(async () => {
  try {
    const [page, cats, all] = await Promise.all([
      api.portal(),
      api.categories().catch(() => []),
      api.adminCategoryTree().catch(() => [])
    ])
    courses.value = (page && page.list) || []
    cates.value = (cats || []).slice(0, 8)
    allCates.value = all || []
  } catch (e) {
    error.value = '课包加载失败：' + e.message
  } finally {
    loading.value = false
    startCarousel()
  }
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.home { padding-bottom: 40px; }

/* 首屏 */
.hero { padding: 20px 0 0; }
.hero-inner { display: grid; grid-template-columns: 210px 1fr; gap: 16px; }
.cate-nav {
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 10px 0; height: 320px; overflow: visible;
}
.cate-head { padding: 6px 16px 10px; font-size: 12px; color: #94a3b8; }
.cate-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 16px; font-size: 13px; color: #334155;
}
.cate-item:hover { background: #f5f7ff; color: var(--brand); }
.cate-wrap { position: relative; }
.cate-panel {
  position: absolute; left: 100%; top: 0; z-index: 20;
  width: 420px; background: #fff; border: 1px solid var(--line);
  border-radius: 12px; box-shadow: 0 18px 40px -22px rgba(15, 23, 42, .5);
  padding: 14px 16px;
}
.panel-head { font-size: 13px; font-weight: 600; color: #1e293b; margin-bottom: 10px; }
.panel-body { display: flex; flex-wrap: wrap; gap: 8px 14px; max-height: 240px; overflow: auto; }
.panel-item { font-size: 12px; color: #475569; padding: 2px 0; }
.panel-item:hover { color: var(--brand); }
.cate-item i { color: #cbd5e1; font-style: normal; }

.banner { position: relative; height: 320px; border-radius: 16px; overflow: hidden; }
.slides { position: absolute; inset: 0; }
.slide {
  position: absolute; inset: 0; opacity: 0; transition: opacity .5s ease;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 48px; color: #fff;
}
.slide.on { opacity: 1; }
.slide-txt { max-width: 62%; }
.tag {
  display: inline-block; background: rgba(255, 255, 255, .2);
  padding: 3px 10px; border-radius: 999px; font-size: 12px; margin-bottom: 14px;
}
.slide-txt h2 { font-size: 30px; line-height: 1.3; margin-bottom: 12px; }
.slide-txt p { font-size: 14px; opacity: .9; margin-bottom: 22px; }
.btn-light {
  border: none; background: #fff; color: #1d4ed8; border-radius: 999px;
  padding: 9px 22px; font-size: 13px; font-weight: 600; cursor: pointer;
}
.slide-art { opacity: .32; display: flex; align-items: center; }
.dots { position: absolute; bottom: 16px; left: 48px; display: flex; gap: 8px; }
.dots i {
  width: 22px; height: 4px; border-radius: 3px; background: rgba(255, 255, 255, .45);
  cursor: pointer; transition: all .3s;
}
.dots i.on { background: #fff; width: 30px; }

/* 能力条 */
.stats {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 18px; margin-top: 16px;
}
.stat { display: flex; flex-direction: column; gap: 4px; align-items: center; }
.stat b { font-size: 18px; color: #1e293b; }
.stat span { font-size: 12px; color: #94a3b8; }

/* 分区 */
.block { margin-top: 26px; }
.block-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.block-head h2 { font-size: 19px; color: #0f172a; }
.block-head .sub { font-size: 12px; color: #94a3b8; }
.block-head .more { margin-left: auto; font-size: 13px; color: var(--brand); }

.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.card {
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  overflow: hidden; cursor: pointer; transition: transform .18s, box-shadow .18s;
}
.card:hover { transform: translateY(-3px); box-shadow: 0 18px 36px -24px rgba(15, 23, 42, .5); }
.cover {
      position: relative; height: 118px; display: flex; flex-direction: column;
      justify-content: space-between; align-items: flex-start; padding: 12px; color: #fff;
    }
    .cover-ico { opacity: .55; }
.cover-name { font-size: 14px; font-weight: 600; text-shadow: 0 1px 4px rgba(0, 0, 0, .25); }
.cover-tag {
  position: absolute; top: 10px; right: 10px; font-size: 11px;
  background: rgba(255, 255, 255, .25); padding: 2px 8px; border-radius: 999px;
}
.card-body { padding: 12px 14px 14px; }
.card-body h3 { font-size: 14px; color: #1e293b; line-height: 1.4; height: 40px; overflow: hidden; }
.meta { display: flex; gap: 12px; font-size: 12px; color: #94a3b8; margin: 8px 0 10px; }
.card-foot { display: flex; align-items: center; justify-content: space-between; }
.card-foot b { color: #e11d48; font-size: 16px; }
.card-foot .go { font-size: 12px; color: var(--brand); }

.rows .card-body h3 { height: auto; }
.loading { padding: 40px 0; text-align: center; color: #94a3b8; font-size: 13px; }

/* 访客 CTA */
.cta {
  margin-top: 30px; background: linear-gradient(120deg, #0f172a, #1e3a8a);
  border-radius: 16px; padding: 26px 30px; color: #fff;
  display: flex; align-items: center; justify-content: space-between;
}
.cta h3 { font-size: 18px; margin-bottom: 6px; }
.cta p { font-size: 13px; opacity: .8; }
.cta-actions { display: flex; gap: 10px; }
.cta-actions .btn.ghost { background: rgba(255, 255, 255, .14); color: #fff; border-color: transparent; }
</style>
