import { authState, clearAuth, getToken, setAuth } from './auth'

const BASE = '/api'

/** 把对象拼成 query string（跳过空值） */
export function qs(params = {}) {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => k + '=' + encodeURIComponent(v))
    .join('&')
}

function buildHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) {
    // 网关直接取 authorization 头的原始值做 JWT 校验，不加 Bearer 前缀
    headers['authorization'] = token
  }
  return headers
}

/**
 * 刷新 access token。
 * 关键点：
 * 1. 只刷一次（single-flight）：并发 401 时共用同一个 Promise，
 *    否则多个请求同时刷新会把 refresh token 轮换掉，刚刷新完又失效。
 * 2. credentials: 'include'：refresh token 在 HttpOnly cookie 里，
 *    跨端口/跨域时必须显式允许携带。
 * 3. /accounts/refresh 在网关免登录白名单里，不需要旧 token。
 */
let refreshing = null

export function refreshAccessToken() {
  if (!refreshing) {
    refreshing = fetch(BASE + '/as/accounts/refresh', {
      method: 'GET',
      credentials: 'include'
    })
      .then(async (res) => {
        if (!res.ok) return null
        const text = await res.text()
        const body = text ? JSON.parse(text) : null
        const token = body && body.data
        if (token) {
          setAuth(token, authState.user)
          return token
        }
        return null
      })
      .catch(() => null)
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

function toLogin() {
  clearAuth()
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login?expired=1'
  }
}

async function request(path, options = {}, retried = false) {
  const res = await fetch(BASE + path, {
    credentials: 'include',
    ...options,
    headers: buildHeaders()
  })
  const text = await res.text()
  let body = null
  try {
    body = text ? JSON.parse(text) : null
  } catch {
    body = text
  }

  const unauthorized = res.status === 401 || (body && body.code === 401)

  // 401 且没重试过、且不是登录/刷新接口本身 -> 刷新一次再重放
  if (unauthorized && !retried && !path.startsWith('/as/accounts/')) {
    // 匿名浏览（从未登录）时不该被踢去登录页，交给调用方决定是否引导登录
    if (!authState.token) {
      throw new Error('需要登录')
    }
    const token = await refreshAccessToken()
    if (token) {
      return request(path, options, true)
    }
    toLogin()
    throw new Error('登录已过期')
  }

  if (unauthorized) {
    toLogin()
    throw new Error('登录已过期')
  }

  if (!res.ok) {
    const msg = (body && (body.msg || body.message)) || 'HTTP ' + res.status
    throw new Error(msg)
  }
  // 网关统一包装 R{code,msg,data}
  return body && body.data !== undefined ? body.data : body
}

export const api = {
  login: (form) => request('/as/accounts/login', { method: 'POST', body: JSON.stringify(form) }),
  me: (userId) => request('/us/users/' + userId),
  portal: (pageSize = 30) => request('/ss/courses/portal?pageNo=1&pageSize=' + pageSize),
  best: () => request('/ss/recommend/best'),
  catas: (courseId) => request('/cs/courses/catas/' + courseId),
  // 课表 + 每个小节的学习记录（lessonId 在返回的 id 上）
  courseProgress: (courseId) => request('/ls/learning-records/course/' + courseId),
  myLesson: (courseId) => request('/ls/lessons/' + courseId),
  // 上报学习进度：sectionType 1=视频 2=考试
  reportRecord: (dto) => request('/ls/learning-records', { method: 'POST', body: JSON.stringify(dto) }),
  createExam: (dto) => request('/es/exams', { method: 'POST', body: JSON.stringify(dto) }),
  submitExam: (dto) => request('/es/exams/details', { method: 'POST', body: JSON.stringify(dto) }),
  verify: (code) => request('/cts/cert/verify/' + encodeURIComponent(code)),
  myCerts: () => request('/cts/cert/my'),
  // 错题本
  wrongPoints: () => request('/es/wrong/points'),
  wrongList: (mastered) => request('/es/wrong/list' + (mastered === undefined ? '' : '?mastered=' + mastered)),
  markMastered: (questionId) => request('/es/wrong/' + questionId + '/mastered', { method: 'POST' }),
  removeWrong: (questionId) => request('/es/wrong/' + questionId, { method: 'DELETE' }),
  // 消息中心（网关 StripPrefix=1，/sms/inboxes -> message-service /inboxes）
  inboxes: (params = {}) => {
    const qs = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => k + '=' + encodeURIComponent(v))
      .join('&')
    return request('/sms/inboxes' + (qs ? '?' + qs : ''))
  },
  unreadCount: () => request('/sms/inboxes/unread-count'),
  markRead: (id) => request('/sms/inboxes/' + id + '/read', { method: 'PUT' }),
  markAllRead: () => request('/sms/inboxes/read-all', { method: 'PUT' }),

  // 搜索 / 课程
  searchCourses: (params) => request('/ss/courses/portal?' + qs(params)),
  courseDetail: (id) => request(`/cs/course/${id}?withCatalogue=true&withTeachers=true`),
  categories: () => request('/cs/categorys/list'),

  // 订单 / 支付
  prePlaceOrder: (courseIds) =>
    request('/ts/orders/prePlaceOrder?' + courseIds.map((i) => 'courseIds=' + i).join('&')),
  placeOrder: (dto) => request('/ts/orders/placeOrder', { method: 'POST', body: JSON.stringify(dto) }),
  applyPay: (dto) => request('/ts/pay/order', { method: 'POST', body: JSON.stringify(dto) }),
  mockPay: (orderNo) => request(`/ps/pay-orders/${orderNo}/mock-pay`, { method: 'POST' }),
  orderStatus: (orderId) => request(`/ts/orders/${orderId}/status`),
  myOrders: (params) => request('/ts/orders/page?' + qs(params)),
  enrollFree: (courseId) => request(`/ts/orders/freeCourse/${courseId}`, { method: 'POST' }),

  // 学习 / 积分 / 榜单
  myLessons: (params) => request('/ls/lessons/page?' + qs(params)),
  signIn: () => request('/ls/sign-records', { method: 'POST' }),
  signRecords: () => request('/ls/sign-records'),
  pointsToday: () => request('/ls/points/today'),
  boardSeasons: () => request('/ls/boards/seasons/list'),
  board: (season) => request('/ls/boards' + (season ? '?season=' + season : '')),

  // 笔记
  notes: (params) => request('/ls/notes/page?' + qs(params)),
  saveNote: (dto) => request('/ls/notes', { method: 'POST', body: JSON.stringify(dto) }),
  gatherNote: (id) => request(`/ls/notes/gathers/${id}`, { method: 'POST' }),
  ungatherNote: (id) => request(`/ls/notes/gathers/${id}`, { method: 'DELETE' }),

  // 问答
  questions: (params) => request('/ls/questions/page?' + qs(params)),
  saveQuestion: (dto) => request('/ls/questions', { method: 'POST', body: JSON.stringify(dto) }),
  replies: (params) => request('/ls/replies/page?' + qs(params)),
  saveReply: (dto) => request('/ls/replies', { method: 'POST', body: JSON.stringify(dto) }),

  // 退款
  applyRefund: (dto) => request('/ts/refund-apply', { method: 'POST', body: JSON.stringify(dto) }),
  myRefunds: (params) => request('/ts/refund-apply/page?' + qs({ mine: true, ...params })),

  // 优惠券：按订单课程算可用方案
  availableCoupons: (courses) =>
    request('/prs/user-coupons/available', { method: 'POST', body: JSON.stringify(courses) }),

  // ===== 管理端 =====
  adminLogin: (form) => request('/as/accounts/admin/login', { method: 'POST', body: JSON.stringify(form) }),
  dashboardToday: () => request('/ds/data/today'),
  dashboardTop10: (type) => request('/ds/data/top10?type=' + type),
  boardData: (types) => request('/ds/data/board?types=' + types),
  adminCoursePage: (params) => request('/cs/courses/page?' + qs(params)),
  courseStatistics: () => request('/cs/courses/statistics'),
  upShelf: (id) => request('/cs/courses/upShelf', { method: 'POST', body: JSON.stringify({ id }) }),
  downShelf: (id) => request('/cs/courses/downShelf', { method: 'POST', body: JSON.stringify({ id }) }),
  adminRefundPage: (params) => request('/ts/refund-apply/page?' + qs(params)),
  approveRefund: (dto) => request('/ts/refund-apply/approval', { method: 'PUT', body: JSON.stringify(dto) }),
  adminStudentPage: (params) => request('/us/students/page?' + qs(params)),
  adminNoticePage: (params) => request('/sms/notice-tasks?' + qs(params)),

  // 管理端 - 题库
  adminQuestionPage: (params) => request('/es/questions/page?' + qs(params)),
  createQuestion: (dto) => request('/es/questions', { method: 'POST', body: JSON.stringify(dto) }),
  deleteQuestion: (id) => request('/es/questions/' + id, { method: 'DELETE' }),

  // 管理端 - 内容审核（笔记 / 提问 / 回答）
  adminNotePage: (params) => request('/ls/admin/notes/page?' + qs(params)),
  hideNote: (id, hidden) => request(`/ls/admin/notes/${id}/hidden/${hidden}`, { method: 'PUT' }),
  adminQaPage: (params) => request('/ls/admin/questions/page?' + qs(params)),
  hideQa: (id, hidden) => request(`/ls/admin/questions/${id}/hidden/${hidden}`, { method: 'PUT' }),
  adminReplyPage: (params) => request('/ls/admin/replies/page?' + qs(params)),
  hideReply: (id, hidden) => request(`/ls/admin/replies/${id}/hidden/${hidden}`, { method: 'PUT' }),

  // 管理端 - 教师 / 优惠券 / 分类
  adminTeacherPage: (params) => request('/us/teachers/page?' + qs(params)),
  adminCouponPage: (params) => request('/prs/coupons/page?' + qs(params)),
  adminCategoryTree: () => request('/cs/categorys/getAllOfOneLevel'),
  createCategory: (dto) => request('/cs/categorys/add', { method: 'POST', body: JSON.stringify(dto) }),

  // 管理端 - 写操作
  createCoupon: (dto) => request('/prs/coupons', { method: 'POST', body: JSON.stringify(dto) }),
  createNoticeTask: (dto) => request('/sms/notice-tasks', { method: 'POST', body: JSON.stringify(dto) }),
  noticeTemplates: (params) => request('/sms/notice-templates?' + qs(params)),
  logout: () => request('/as/accounts/logout', { method: 'POST' }),

  // 兑换码
  exchangeCoupon: (code) => request(`/prs/user-coupons/${code}/exchange`, { method: 'POST' }),
  exchangeCodes: (params) => request('/prs/codes/page?' + qs(params)),
  // 学习计划
  plans: (params) => request('/ls/lessons/plans?' + qs(params)),
  savePlan: (courseId, freq) => request('/ls/lessons/plans', { method: 'POST', body: JSON.stringify({ courseId, freq }) }),
  nowLesson: () => request('/ls/lessons/now'),
  lessonValid: (courseId) => request(`/ls/lessons/${courseId}/valid`),

  // 取消订单 / 撤销退款
  cancelOrder: (id) => request(`/ts/orders/${id}/cancel`, { method: 'PUT' }),
  payChannels: () => request('/ts/pay/channels'),
  cancelRefund: (dto) => request('/ts/refund-apply/cancel', { method: 'PUT', body: JSON.stringify(dto) }),

  // 点赞
  likeRecord: (dto) => request('/rs/likes', { method: 'POST', body: JSON.stringify(dto) }),
  likedList: (bizIds) => request('/rs/likes/list?' + bizIds.map((i) => 'bizIds=' + i).join('&')),

  // 注册 / 验证码 / 找回密码
  sendCode: (phone) => request('/us/users/code?phone=' + encodeURIComponent(phone), { method: 'POST' }),
  register: (dto) => request('/us/students/register', { method: 'POST', body: JSON.stringify(dto) }),
  resetPassword: (dto) => request('/us/students/password', { method: 'PUT', body: JSON.stringify(dto) }),

  // 购物车
  cart: () => request('/ts/carts'),
  addCart: (courseId) => request('/ts/carts', { method: 'POST', body: JSON.stringify({ courseId }) }),
  removeCart: (id) => request('/ts/carts/' + id, { method: 'DELETE' }),
  removeCarts: (ids) => request('/ts/carts?' + ids.map((i) => 'ids=' + i).join('&'), { method: 'DELETE' }),

  // 我的考试
  myExams: (params) => request('/es/exams/page?' + qs(params)),

  // 优惠券（学员端）
  myCoupons: (params) => request('/prs/user-coupons/page?' + qs(params)),
  issuingCoupons: () => request('/prs/coupons/list'),
  receiveCoupon: (id) => request(`/prs/user-coupons/${id}/receive`, { method: 'POST' }),
  couponRules: (ids) => request('/prs/user-coupons/rules?' + ids.map((i) => 'couponIds=' + i).join('&')),

  // 管理端 - 编辑与课程创建
  questionDetail: (id) => request('/es/questions/' + id),
  updateQuestion: (id, dto) => request('/es/questions/' + id, { method: 'PUT', body: JSON.stringify(dto) }),
  couponDetail: (id) => request('/prs/coupons/' + id),
  deleteCoupon: (id) => request('/prs/coupons/' + id, { method: 'DELETE' }),
  updateCoupon: (id, dto) => request('/prs/coupons/' + id, { method: 'PUT', body: JSON.stringify(dto) }),
  issueCoupon: (id, dto) => request(`/prs/coupons/${id}/issue`, { method: 'PUT', body: JSON.stringify(dto) }),
  pauseCoupon: (id) => request(`/prs/coupons/${id}/pause`, { method: 'PUT' }),
  updateCategory: (dto) => request('/cs/categorys/update', { method: 'PUT', body: JSON.stringify(dto) }),
  toggleCategory: (dto) => request('/cs/categorys/disableOrEnable', { method: 'PUT', body: JSON.stringify(dto) }),
  courseBaseInfo: (id) => request('/cs/courses/baseInfo/' + id + '?see=0'),
  createCourse: (dto) => request('/cs/courses/baseInfo/save', { method: 'POST', body: JSON.stringify(dto) }),
  saveCatalogue: (id, list) => request('/cs/courses/catas/save/' + id, { method: 'POST', body: JSON.stringify(list) })
}

export function fen(price) {
  return '¥' + (Number(price || 0) / 100).toFixed(0)
}
