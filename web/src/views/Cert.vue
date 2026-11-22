<template>
  <main class="wrap">
    <PageHeader title="证书中心">
<div class="tabs" style="margin-left:auto">
        <button class="tab" :class="{ on: tab === 'mine' }" @click="switchTab('mine')">我的证书</button>
        <button class="tab" :class="{ on: tab === 'verify' }" @click="switchTab('verify')">证书验真</button>
      </div>
      </PageHeader>

    <!-- 我的证书 -->
    <template v-if="tab === 'mine'">
      <div v-if="loadingCerts" class="center muted">加载中…</div>
      <div v-else-if="certsError" class="notice bad">{{ certsError }}</div>
      <div v-else-if="!certs.length" class="notice">
        还没有证书。完成课包学习（学时 ≥ 120 分钟）并参加一次正式考（≥ 60 分）后自动签发。
      </div>
      <div v-else class="cert-grid">
        <article v-for="c in certs" :key="c.certNo" class="cert-card">
          <div class="cert-top">
            <span class="seal">🏅</span>
            <div>
              <div class="cert-type">{{ c.certType }}</div>
              <div class="muted" style="font-size:11px">编号 {{ c.certNo }}</div>
            </div>
            <span class="badge" :class="c.expired ? 'warn' : 'green'" style="margin-left:auto">
              {{ c.expired ? '已过期' : '有效' }}
            </span>
          </div>
          <div class="cert-body">
            <div class="holder">{{ c.userName || '—' }}</div>
            <div class="muted" style="font-size:12px;margin-top:6px">
              考核成绩 {{ c.score }} 分<br />
              签发 {{ c.issueDate }} · 剩余 {{ c.remainDays }} 天
            </div>
          </div>
          <div class="cert-foot">
            <button class="btn ghost" style="padding:6px 12px;font-size:12px"
                    @click="verifyCode(c.verifyCode)">去验真</button>
            <span class="muted" style="font-size:11px">验真码 {{ c.verifyCode }}</span>
          </div>
        </article>
      </div>
    </template>

    <!-- 验真 -->
    <template v-else>
      <div class="panel" style="max-width:640px">
        <label class="muted">输入验真码（证书上的 12 位码）</label>
        <div style="display:flex;gap:10px;margin:10px 0 16px">
          <input v-model="code" class="input" placeholder="例如 rivi4qchhpak" @keyup.enter="verify" />
          <button class="btn primary" :disabled="loading || !code" @click="verify">
            {{ loading ? '查询中…' : '验真' }}
          </button>
        </div>

        <div v-if="error" class="notice bad">{{ error }}</div>
        <div v-else-if="result" class="notice" :class="result.valid ? 'ok' : 'bad'">
          <div style="font-size:15px;font-weight:600;margin-bottom:6px">
            {{ result.valid ? '✓ 证书真实有效' : '✗ ' + (result.message || '未查询到') }}
          </div>
          <div v-if="result.certNo">
            证书编号：{{ result.certNo }}<br />
            持证人：{{ result.holder }}<br />
            证书类型：{{ result.certType }}<br />
            签发时间：{{ result.issueTime }}<br />
            有效期至：{{ result.expireTime }}
          </div>
        </div>

        <p class="muted" style="margin-top:14px">
          证书编号与验真码均可从证书上获取，无需登录即可验真
        </p>
      </div>
    </template>
  </main>
</template>

<script setup>
import PageHeader from '../components/PageHeader.vue'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const tab = ref('mine')
const certs = ref([])
const loadingCerts = ref(true)
const certsError = ref('')

const code = ref('')
const result = ref(null)
const error = ref('')
const loading = ref(false)

async function loadCerts() {
  loadingCerts.value = true
  certsError.value = ''
  try {
    certs.value = (await api.myCerts()) || []
  } catch (e) {
    certsError.value = '证书加载失败：' + e.message
  } finally {
    loadingCerts.value = false
  }
}

function switchTab(t) {
  tab.value = t
  if (t === 'mine') loadCerts()
}

function verifyCode(vc) {
  tab.value = 'verify'
  code.value = vc
  verify()
}

async function verify() {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await api.verify(code.value.trim())
  } catch (e) {
    error.value = '验真失败：' + e.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 支持深链：/cert?code=xxx 直接验真
  const preset = route.query.code
  if (preset) {
    tab.value = 'verify'
    code.value = String(preset)
    verify()
  } else {
    loadCerts()
  }
})
</script>

<style scoped>
.tabs { display: flex; gap: 6px; }
.tab {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
  color: #475569;
}
.tab.on { border-color: #93c5fd; background: #eef2ff; color: var(--brand); font-weight: 600; }
.cert-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.cert-card {
  border: 1px solid #f1d9a8;
  border-radius: 14px;
  background: linear-gradient(135deg, #fffdf7, #fff8e8 55%, #fef3c7);
  overflow: hidden;
}
.cert-top { display: flex; gap: 10px; align-items: center; padding: 14px 16px; border-bottom: 1px dashed #e7cf9b; }
.seal {
  width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center;
  background: linear-gradient(135deg, #b45309, #f59e0b); color: #fff; flex: none;
}
.cert-type { font-size: 14px; font-weight: 600; color: #7c2d12; }
.cert-body { padding: 14px 16px; }
.holder { font-size: 22px; letter-spacing: 5px; color: #7c2d12; }
.cert-foot { padding: 10px 16px; display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,.6); }
.badge.warn { background: #fef2f2; color: #b91c1c; }
</style>
