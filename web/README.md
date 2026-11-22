# 职考云前端（web）

> Vue 3 + Vite 的最小可用前端：4 个页面，全部接**真实后端接口**（经网关 10010）。
> 不是原型图——`docs/mockup/` 里的是设计稿，这里是能跑、能点、能出数据的工程。

## 页面

| 路由 | 数据来源 | 说明 |
|---|---|---|
| `/login` | `POST /as/accounts/login`（`type=1`）、`POST /as/accounts/logout` | **学员端**登录：账号 + 密码、7 天免密；员工账号会被后端拒绝（`非学生端用户`） |
| `/admin/login` | `POST /as/accounts/admin/login` | **管理端**独立登录页（深色底 + 橙色按钮，仅员工账号+密码）；学员账号会被后端拒绝（`非管理端用户`） |
| `/` | `GET /ss/courses/portal`、`GET /cs/categorys/list`、`GET /cs/categorys/getAllOfOneLevel` | 首页：**分类导航（悬停展开二级，点击进搜索）** + 自动轮播 + 精品课包 + 新课推荐（游客可看） |
| `/learn/:courseId` | `catas`、`/ls/lessons/{id}`、`learning-records`、`/ls/notes`、`/ls/questions`、`/ls/replies` | 课包目录 + 模拟播放/进度回写 + 小节内「笔记」「问答」；**未购买只开放前 2 节（试看）**，其余上锁、笔记问答隐藏 |
| `/exam/:courseId/:sectionId` | `POST /es/exams`、`POST /es/exams/details` | 组卷 → 答题 → 交卷 → 判分与解析 |
| `/cert?code=xxx` | `GET /cts/cert/verify/{code}`、`GET /cts/cert/my` | 证书中心：我的证书 + 公开验真 |
| `/wrong` | `GET /es/wrong/list`、`GET /es/wrong/points` | 错题本：考点聚合、标记已掌握、移出 |
| `/inbox` | `GET /sms/inboxes`、`PUT /sms/inboxes/{id}/read` | 消息中心：未读/已读筛选、全部已读（导航栏红点） |
| `/search` | `GET /ss/courses/portal?keyword=&categoryIdLv1=&free=` | 搜索：关键词 + 一级分类 + 免费筛选（ES 高亮） |
| `/course/:id` | `GET /cs/course/{id}?withCatalogue=true` | 课程详情：目录、价格、购买/免费报名 |
| `/order?courseIds=` | `prePlaceOrder` → `user-coupons/available` → `placeOrder` → `pay/*` | 确认订单 → **优惠券下拉（方案式，显示优惠/应付）** → 支付成功 |
| `/orders` | `GET /ts/orders/page`、`PUT /ts/orders/{id}/cancel` | 我的订单：状态、明细、去支付/去学习/**取消订单** |
| `/profile` | `us/users`、`ls/lessons`、`ls/lessons/plans`、`es/exams`、`ts/orders`、`prs/user-coupons`、`prs/user-coupons/{code}/exchange`、`ls/points`、`ls/sign-records`、`ls/boards` | 个人中心（左侧菜单）：我的课程 / **学习计划**（本周频次+进度）/ 我的考试 / 我的订单 / 我的优惠券（含**领券中心**与**兑换码兑换**）/ 我的积分（整月签到日历+积分明细+榜单）/ 个人设置 |
| `/register` | `POST /us/users/code`、`POST /us/students/register` | 学员注册：手机号 + 验证码（60s 倒计时）+ 密码；成功后显示「去登录」 |`n| `/forgot` | `POST /us/users/code`、`PUT /us/students/password` | 找回密码：验证码重置登录密码；成功后显示「去登录」 |`n| `/cart` | `GET/POST/DELETE /ts/carts` | 购物车：勾选、删除、合计、去下单（带选中的 courseIds） |
| `/refund` | `POST /ts/refund-apply`、`GET /ts/refund-apply/page` | 退款：填写原因提交、查看退款进度 |

## 访客（未登录）能做什么

| 页面 | 未登录 | 说明 |
|---|---|---|
| `/` 首页、`/search` 搜索、`/course/:id` 课程详情 | ✅ 可浏览 | 对应后端公开接口：`/ss/courses/portal`、`/cs/course/{id}`、`/prs/coupons/list`（实测匿名 200） |
| 点「立即购买 / 加入购物车 / 去学习」或访问受保护页 | → **弹窗**「登录后才能继续」 | 两个选项：**去登录**（带 `redirect` 回跳）/ **暂时不登录**（留在原页，不跳转） |
| 课程详情目录 / 学习页 | **前 2 节标「试看」可学**，第 3 节起 🔒 | 未购买：点试看进学习页（无进度写入、笔记问答隐藏）；点锁定节 → 游客弹登录、已登录跳下单 |
| 试看的判定口径 | 读 `course_catalogue.trailer` | 后端发放播放签名时也按 `trailer` 校验，前端只是展示；两者同一数据源 |
| 服务端兜底 | 播放签名 / 进度写入 | 未购买：锁定节签名返回 `403 课程不支持试看`；写学习进度校验 `lesson.userId == 当前用户`，越权报 `未购买该课程` |
| `/profile`、`/cart`、`/orders`、`/inbox`、`/wrong`、`/learn`、`/exam` | ❌ 需登录 | 路由守卫拦下并跳登录 |

实现要点：`main.js` 给首页/搜索/详情标 `meta.public`；`api.js` 在**没有 token**时遇到 401 只抛 `需要登录`、不再强制跳转（避免访客被莫名踢走）。

## 登录与鉴权

**演示账号**：`jack` / `123456`（学员端账号；`admin` 是员工账号，走学员端登录会被拒「非学生端用户」）

链路：

```
登录页 → POST /as/accounts/login {type:1, username, password, rememberMe}
      → auth-service 调 user-service 校验密码，签发 JWT（RS256）
      → 前端存 localStorage
每次请求 → authorization: <JWT>     （网关直接取原始值，没有 Bearer 前缀）
      → 网关 AuthUtil 校验签名/有效期 → 解出 userId 写入 user-info 头
      → 下游服务 UserContext.getUser() 拿到当前用户
401 → 前端清 token 并跳回 /login?expired=1
```

- `rememberMe=true` 时 token 有效期 7 天，否则只有 **5 分钟**（`JwtConstants.JWT_TOKEN_TTL`）。
- 路由守卫：未登录访问任何页面都重定向到 `/login`。
- 演示便捷：`/login?auto=1` 用默认账号自动登录（生产应移除）。

### token 自动续期

access token 只有 5 分钟，过期后不该把用户踢回登录页。现在的做法（[src/api.js](src/api.js)）：

```
请求 → 401
  → refreshAccessToken()：GET /as/accounts/refresh（浏览器自动带 HttpOnly 的 refresh cookie）
      · single-flight：并发多个 401 只刷一次，避免 refresh token 被轮换多次导致刚刷完又失效
  → 拿到新 access token 存 localStorage
  → 重放原请求（用户无感）
  → 若刷新也失败：清 token，跳 /login?expired=1
```

另外 [App.vue](src/App.vue) 每次进入应用会调一次 `GET /us/users/{id}` 校验会话并同步姓名，这样"刚打开页面"就能发现过期并自动续期。

**为此修了后端两个 bug**（都是课程代码在 localhost/IP 下才会暴露）：

1. `CookieBuilder`：`serverName` 是 `127.0.0.1` / `localhost` 时没有父域，旧逻辑截出空串拼成 `Domain="."`，浏览器直接丢弃整个 cookie → refresh token 从来没进过浏览器。
2. `AccountController.refreshToken`：靠 `Origin` 头 + `host.startsWith("www", 7)` 判断学生端/管理端，而浏览器同源请求不带 `Origin` → 永远「登录超时」。改成按"哪个 cookie 存在"选择。


> 演示密码已被重置为 `123456`。原始种子账号的明文密码不可知，重置方法见 [tools/GenHash.java](../tools/GenHash.java)：
> `java -cp "spring-security-crypto.jar;spring-jcl.jar" tools/GenHash.java 123456` 生成 hash 后更新 `zk_user.user`。


## 启动

```bash
# 前置：后端 5 个服务 + 网关在跑（gateway 10010、course 8086、search 8083、exam 8089、cert 8091）
cd web
pnpm install
pnpm dev          # http://127.0.0.1:5173
```

Vite 把 `/api/*` 代理到 `http://localhost:10010`，由网关路由到各微服务。

## 学习进度回写（模拟播放）

没有真实视频素材，用一个"模拟播放器"驱动整条进度链路：

```
点播放（可选 1x / 10x / 60x）
  → 每 1 秒 moment += 倍速，进度条推进
  → 每推进 15 秒（视频时间）POST /ls/learning-records
        {sectionType:1, lessonId, sectionId, duration, moment, commitTime}
  → 后端 moment * 1.25 >= duration 时判定学完
        → learning_record.finished=1、课表 learned_sections+1、发积分 MQ
  → 前端重新拉进度，徽标变"已学完"、左侧小节出现"已学完"
```

要点：
- **倍速是为了演示**：35 分钟的课 60 倍速下 28 秒到 80%，否则要真坐半小时
- `lessonId` 不是 courseId，来自 `GET /ls/learning-records/course/{courseId}` 返回的 `id`
- `commitTime` 必须是 `yyyy-MM-dd HH:mm:ss`（后端 Jackson 配的是这个格式，ISO 的带 `T` 会 400）
- 没有报名该课包时拿不到 lessonId，页面会提示且不上报

## 消息中心

证书到期提醒是**真送达**的：`certExpireNoticeJob`（每小时）→ `notifyExpiring()` → `MessageClient.sendInbox()`
→ `message-service /inboxes` → 前端 `/inbox` 可见，导航栏红点显示未读数。

```
GET  /sms/inboxes?pageNo=1&pageSize=10&isRead=false   分页（网关 StripPrefix=1）
GET  /sms/inboxes/unread-count                        未读数（导航红点）
PUT  /sms/inboxes/{id}/read                           标记单条已读
PUT  /sms/inboxes/read-all                            全部已读
```

> 这里修了一个原始项目的问题：**message-service 的 pom 没引 `zk-auth-resource-sdk`**，
> 导致 `UserInfoInterceptor` 没注册、`UserContext` 永远是 null，
> 所有"我的消息"类接口都会查出 0 条。补依赖 + `includeLoginPaths: /inboxes/**` 后正常（不带 token 访问返回 401）。


## 下单与支付

```
/course/:id  立即购买
   → /order?courseIds=  prePlaceOrder（生成订单号 + 算价）
   → 选优惠券（POST /prs/user-coupons/available 按课程算方案）
   → 提交订单 placeOrder（带 couponIds，后端重新校验并落 discount_amount）
   → 选支付渠道（mock / wxPay / aliPay）
   → mock 渠道直接 POST /ps/pay-orders/{no}/mock-pay 视为支付成功
   → 轮询 /ts/orders/{id}/status 直到 status=2 → 课表自动出现该课程
```

退款：订单页明细上「申请退款」（`canRefund` 由后端算：已支付且无进行中的退款申请）→ `/refund` 填写原因 → `POST /ts/refund-apply`。

## 管理后台

访问 `/admin`（自带员工登录门禁，无需学员账号）：

```
账号 admin / 密码 123456        → POST /as/accounts/admin/login（isStaff=true）
```

| 模块 | 功能 | 接口 |
|---|---|---|
| 数据概览 | 今日访问/订单额/订单数/新增学员 + 热门课 TOP10 | `GET /ds/data/today`、`/ds/data/top10?type=1` |
| 课程管理 | 分页/筛选/上下架、**新建课程**（可同时建 1 章 1 节）、统计 | `GET /cs/courses/page`、`POST /cs/courses/baseInfo/save`、`catas/save`、`{upShelf,downShelf}` |
| 题库管理 | 分页/筛选、**新增**、**编辑**（回填详情）、删除 | `GET/POST /es/questions`、`PUT/DELETE /es/questions/{id}` |
| 分类管理 | 三级分类树、筛选、**新增/编辑/启停** | `GET /cs/categorys/getAllOfOneLevel`、`POST add`、`PUT update`、`PUT disableOrEnable` |
| 讲师管理 | 讲师列表（课程数、出题数） | `GET /us/teachers/page` |
| 内容审核 | 笔记 / 提问 / 回答 三个页签，隐藏与恢复 | `GET /ls/admin/{notes,questions,replies}/page`、`PUT /ls/admin/*/hidden/{hidden}` |
| 优惠券 | 对齐原版「营销中心」：类型/状态/名称筛选 + 规则文案 + 使用范围 + 推广方式 + 状态中文 + 两段式表单 + 发放（领用/使用期限）+ 查看兑换码 + 删除 | `GET /prs/coupons/page`、`GET/DELETE /prs/coupons/{id}`、`POST /prs/coupons`、`PUT /{id}`、`PUT /{id}/issue`、`PUT /{id}/pause`、`GET /prs/codes/page` |
| 退款审批 | 待审批筛选、同意/拒绝（同意后走 mock 退款并置为退款成功） | `GET /ts/refund-apply/page`、`PUT /ts/refund-apply/approval` |
| 学员管理 | 学员分页列表 | `GET /us/students/page` |
| 通知任务 | 任务列表、**新建通知任务**（默认 5 分钟后推送） | `GET /sms/notice-tasks`、`POST /sms/notice-tasks` |

> 员工 token 的 `roleId=1`，管理端据此判断是否显示后台；学员 token（roleId=2）进来会提示「不是员工账号」。

## 前后端联调测试

一体化脚本（Chrome CDP 真浏览器跑，逐页断言 + 截图 + 收集 JS 报错）：

```bash
node tools/e2e_full.mjs      # 13 项：页面渲染 + 下单支付闭环 + 支付后课表核对
node tools/e2e_ui2.mjs       # 10 项：小节内笔记/提问/回答/回复某人 + 个人中心各页 + 购物车 + 优惠券下拉
node tools/e2e_auth.mjs      # 4 项：注册(验证码) / 短信验证码登录 / 找回密码 / 新密码登录
node tools/e2e_extra.mjs     # 4 项：学习计划 / 取消订单 / 撤销退款 / 回答点赞
node tools/e2e_trial.mjs    # 6 项：分类悬停展开 / 二级分类跳搜索 / 前两节试看 / 锁定节提示
python tools/trial_guard_smoke.py  # 4 项：播放签名与进度写入的服务端兜底
node tools/e2e_anon.mjs     # 8 项：游客可浏览、点购买弹窗、暂时不登录
node tools/e2e_dual_login.mjs    # 7 项：学员站/管理端两个登录页 + 两个登录接口互斥
node tools/e2e_codes.mjs     # 2 项：学员端兑换码兑换 / 管理端兑换码列表
node tools/e2e_coupon_admin.mjs  # 6 项：优惠券管理 UI（表头/中文状态/规则文案/两段式表单/发放弹窗/兑换码）
node tools/e2e_admin.mjs       # 12 项：门禁/登录 + 10 个模块 + 上下架实操
node tools/e2e_admin_write.mjs # 9 项：新增题目/分类/券/通知 + 编辑题目 + 发放弹层 + 新建课程弹层
python tools/smoke_all.py    # 25 个后端接口冒烟
python tools/pay_flow_smoke.py  # 预下单→下单→支付→回调→订单状态
python tools/feature_smoke.py   # 笔记/问答/优惠券/退款 四条链路
python tools/refresh_dashboard.py  # 按自己的课程数据刷新看板 TOP10 快照
```

最近一次结果：**学员端 17/17、管理端 12/12、后端 25/25，前端 JS 报错 0**。
报告与截图在 `docs/mockup/e2e/`。

> 联调中修掉的两个真实问题：
> 1. **大整数 id 精度丢失**：课程 id 是 19 位雪花号，`Number('1549...')` 会被截断成 `1549...800`，
>    后端直接「课程不存在」——前端一律按字符串传。
> 2. **积分榜 500**：`/ls/boards?season=2` 会去查历史分表 `points_board_2`（不存在）；
>    查「当前赛季」必须不传 season（后端走 Redis）。

## 演示用的取舍（要如实讲）

- 登录、JWT、网关鉴权、路由守卫、**token 自动续期**都是真的（auth-service + user-service + gateway 全链路）
- refresh token 轮换：每次刷新都会下发新 refresh cookie（后端已有），前端只负责重放原请求
- 视频是**模拟播放**（没有真实媒资）：进度、学时、课表都是真的，只是画面是占位图
- 学习进度已回写（`POST /ls/learning-records`），学时不再依赖种子数据
- `/login?auto=1` 是演示便利，生产必须删掉

## 截图

`docs/mockup/web-*.png` 是这四个页面的真实运行截图（Chrome 无头），不是设计稿。
