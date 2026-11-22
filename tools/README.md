# 工具脚本

部署、联调与自测用的脚本，都在仓库根目录执行。

## 部署与运维

| 脚本 | 用途 |
|---|---|
| `vm_run.py` / `vm_exec.py` / `vm_upload.py` | 通过 Docker API 在远程主机执行命令、传文件 |
| `jenkins_job.py` / `jenkins_wait.py` / `jenkins_view.py` | 触发 Jenkins 构建、等待结果、查看日志 |
| `gogs_login.py` / `short_msgs.py` | Gogs 仓库与提交信息维护 |

## 自测

| 脚本 | 用途 |
|---|---|
| `smoke_all.py` | 15 个服务的接口冒烟，逐条打印状态码 |
| `api_selftest.py` | 按 OpenAPI 文档遍历只读接口 |
| `api_write_sweep.py` | 按 schema 造数据遍历写接口，按返回码分类 |
| `trial_guard_smoke.py` | 试看与越权的服务端兜底校验 |
| `exchange_code_smoke.py` | 兑换码链路（兑换、到账、重复拦截） |
| `data_inventory.py` | 盘点各业务表是否有可测的实例数据 |

## 文档

| 脚本 | 用途 |
|---|---|
| `gen_openapi.py` | 扫描控制器与 DTO，生成 Apifox 可导入的 OpenAPI 文档 |
