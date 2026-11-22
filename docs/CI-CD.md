# CI/CD：Gogs + Jenkins + Docker

职考云的代码托管、自动构建与容器部署链路。

## 1. 组件

| 组件 | 作用 | 本地地址（示例） |
|---|---|---|
| **Gogs** | 代码托管（Git 服务） | `http://git.zkxt.com/zkxt/zkxt` |
| **Jenkins** | 流水线：拉代码 → 打包 → 构建镜像 → 部署 → 冒烟 | `http://jenkins.zkxt.com/job/zkxt-pipeline/` |
| **Docker** | 运行 15 个微服务容器 + 中间件 | `http://192.168.150.101:2375` |

> 账号密码请按自己的环境配置，仓库里不放真实凭据。

## 2. 仓库结构

```
zkxt/
├── zk-gateway/           网关（端口 10010，StripPrefix=1）
├── zk-auth/              认证服务（8081）
├── zk-user/              用户/学员/员工（8082）
├── zk-course/            课程与目录（8086）
├── zk-learning/          课表、进度、笔记、问答（8090）
├── zk-exam/              考试、题目、错题本（8089）
├── zk-cert/              证书签发与验真（8091）
├── zk-trade/             购物车、订单、退款（8088）
├── zk-pay/               支付单、支付渠道（8087）
├── zk-promotion/         优惠券、兑换码（8094）
├── zk-search/            ES 搜索（8083）
├── zk-media/             视频与文件（8084）
├── zk-message/           站内信、通知（8085）
├── zk-remark/            点赞（8092）
├── zk-data/              数据看板（8093）
├── zk-api/               服务间 Feign 客户端与 DTO
├── zk-common/            公共模块（异常处理、工具、上下文）
├── web/                  Vue3 + Vite 前端（学员端 + 管理端）
├── docker/               部署脚本与 nginx 配置
├── docs/                 设计文档与截图
└── Jenkinsfile           流水线定义
```

## 3. 流水线阶段

```
拉取代码（Gogs main）
  → 编译打包（Jenkins 容器内 Maven 3.8.6 + JDK11，阿里云镜像源）
  → 构建镜像并部署（每个服务：拷 jar → docker build → docker run）
  → 冒烟验证（网关文档 / 门户课程 / 学员登录，带重试）
  → 失败时自动 dump 各容器最后 25 行日志
```

参数：`SERVICES`（留空=全部 15 个，可填 `gateway,auth,user` 只部署部分）、`SKIP_TESTS`。

## 4. 关键脚本

| 文件 | 作用 |
|---|---|
| `Jenkinsfile` | 流水线定义（5 个阶段 + 失败日志） |
| `docker/startup.sh` | 单服务部署：拷 jar → build → run（固定内存、接入 `zkxt-net`、发布端口） |
| `docker/Dockerfile.runtime` | 运行镜像：`openjdk:11.0-jre-buster` + `app.jar` |
| `docker/deploy.sh` | 全量部署：两轮启动，第二轮重启未就绪的服务 |
| `docker/smoke.sh` | 部署后冒烟（带重试，慢启动不误判） |
| `docker/nginx-zkxt.conf` | `*.zkxt.com` 虚拟主机配置 |
| `docker/setup-nginx.sh` | 把上面的配置装到宿主机 nginx 并 reload |
| `docker/hosts-zkxt.txt` | 本地 hosts 片段 |

## 5. 部署时容易踩的坑

| 现象 | 原因 | 处理 |
|---|---|---|
| 拉不到 Docker Hub 镜像 | 网络限制 | 用本地已有镜像做基础（`openjdk:11`），Maven 装在 Jenkins 容器里 |
| Maven 下载极慢 | 没走镜像源 | 显式 `-s settings.xml`，镜像源配置在 `~/.m2/settings.xml` |
| 多模块服务镜像缺失 | jar 在 `zk-xxx/zk-xxx-service/target/` | 构建脚本按 `maxdepth 5` 查找 |
| 服务启动报 `UnknownHostException` | 容器解析不到主机名（MyBatis-Plus `IdWorker` 调 `getLocalHost()`） | 用桥接网络 `zkxt-net`，或 `--add-host` 补 hosts |
| 全量部署时个别服务起不来 | 8G/2核 机器上 15 个 JVM 同时启动，MySQL/Redis 连接被打满 | 提高 `max_connections`；演示时用 `SERVICES=` 只部署核心 6 个 |

## 6. 常用命令

```bash
# 触发构建（带参数）
curl -u <user>:<password> -X POST \
  "http://<jenkins>:8080/job/zkxt-pipeline/buildWithParameters" \
  -d "SERVICES=&SKIP_TESTS=true"

# 看最近一次日志
curl -u <user>:<password> \
  "http://<jenkins>:8080/job/zkxt-pipeline/lastBuild/consoleText" | tail -50

# 查看部署状态
docker ps --filter name=zkxt- --format '{{.Names}}\t{{.Status}}'

# 全量回滚
for c in $(docker ps -aq --filter 'name=zkxt-'); do docker rm -f "$c"; done

# 单个服务重发
bash docker/startup.sh -c zkxt-course -n zk-course -d zk-course -p 8086
```
