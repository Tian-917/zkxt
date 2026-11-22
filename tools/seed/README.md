# 种子数据说明（P1）

> 目标：让系统"看起来就是职考云"，而不是"课程1、课程2"的教学项目。
> 全部可重跑：`python tools/seed/gen_seed.py --apply`（幂等，只做 UPDATE）。

## 1. 做了什么

| 对象 | 改造前 | 改造后 |
|---|---|---|
| 课程（14 门） | 课程1 / 课程2 / java泛型 … | 初级会计职称 · 零基础全程班、教师资格 · 综合素质（小学）、软考中级 · 系统集成项目管理工程师 … |
| 分类 | 散落在 IT·互联网 | 统一挂到 **考试·考证** 下，并新增「计算机软考」「人力资源」两个二级分类 |
| 课程目录（168 条） | 第一章 带你玩 / 第一章 带你玩-第1课 | 第1章 会计概述 / 1.1 会计概述 · 考点精讲 / 1.2 · 真题解析 … |
| 题库（6 道） | Java 基本数据类型、JDk 安装步骤… | 存货跌价准备转回、固定资产折旧、教师职业道德、项目进度压缩…（单选/多选/答案解析齐全） |
| 题库（新增 30 道） | — | 初级会计实务 / 经济法 / 教资 / 软考，挂到各课程第一节，可直接组卷 |
| 讲师（4 位） | 旧关联指向 Java/前端/舞蹈讲师 | 王敏（财会）、李昊（教资）、张锐（软考）、陈静（建工/医药），已重建 14 门课的讲师关联 |
| 报名记录 | — | user_id=1 报名全部 14 个课包（考试前置校验需要，顺带让「我的课表」有内容） |
| ES 索引（12 条） | 旧课程名 | 与新课程名、价格、分类一致 |

## 2. 四个考证方向

| 方向 | 课程数 | 分类 |
|---|---|---|
| 财会金融 | 3 | 初级会计 ×2、中级会计 ×1 |
| 公考教资 | 2 | 综合素质、教育知识与能力 |
| 计算机软考 | 2 | 系统集成、软件设计师 |
| 建造工程 | 2 | 二级建造师（法规 / 施工管理） |
| 其他 | 5 | 执业药师、法考民法、考研英语、人力资源四级、自考本科英语 |

## 3. 脚本

| 脚本 | 作用 |
|---|---|
| [gen_seed.py](gen_seed.py) | 生成并执行 SQL（`--apply`），产物是 [docs/sql/seed-cert-platform.sql](../sql/seed-cert-platform.sql) |
| [gen_teachers_questions.py](gen_teachers_questions.py) | 4 位讲师 + 30 道考证题 + 14 条报名记录，产物 [docs/sql/seed-teachers-questions.sql](../sql/seed-teachers-questions.sql) |
| [sync_es.py](sync_es.py) | 把新课程名/价格/分类 bulk 更新到 ES 的 `course` 索引 |

**为什么要单独同步 ES**：正式链路是「课程上架 → 发 MQ(`course.up`) → search-service 重建索引」。这里只改了数据库，不重放事件 ES 就还是旧数据，搜索页会露馅。

## 4. 已知缺口

- **MongoDB 未启动**：考试记录存 Mongo（`zk_exam` 服务的 `shared-mongo.yaml` 指向 `192.168.150.101:27017`）。VM 上 27017 不通时，`POST /exams` 会在组卷完成后卡在写记录这一步（日志里能看到 `Total: 18`，说明组卷本身是通的）。
  解法：VM 上启动 MongoDB，或改用本地 Mongo。
- **题库只有 6 + 30 题**：够一轮 18 题的会计模考，不够多轮；需要更多可按同样格式往 `gen_teachers_questions.py` 里加。
- **`zk_course.subject`（63 条）没动**：那是「课程章节练习」用的另一套题库，和考试模块的 `zk_exam.question` 是两套。
- **课程详情富文本**（`course_content`）里可能还有占位文案，未逐条替换。

## 5. 验证方式

```sql
-- 课程
SELECT id, name, second_cate_id, price FROM zk_course.course ORDER BY id;
-- 目录
SELECT name FROM zk_course.course_catalogue WHERE course_id = 1 ORDER BY c_index LIMIT 6;
-- 题库
SELECT q.name, d.answer FROM zk_exam.question q JOIN zk_exam.question_detail d ON d.id = q.id;
```

ES 侧：`GET http://192.168.150.101:9200/course/_search?q=会计`
