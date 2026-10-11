# 2\-WorkOS MVP 研发设计文档

最终技术路线确定为：

```Plain Text
前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router
桌面：Tauri
后端：Java 21 + Spring Boot 3
ORM：MyBatis-Plus
数据库：PostgreSQL
缓存：无（MVP 不引入 Redis）
Markdown：Milkdown / Tiptap
搜索：PostgreSQL pg_trgm（中文友好，不用 tsvector）
部署：Docker Compose
```

> **修订说明（V1\.3）**
>
> 本轮是"领域模型冻结 2\.0"，与文档 1（V1\.3）配套。
>
> ```Plain Text
> Item 类型收敛为 TASK / NOTE
>   Follow-up 从类型降为能力（next_follow_up_at 非空）
>
> 删除 parent_id（不引入子任务）
> 删除 Tag / item_tag（后置 V1.1）
>
> 状态变更只保留 POST /items/{id}/transitions
>   删除 /complete、/waiting、/follow-up
>
> Reminder 状态简化为 PENDING / DISMISSED / CANCELLED
>
> project_context：current_progress → progress_note
>   并区分 稳定信息 / 动态状态
>
> Daily 明确为"快照 + 表达"，不是事实真源
>
> Redis / 对象存储不进 MVP
> 新增单用户最小身份（auth）
>
> Today ⑥ 建议处理 → ⑥ 待安排
> ```

> **修订说明（V1\.3\.3 交互减负）**
>
> 与文档 1 的 V1\.3\.3 配套，不改领域模型。
>
> ```Plain Text
> 「待安排」改成决策队列：每条给 [今天做] [排到明天] [暂不安排]
>   暂不安排 = 显式降为 P2（不是隐藏）
>
> Today 三层分组：今天做什么 / 今天别忘了 / 我在等谁 + 还没安排
>
> Reminder 三个动作：现在跟进 / 稍后提醒 / 忽略这次
>   忽略提醒不改变业务事实（next_follow_up_at 不动）
>   MVP 取消后端 Reminder 调度，改由客户端驱动
>
> WorkLog 定义为"补充事实"，界面文案改"记一句"
> "现在跟进"改成三选一快捷操作（对方没回 / 对方给了 / 暂时不跟）
> Daily 改成"新增事实 + 更新草稿"，不是重新生成整篇
>
> 删除 related_company（"人"只留一个自由文本字段）
> 删除 work_log.source（MVP 无 import）
> 删除 WebSocket
>
> 首次使用：示例数据与真实工作区分离
> 时区按系统 / 浏览器初始化
>
> 新增：用户维护成本预算 / 原则十一（显式优先于推断）
> K3 改为可用性测试；四句"真源关系"写死
> ```

> **修订说明（V1\.3\.4 可用性补强）**
>
> 与文档 1 的 V1\.3\.4 配套。来自"28 场景 × 3 角色"的压力测试。
>
> ```Plain Text
> ① Quick Capture 本地暂存
>     断网时写本地队列，联网自动提交
>     只暂存"新捕获"，不做离线编辑与同步
>
> ② 手机端只读 + 速记（响应式下探到 375px）
>     手机只做"看 Today"和"记一句"
>
> ③ Inbox 增加"先都收下"
>     一次性把未整理项设为 TODO（P2、无项目）
>     整理完成条件放宽为 status != INBOX
>
> ④ Item 列表返回"最近一条记录"摘要，供卡片展示
>
> ⑤ Daily 增加"本周"只读聚合
>
> ⑥ 明确"删除 = 取消"的接口与文案边界
> ```
>
> 这轮之后建议产品侧冻结，进入原型与实现。

> **修订说明（V1\.3\.5 上下文恢复补强）**
>
> 与文档 1 的 V1\.3\.5 配套。来自一份企业真实场景的"用户一天工作旅程"评审。
>
> 那份文档的解法（AI 自动总结 / 会议自动上下文 / 跨系统关联 / 消息聚合）
> 属于我们明确排除的范围，已归档为 V2 / V3 输入，不进 MVP。
>
> 但它暴露的 3 个小洞值得补：
>
> ```Plain Text
> ① 「继续上次」入口
>    最近打开的事项存本地（不上服务端），Today 顶部给"继续"
>
> ② Item 卡片显示"还剩 N 步"
>    前端解析 description 里的 Markdown checklist
>    → 列表接口必须返回 description
>
> ③ 记录占位文案改为"做了什么 / 为什么这么决定？"
> ```

> **修订说明（V1\.3\.1 走查修正）**
>
> 用 13 个真实场景做压力测试后，模型层无需改动，但补齐 7 处规则：
>
> ```Plain Text
> ① ⑥ 待安排恢复 "priority IN (P0, P1)" 无条件入选
> ② 退出 WAITING 清空 waiting_*，并写 Activity(WAITING_ENDED)
> ③ 未完成计划口径：planned_date < 今天（含老化提示）
> ④ 定义"跟进动作"的两种走向
> ⑤ 承诺式文案的推导规则（用现有字段，不新增字段）
> ⑥ Daily 增加"未归属"分组
> ⑦ 种子数据三细节（相对日期 / 可清除 / 埋点排除）
> ```

> **修订说明（V1\.1）**
>
> 本文档与《1\-WorkOS MVP 产品需求文档》配套；与旧版本冲突之处，以最新版本（V1\.3）为准。
>
> 主要变更：以文档 3/4 的表结构为唯一版本（本文旧版 DDL 已删除）；Item 类型收敛为 3 类；引入 Waiting/Follow-up 单一真源；搜索改为 pg_trgm；新增 settings 表驱动时区；Today 改为 6 区且不重复计数；WorkLog 只保留两条系统记录。

> **修订说明（V1\.2）**
>
> 本轮为领域模型冻结修订。
>
> 主要变更：
>
> ```Plain Text
> 状态定义生命周期；类型定义业务语义与可用能力
> 新增 waiting_since / waiting_expected_at / cancelled_at
> WorkLog 只存人工记录；系统事件改存 item_activity
> 状态变更统一走 /transitions（可带载荷），其余为语法糖
> ⑥ 建议 → ⑥ 建议处理
> Today 统计层互斥、展示层允许次级标签
> 早晨简报分为"需要你决定 / 今日概览"
> Context 结构化 + notes_markdown
> Reminder 状态改为 PENDING / FIRED / DISMISSED / CANCELLED
> ```

# WorkOS MVP 研发设计文档

**项目名称：** WorkOS
**版本：** MVP V1\.3\.5
**文档状态：** 产品调研阶段基线（领域模型 + 技术路线）
**冻结状态：** 产品模型已冻结（FROZEN V1\.3\.5）；接口与字段在技术设计阶段仍可调整，但改后必须同步文档 3/4/6
**文档类型：** 研发设计文档
**对应 PRD：** WorkOS MVP 产品需求文档 V1\.3\.5
**目标平台：** Web \+ macOS Desktop
**后端：** Java 21 \+ Spring Boot 3
**前端：** Vue 3 \+ TypeScript \+ Vite
**数据库：** PostgreSQL
**桌面框架：** Tauri 2\.x

---

# 文档目标

本文档用于指导 WorkOS MVP 的实际研发实施。

与产品 PRD 不同，本文件进一步明确：

- 前端工程结构

- 后端工程结构

- 页面结构

- 数据模型

- 数据库表

- 状态机

- REST API

- Markdown 编辑器

- 搜索

- Reminder

- Project Context

- Work Log

- Tauri 桌面能力

- Docker 部署

- 测试

- MVP 开发拆分

目标是：

> **研发人员可以根据本文档直接开始开发，而不需要重新设计核心业务模型。**
> 
> 

---

# 技术选型

## 2\.1 技术栈

---

# 为什么选择 Vue

WorkOS 是典型的：

> 管理后台 \+ 文档编辑器 \+ 工作台
> 
> 

类型应用。

Vue 3 非常适合：

- 大量表单

- 列表

- Drawer

- Modal

- Tree

- Calendar

- Markdown 编辑器

- Dashboard

- 快捷操作

- 状态驱动 UI

Vue 官方目前提供 Vue 3 的 TypeScript 一等支持，并推荐 Vite 作为现代 Vue 项目的构建方式。\(Vue\.js\)

因此：

```Plain Text
Vue 3
+
TypeScript
+
Vite
```

作为 WorkOS 前端技术基线。

---

# 为什么选择 Tauri

WorkOS 需要：

- macOS App

- 全局快捷键

- 系统通知

- 系统托盘

- 开机启动

- 窗口控制

这些属于桌面能力。

业务页面仍然使用 Vue。

因此：

```Plain Text
Vue
 ↓
Vite
 ↓
Tauri
 ↓
macOS
```

Tauri 官方支持 Vue，并支持通过 Vite 构建 SPA；官方的 `create-tauri-app` 也直接提供 Vue 模板。\([Tauri](https://tauri.app/start/frontend/?utm_source=chatgpt.com)\)

---

# PostgreSQL 私有化部署

## 5\.1 结论

PostgreSQL 本身就是开源数据库，可以：

- 本地部署

- 公司服务器部署

- 私有云部署

- Docker 部署

- Kubernetes 部署

- 商业产品内部部署

PostgreSQL 使用 PostgreSQL License，官方明确说明该许可证是宽松的开源许可证，并且商业软件使用无需支付许可费用。\(PostgreSQL\)

因此 WorkOS 不需要购买 PostgreSQL 商业授权。

---

## 5\.2 MVP 部署

开发环境：

```Plain Text
Docker Compose
│
├── workos-server
└── postgres
```

生产环境：

```Plain Text
Docker
│
├── WorkOS Backend
└── PostgreSQL
```

说明（V1\.3）：

```Plain Text
MVP 只有一个有状态组件：PostgreSQL
Redis 不进入 MVP（无缓存 / 无分布式会话需求）
对象存储不进入 MVP（Markdown 只支持图片 URL）
```

---

# 总体系统架构

```Plain Text
WorkOS
                            │
             ┌──────────────┴──────────────┐
             │                             │
       Web Browser                    macOS Desktop
             │                             │
       Vue3 + Vite                    Tauri + Vue
             │                             │
             └──────────────┬──────────────┘
                            │
                       HTTPS / REST
                            │
                   ┌────────▼─────────┐
                   │  Spring Boot API │
                   │      Java 21     │
                   └────────┬─────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  PostgreSQL   │
                    │  唯一数据源    │
                    └───────────────┘
```

---

# MVP 架构原则

## 7\.1 单体优先

MVP 不采用微服务。

只部署：

```Plain Text
workos-server
```

内部采用模块化结构。

---

## 7\.2 不引入 Spring Cloud

MVP 不需要：

```Plain Text
Nacos
Gateway
OpenFeign
RocketMQ
Seata
Kubernetes
```

原因：

WorkOS 当前主要是单用户/小规模用户场景。

微服务不会带来明显收益，反而增加：

- 部署成本

- 调试成本

- 数据一致性问题

- 本地开发复杂度

---

# 后端工程结构

```Plain Text
workos-server
│
├── pom.xml
│
└── src/main/java/com/workos
    │
    ├── WorkosApplication.java
    │
    ├── common
    │   ├── config
    │   ├── exception
    │   ├── response
    │   ├── security
    │   └── util
    │
    ├── project
    │   ├── controller
    │   ├── service
    │   ├── mapper
    │   ├── entity
    │   └── dto
    │
    ├── item
    │   ├── controller
    │   ├── service
    │   ├── mapper
    │   ├── entity
    │   └── dto
    │
    ├── worklog
    │
    ├── daily
    │
    ├── reminder
    │
    ├── search
    │
    ├── settings
    │
    └── auth
```

说明（V1\.3）：

```Plain Text
删除 tag 模块
   MVP 不做标签

新增 settings
   时区 / 日界 / 每日节奏 是"今天"的唯一真源

新增 auth
   单用户最小身份（本机免登录 + 非本机必须登录）

没有 followup / waiting 模块
   Follow-up 是 Item 的能力，Waiting 是 Item 的状态
   拆成独立模块会让代码模型与产品模型脱节
```

---

# 前端工程结构

```Plain Text
workos-web
│
├── package.json
├── vite.config.ts
├── tsconfig.json
│
└── src
    │
    ├── main.ts
    │
    ├── App.vue
    │
    ├── router
    │
    ├── stores
    │
    ├── api
    │
    ├── layouts
    │
    ├── views
    │   ├── today
    │   ├── inbox
    │   ├── items
    │   ├── projects
    │   ├── calendar
    │   ├── daily
    │   └── search
    │
    ├── components
    │   ├── item
    │   ├── project
    │   ├── daily
    │   ├── markdown
    │   ├── reminder
    │   └── common
    │
    ├── composables
    │
    ├── types
    │
    └── utils
```

---

# 核心领域模型

```Plain Text
Project
   │
   ├── ProjectContext     当前状态
   │
   ├── Item
   │     ├── Task         我要做的事
   │     └── Note         只记录，不进入执行流
   │
   │   Item 的能力与状态：
   │     状态   INBOX / TODO / DOING / WAITING / DONE / CANCELLED
   │     能力   计划 / 截止 / 跟进 / 提醒
   │
   ├── WorkLog            人的工作记录
   ├── Activity           系统事件
   │
   └── Daily

Reminder                  独立提醒对象
   └── 可选引用 Item / Daily
```

说明（V1\.3\.2）：

```Plain Text
Follow-up  不是实体、不是类型，是 Item 的能力（next_follow_up_at 非空）
Waiting    不是实体、不是类型，是 Item 的状态
Reminder   不是业务状态，是通知机制
没有 Tag / parent_id
```

---

# Project 数据模型

## 11\.1 字段

```Plain Text
id
name
description

status
color

created_at
updated_at
archived_at
sort_order
version
```

项目上下文（目标 / 重点 / 进展 / 阻塞 / 下一步 / 关键人员 / 关键链接）**不在 project 表内**，统一由 `project_context` 独立表承载。

字段（V1\.3）：

```Plain Text
稳定信息
  goal
  key_people
  key_links

动态状态
  focus
  blocker
  next_action
  progress_note    原 current_progress

可选补充
  notes_markdown

updated_at
version
```

规则：

> Context 是结构化状态板，不是 Markdown 文档。
> 目标/重点/进展/阻塞/下一步 必须是短文本字段，
> 这样 Today、项目卡片、AI、搜索才能直接读取。

设计意图（V1\.3）：

```Plain Text
为什么要拆稳定信息 / 动态状态
  稳定信息半年才会改一次，不需要提醒维护
  动态状态会随项目推进变化，需要保鲜提示
  不拆开，用户就会被要求维护 7 个字段
  3 个月后一定会说"又让我更新 Context？"

为什么保留 progress_note（原 current_progress）
  它回答核心五问之一："我上次做到哪里？"
  删掉它，用户回到项目只能靠翻 WorkLog 重建

为什么改名
  current_progress 看起来像"项目进度百分比"
  改成 progress_note 并明确：
    它是用户对现状的一句话判断
    不是系统计算的进度
```

V1\.3\.2 减负修正：

```Plain Text
再进一步：progress_note 降为**可选**

必填：focus / blocker / next_action
可选：progress_note

为空时由系统给默认文本（用户可改可删）：
  最近完成：完成工单接口
  正在进行：工单联调

为什么
  强制手写"当前进展"是重复劳动——系统已经知道
  "最近完成了什么、正在做什么"
  但删掉字段又不行：一个最近没有完成任何事项的项目，
  就再也答不出"我上次做到哪了"

  所以：保留字段 + 默认文本 + 允许为空
  （对应原则十：用户只记录事实一次）
```

详见《3\-WorkOS MVP 数据库、API 与页面详细设计》第六章。

---

# Project 状态

MVP：

```Plain Text
ACTIVE
ARCHIVED
```

默认：

```Plain Text
ACTIVE
```

归档项目不出现在默认 Today 中。

---

# Item 数据模型

```Plain Text
id

project_id

title
description

item_type
status
priority

planned_date
plan_rank

due_at
next_follow_up_at

waiting_since
waiting_expected_at

completed_at
cancelled_at

waiting_for

related_person     自由文本，一个字段（V1.3.3 删除 related_company）

created_at
updated_at
version
```

说明：

```Plain Text
planned_date / plan_rank  → 今日计划（新增）
due_at                    → 最晚什么时候完成
next_follow_up_at         → 我什么时候再次主动推动
                            （非空即表示"这条有跟进能力"）

waiting_for               → 在等谁（status = WAITING 时必填）
waiting_since             → 什么时候开始等
waiting_expected_at       → 对方预计什么时候给（可空）

cancelled_at              → 取消时间

不再有 start_date、deleted、parent_id、related_company
```

"人"的字段收敛（V1\.3\.3）：

```Plain Text
删除 related_company     它没有任何产品角色
不建 Person / Company 实体
不做"按人员筛选"         避免长成人员维度

只保留 related_person 一个自由文本字段
  进入 Waiting 时默认带成 waiting_for，用户不用填两遍
```

为什么不能把"人"删干净：

```Plain Text
"我在等谁 / 我该催谁" 是头号差异点
非等待类的跟进（"周三问问张三进度"）也需要一个具体的人
```

为什么删除 parent_id（V1\.3 设计意图）：

```Plain Text
原则
  一个 Item = 一个可闭环事项

一旦引入父子任务，就必须回答：
  父任务什么算完成？
  子任务进不进 Today？
  父任务是否自动完成？
  父任务 due 和子任务 due 什么关系？
  Waiting 挂父还是挂子？
  Daily 记录谁？

个人工具没有精力维护这套分支。

反过来，这条原则决定了 Waiting 是否成立：
  "等待厂商接口" 必须是独立 Item
  否则一条大任务卡住时，整条任务都不能进入 DOING

代价与对策
  过度原子化会让 Inbox 变多，整理成本上升
  → 在 Inbox 与 Item Drawer 给出"要不要拆开"的轻提示
  → 但不引入 subtask 机制
```

五种时间语义（V1\.2）：

| 时间 | 回答的问题 | 影响 Today |
|---|---|---|
| planned_date | 我计划哪天做 | 是 |
| due_at | 最晚什么时候完成 | 是 |
| next_follow_up_at | 我什么时候再次推动 | 是 |
| waiting_expected_at | 对方预计什么时候给 | 否（只打标记） |
| reminder.remind_at | 系统什么时候提醒我 | 否 |

---

# Item Type

```Java
public enum ItemType {

    TASK,

    NOTE
}
```

规则：

```Plain Text
TASK       我要做的事（默认）
NOTE       只记录，不进入执行流（只允许 INBOX / DONE / CANCELLED）

删除 FOLLOW_UP / WAITING / SYNC / REMINDER：
  FOLLOW_UP 是能力，不是类型（next_follow_up_at 非空）
  WAITING  是状态，不是类型
  REMINDER 是独立提醒对象，不是类型
  SYNC     本质是带时间的 TASK，用 due_at 表达
```

NOTE 的界面文案（V1\.3\.2）：

```Plain Text
底层复用 status = DONE
界面上显示"已归档"，不显示"已完成"

原则
  数据模型可以复用，用户概念不能混乱
```

为什么 Follow-up 不是类型（V1\.3 设计意图）：

```Plain Text
用户记事时不会想"这是 Task 还是 Follow-up"
他只会想"我得催一下张三"

要求他先选类型 = 多一道判断成本，而且选错了没人纠正

而"我在跟进什么"这个视图
  可以由 next_follow_up_at 非空 100% 推导
  而且比类型更可靠：用户可能忘选类型，但不会忘设跟进时间
```

跟进动作（V1\.3\.1 定义，V1\.3\.3 改为快捷操作）：

```Plain Text
到点后点"现在跟进"，是三选一的快捷操作，不是打开编辑页

界面
┌──────────────────────────────────────────┐
│ 厂商接口问题                              │
│ 结果？[对方没回] [对方给了] [暂时不跟]     │
└──────────────────────────────────────────┘

A：对方没回
  系统先生成默认文案（"已跟进 XX"），用户可改可不改
  next_follow_up_at 推到下次
  status 保持 WAITING

B：对方给了
  status WAITING → DOING（继续做）或 DONE（到此为止）
  按"退出 Waiting"规则清空 waiting_*

C：暂时不跟
  next_follow_up_at = null
  从"待跟进"消失，但仍留在 Items

不允许"点完什么都不发生"
```

设计意图：

```Plain Text
用户真正需要的是"处理掉这个提醒"，不是"填写一条工作日志"
系统在背后替他留下记录（默认文案），他不必打字
否则用户的第一反应是"太麻烦，先不点"
```

承诺式文案的推导规则（V1\.3\.1，不新增字段）：

```Plain Text
① 别人欠我（事实，可以说）
     status = WAITING
     AND waiting_for 非空
     AND next_follow_up_at <= now
        ↓
   "该催 {waiting_for} 了：{title}"

② 我和某人相关（中性陈述）
     related_person 非空 AND due_at 临近
        ↓
   "{related_person} · {due_at} 前 · {title}"
```

V1\.3\.2 修正：

```Plain Text
禁止生成 "你答应 {related_person} ……"

原因
  due_at 可能只是我自己定的内部截止
  并不代表我对谁承诺过

  系统替用户断言承诺 = 文案比数据模型表达得更强
  用户会立刻觉得系统在瞎编

原则
  "你答应"这种话只能由用户自己写在标题里
```

跟进历史（V1\.3\.2 补充）：

```Plain Text
Follow-up 没有独立历史，历史 = WorkLog

因此视图里不要出现"已完成跟进"这种分区
  （会让人以为 Follow-up 是有生命周期的实体）

正确分区
  该跟的：今日 / 未来 / 逾期
  +
  跟进历史（我之前催过几次、跟对方说了什么）

跟进历史的数据来源
  每次实际跟进行为记一条 WorkLog
```

能力矩阵（V1\.3）：

```Plain Text
状态定义生命周期；类型定义业务语义与可用能力。

类型       可 WAITING   可 DONE   进 Today   可挂 Reminder   进日报
TASK          是          是         是           是            是
NOTE          否          是(归档)   否           否            否
```

注意：

> 旧版表述"类型不驱动流程"不准确。
> 准确说法是：类型不定义生命周期，但决定可用能力。

---

# Item Status

```Java
public enum ItemStatus {

    INBOX,

    TODO,

    DOING,

    WAITING,

    DONE,

    CANCELLED
}
```

---

# Item Priority

```Java
public enum ItemPriority {

    P0,

    P1,

    P2,

    P3
}
```

语义（V1\.3\.2 补充，界面上要有提示）：

```Plain Text
P0  不处理会产生立即影响
P1  本周期必须主动推进
P2  正常工作
P3  有空再做

为什么必须写清
  P1 若只表示"重要"，
  一个项目最终会有 8 条 P1 一起进"待安排"，
  用户又回到"那我到底先做哪个？"

  优先级必须成为用户做决策的语言，而不只是颜色
```

---

# Item 状态流转规则

```Plain Text
INBOX      → TODO, CANCELLED
TODO       → DOING, WAITING, DONE, CANCELLED
DOING      → TODO, WAITING, DONE, CANCELLED
WAITING    → TODO, DOING, DONE, CANCELLED
DONE       → TODO
CANCELLED  → TODO
```

说明：

```Plain Text
DONE → TODO
  撤销完成 / 重开，completed_at 清空，记录系统 WorkLog

CANCELLED → TODO
  恢复误取消的事项

个人工具必须可撤销，终态不可逆是错误设计。
```

---

# 状态转换接口

唯一的状态变更入口（V1\.3）：

```HTTP
POST /api/items/{id}/transitions
```

可以携带业务载荷，一次原子完成：

```JSON
{
  "version": 3,
  "toStatus": "WAITING",
  "waitingFor": "XX厂商",
  "waitingExpectedAt": "2026-09-23T10:00:00Z",
  "nextFollowUpAt": "2026-09-24T02:00:00Z",
  "remark": "已联系，等待接口文档"
}
```

属性修改走 PUT：

```HTTP
PUT /api/items/{id}
```

```JSON
{
  "version": 3,
  "nextFollowUpAt": "2026-09-24T02:00:00Z",
  "relatedPerson": "张三"
}
```

设计意图（V1\.3 删除三个 action 端点）：

```Plain Text
旧版：
  POST /items/{id}/transitions   ← 声明为"唯一状态变更入口"
  POST /items/{id}/complete
  POST /items/{id}/waiting
  POST /items/{id}/follow-up

问题：
  一边说"唯一入口"，一边并排三个 action，自相矛盾
  /follow-up 会让 Follow-up 在 API 层再次"实体化"

现在：
  状态变更  → POST /items/{id}/transitions（可带载荷）
  属性修改  → PUT /items/{id}

前端可以在 api/item.ts 里包 convenience 函数，
但 HTTP 契约只有这两个入口。
```

---

# WorkLog 数据模型

WorkLog 是**人记录的工作内容**，不是系统事件（V1\.2 明确）。

定义再收紧一句（V1\.3\.3）：

> **只有 Item 表达不了的事实，才值得写 WorkLog。**

```Plain Text
id

project_id
item_id

work_date
occurred_at

content

created_at
updated_at
```

删除 `source` 的理由（V1\.3\.3）：

```Plain Text
MVP 明确不做第三方集成
        ↓
IMPORT 没有任何来源
        ↓
只留一个取值（MANUAL）的字段 = 没有必要的复杂度

将来接 Jira / Git / AI 时再加，形态是 source_type + source_id
```

界面文案：

```Plain Text
❌ 工作记录    听起来像义务
✅ 记一句      听起来像顺手

位置：Item Drawer 内，完成事项后不弹窗
```

系统状态变化不再写入 WorkLog，而是写入 Activity：

```Plain Text
过去（V1.1）
  TODO → DOING   → 写一条 SYSTEM WorkLog
  DOING → DONE   → 写一条 SYSTEM WorkLog

现在（V1.2）
  TODO → DOING   → 写一条 item_activity
  DOING → DONE   → 写一条 item_activity
```

---

# Item Activity 数据模型（V1\.2 新增）

```Plain Text
id

item_id

activity_type     CREATED | STATUS_CHANGED | FIELD_CHANGED
                  | PLAN_CHANGED | WAITING_ENDED
from_status
to_status
field_name
old_value
new_value

remark
actor             USER | SYSTEM
occurred_at
```

用途：

```Plain Text
Item Drawer 的"活动时间线"
状态变化审计
Context 保鲜的"项目近期是否有变化"判断
```

禁止：

```Plain Text
进入 Daily 的"工作内容"
```

但不等于完全无关（V1\.3\.2 修正）：

```Plain Text
Activity 可以用来判断"今天发生了什么变化"
  今天是否进入 Waiting
  今天是否退出 Waiting
  今天是否真的跟进过
        ↓
  决定 Daily 的"跟进与等待"区块写什么

为什么必须这样
  否则日报每天都写一遍：
    9/20 等待厂商接口
    9/21 等待厂商接口
    9/22 等待厂商接口
  四行同样的字，没有信息量

  正确做法：只写"今天发生变化的"
```

WAITING_ENDED（V1\.3\.1 新增）：

```Plain Text
写入时机
  status 从 WAITING 变为 TODO / DOING / DONE

remark
  "已等待 3 天"

为什么必须有它
  退出 WAITING 时 waiting_since 会被清空
  而"上次等了多久"是复盘"为什么这个项目慢"的关键证据
  所以字段可以清空，历史不能丢
```

理由：

> 日报的价值来自"我做了什么"，不是"系统发生了多少次状态变更"。

---

# Daily 数据模型

```Plain Text
id

daily_date

content
generated_content
status

created_at
updated_at
version
```

约束：

```Plain Text
daily_date UNIQUE
```

一个日期只有一份主日报。

规则：

```Plain Text
generated_content  → 系统生成
content            → 用户内容，永不自动覆盖
```

设计意图（V1\.3）：

```Plain Text
Daily 是"快照 + 表达"，不是事实真源

事实真源
  Item     事项状态真源
  WorkLog  工作事实真源

Daily
  人对上述事实的表达
  生成 = 对当前事实拍一张快照
  不是实时同步

因此：
  18:00 生成日报
  18:30 又完成一件事
        ↓
  不自动改 content
  只更新 generated_content
  用户点"更新日报草稿"（增量补充，不是重新生成整篇）

计划真源
  Item.planned_date 是计划的唯一真源
  Markdown 里的"明日计划"只是表达，不回写 Item
```

分组规则（V1\.3\.1）：

```Plain Text
按项目分组

project_id 为空的事项 → 归入"未归属"分组 → 排在最后

原因
  一天做 10 件杂事时，这些事很可能都没有项目
  没有这条规则，日报会散成一堆条目
```

生成方式（V1\.3\.3 改为增量）：

```Plain Text
背景
  12:00 点一次"生成"，15:00 又完成两件事，18:00 再点一次
  → 用户会担心"会不会把我上午改的覆盖掉"

  虽然模型里写了"不覆盖 content"，
  但用户不应该需要理解这件事

界面语义
  ❌ 生成 / 重新生成 / 应用
  ✅ 展示"新增的工作事实" + [更新日报草稿]

  多次点击的语义是累加，而不是重置
```

---

# Tag 数据模型（V1\.3 后置，MVP 不建表）

```Plain Text
MVP 不实现 Tag

原因（设计意图）
  Project + 类型 + 状态 + 优先级 + 日期 已经足够分类
  标签会变成第二套分类系统
  并产生"这条到底打哪个标签"的纠结

后置条件
  真实使用中出现"用现有维度真的分不开"的场景
  → 放到 V1.1
```

---

# Item Tag（V1\.3 后置，MVP 不建表）

```Plain Text
随 tag 一起后置
```

---

# Reminder 数据模型

```Plain Text
id

kind
item_id

title

remind_at

status
triggered_at
acknowledged_at

created_at
```

状态：

```Plain Text
PENDING
DISMISSED
CANCELLED
```

含义（V1\.3）：

```Plain Text
PENDING     待处理
DISMISSED   这次提醒已处理
            （注意：不等于业务已处理）
CANCELLED   已取消
```

Reminder 的三种用户动作（V1\.3\.3）：

| 动作 | 对 Reminder | 对业务事实（Item） |
|---|---|---|
| 现在跟进 | → 已处理 | 记一条工作记录；更新下次跟进时间或改状态 |
| 稍后提醒 | 修改 `remind_at` | 不变 |
| 忽略这次 | → 已处理 | **不变**（`next_follow_up_at` 保持原值） |

设计意图：

```Plain Text
用户点"关闭提醒"≠"我已经催了张三"
可能只是"我现在不想看它"

如果把两者混在一起：
  提醒关掉了 → 系统以为催过了 → 下次不再提醒 → 事情真的漏了

所以：
  提醒是注意力机制，不是业务状态
```

启动补偿：

```Plain Text
应该提醒 = PENDING 且 remind_at <= now

打开 App 时把这一批列出来
不需要判断"用户到底看没看到"
```

为什么去掉 FIRED（设计意图）：

```Plain Text
服务器触发 ≠ 通知送达 ≠ 用户看到 ≠ 用户处理

用"已触发"冒充"已送达"是自欺
而 MVP 本来就不承诺送达

所以：
  不建模"送达"
  只建模"待处理 / 已处理 / 已取消"

原则
  绝不能让 Reminder 成为 Follow-up 是否存在的依据
```

kind：

```Plain Text
ITEM        事项提醒
DAILY       日报提醒
STANDALONE  独立提醒
```

---

# PostgreSQL 表设计

> **本节旧版 DDL 已删除（V1\.1）**
>
> 原因：本文档旧版使用 `workos_` 表前缀、`TIMESTAMP`、`item.start_date`、`daily.content_markdown`，与文档 3/4 形成两套互相冲突的表结构。
>
> 表结构唯一真源为：
>
> ```Plain Text
> 《3-WorkOS MVP 数据库、API 与页面详细设计》第五~二十章
> +
> 《4-WorkOS MVP 开发实施规格》Flyway Migration
> ```
>
> 统一规范：
>
> ```Plain Text
> 表名    无前缀：project / project_context / item / work_log / daily / tag / item_tag / reminder / settings
> 时间点  TIMESTAMPTZ
> 日期    DATE
> 主键    BIGINT GENERATED BY DEFAULT AS IDENTITY（API 以 number 暴露）
> 日报    daily.content + daily.generated_content
> ```
>
> V1\.1 相对文档 3/4 的增量：
>
> ```Plain Text
> 新增 settings 表（时区 / 日界 / 通知）
> project_context 恢复 current_progress
> item 新增 planned_date、plan_rank
> work_log 新增 occurred_at
> reminder 重构为 kind + item_id(可空) + title + notified_at
> 新增 pg_trgm GIN 索引
> ```

---

# 为什么暂时不使用 Elasticsearch

MVP 数据规模非常小。

即使用户积累：

```Plain Text
10,000 Tasks
10,000 Work Logs
3,000 Daily
```

PostgreSQL 仍然可以很好地处理。

因此 MVP 使用 PostgreSQL `pg_trgm` + `ILIKE`。

不使用 `to_tsvector()`：

> WorkOS 内容以中文为主，PostgreSQL 原生 FTS 不做中文分词。

避免：

```Plain Text
PostgreSQL
+
Elasticsearch
+
同步机制
```

带来的额外复杂度。

---

# Search 设计

搜索对象：

```Plain Text
Project
Item
WorkLog
Daily
```

可以先采用：

```SQL
ILIKE '%keyword%'
```

但必须配合 `pg_trgm` 索引，否则中文 LIKE 会全表扫描：

```SQL
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_item_title_trgm
ON item USING gin (title gin_trgm_ops);
```

不采用 `to_tsvector()`：

```Plain Text
WorkOS 内容以中文为主
PostgreSQL 原生 FTS 不做中文分词（需 zhparser / pg_jieba）
该演进路线实际不可用
```

如果未来达到千万级文本，再考虑 OpenSearch。

---

# Today API

```HTTP
GET /api/today?date=2026-09-21
```

返回：

```JSON
{
  "date": "2026-09-21",
  "focusItems": [],
  "followUps": [],
  "waitingItems": [],
  "completedItems": [],
  "workLogs": [],
  "overdueItems": []
}
```

---

# Inbox API

```HTTP
GET /api/inbox
```

快速创建：

```HTTP
POST /api/items/quick-capture
```

请求：

```JSON
{
  "content": "跟进厂商接口问题"
}
```

服务器默认：

```Plain Text
title = content
item_type = TASK
status = INBOX
priority = P2
source = QUICK_CAPTURE
```

注意：

```Plain Text
POST /api/items （普通创建）默认 status = TODO
POST /api/items/quick-capture 默认 status = INBOX
```

Inbox 支持键盘单行整理（V1\.1）：

```Plain Text
1/2/3/4 → P0/P1/P2/P3   P → 项目   D → 日期
F → Follow-up           W → Waiting
Enter → 保存并 INBOX → TODO        Tab → 下一条

不打开 Drawer，复用 PUT /api/items/{id} 与 /transitions
```

---

# Item API

```HTTP
GET    /api/items
POST   /api/items
GET    /api/items/{id}
PUT    /api/items/{id}
DELETE /api/items/{id}
```

查询参数：

```Plain Text
projectId
type
status
priority
startDate
endDate
tagId
keyword
```

---

# Project API

```HTTP
GET    /api/projects
POST   /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

Project Context：

```HTTP
PUT /api/projects/{id}/context
```

---

# WorkLog API

```HTTP
GET  /api/work-logs
POST /api/work-logs
GET  /api/work-logs/{id}
PUT  /api/work-logs/{id}
DELETE /api/work-logs/{id}
```

---

# Daily API

```HTTP
GET /api/dailies/{date}
PUT /api/dailies/{date}
```

生成初稿：

```HTTP
POST /api/dailies/{date}/generate
```

返回：

```JSON
{
  "content": "# 2026-09-21\n\n## 今日完成\n..."
}
```

用户修改后再：

```HTTP
PUT /api/dailies/{date}
```

---

# Reminder API

```HTTP
GET    /api/reminders
POST   /api/reminders
DELETE /api/reminders/{id}
```

---

# Search API

```HTTP
GET /api/search?keyword=工单
```

返回：

```JSON
{
  "projects": [],
  "items": [],
  "workLogs": [],
  "dailies": []
}
```

---

# 页面路由

Vue Router：

```Plain Text
/
├── /today
├── /inbox
├── /items          全部事项（含"未计划 / 无日期"筛选）
├── /items/:id
├── /projects
├── /projects/:id
├── /daily
├── /daily/:date
├── /settings
└── /search
```

说明（V1\.3）：

```Plain Text
MVP 一级导航只有 5 个
  Today / Inbox / Items / Projects / Daily

Follow-ups 下沉（不再是一级导航）
  Today 上的"待跟进 / 等待中"区块
  +
  Items 的"跟进"筛选视图
  （路由 /follow-ups 可以保留为 Items 的预设筛选，不作为独立页面）

Calendar 后置到 V1.1
  MVP 不提供 /calendar
```

Inbox 与"待安排"是两个不同的心理状态（V1\.3\.3）：

```Plain Text
Inbox（界面文案：待整理）
  这件事我甚至还没决定怎么处理

待安排（界面文案：还没安排）
  我已经承认这是一件正式的工作，
  但还没决定什么时候做

为什么必须写清楚
  用户在 Inbox 里整理完一条（设成 P1、无日期），
  它会从 Inbox 消失、出现在 Today 的"待安排"
    → 用户会问："我明明整理完了，为什么它还在首页？"

  所以界面上要用这两句话直接说明，不要让用户自己悟
```

默认：

```Plain Text
/
→ /today
```

---

# Layout

```Plain Text
┌──────────────────────────────────────────────┐
│ Header                                       │
├────────────┬─────────────────────────────────┤
│            │                                 │
│ Sidebar    │          Main Content            │
│            │                                 │
│ Today      │                                 │
│ Inbox      │                                 │
│ Items      │                                 │
│ Projects   │                                 │
│ Calendar   │                                 │
│ Daily      │                                 │
│ Search     │                                 │
│            │                                 │
└────────────┴─────────────────────────────────┘
```

---

# Today 页面

页面组件：

```Plain Text
TodayView
│
├── TodayHeader
├── FocusItems
├── FollowUpList
├── WaitingList
├── CompletedList
├── WorkLogTimeline
└── QuickInput
```

---

# Quick Input

页面右下角：

```Plain Text
+ 快速记录
```

同时支持：

```Plain Text
⌥ Space
```

打开：

```Plain Text
QuickCaptureModal
```

输入：

```Plain Text
跟进厂商接口问题
```

Enter 后立即创建 Inbox Item。

---

# Item Drawer

点击 Item：

```Plain Text
┌───────────────────────────────────────┐
│ 工单接口联调                     ×    │
├───────────────────────────────────────┤
│                                       │
│ 项目       项管系统                   │
│ 类型       Follow-up                  │
│ 状态       Waiting                    │
│ 优先级     P1                         │
│ 截止       09-23                      │
│ 跟进       09-23 10:00               │
│                                       │
│ 描述                                   │
│ ------------------------------------  │
│                                       │
│ 工作记录                               │
│                                       │
│ 09-21 完成接口开发                     │
│ 09-22 等待测试                         │
│                                       │
└───────────────────────────────────────┘
```

---

# Project 页面

结构：

```Plain Text
ProjectDetail
│
├── ProjectHeader
├── ContextCard
├── CurrentFocus
├── Waiting
├── FollowUps
├── Tasks
├── WorkLogs
└── Daily
```

---

# Project Context 编辑

采用 Markdown 编辑器。

例如：

```Markdown
# 项管系统

## 当前目标

完成工单系统 V1。

## 当前重点

工单自动分配。

## 当前阻塞

客户范围规则待产品确认。

## 下一步

完成联调。
```

---

# Markdown 编辑器设计

推荐：

```Plain Text
Milkdown
```

原因：

WorkOS 本质上希望保存真正的 Markdown，而不是仅仅保存富文本 HTML。

数据：

```Plain Text
Markdown
 ↓
Editor
 ↓
Markdown
```

而不是：

```Plain Text
HTML
 ↓
Editor
 ↓
HTML
```

---

# Markdown 编辑器模式

MVP 支持：

```Plain Text
EDIT
PREVIEW
SPLIT
```

默认：

```Plain Text
SPLIT
```

布局：

```Plain Text
┌──────────────────┬──────────────────────┐
│ Markdown         │ Preview              │
│                  │                      │
│ # 今日工作       │ 今日工作             │
│                  │                      │
│ ## 完成          │ 完成                 │
│                  │                      │
└──────────────────┴──────────────────────┘
```

---

# Outline 实现

解析 Markdown AST。

例如：

```Plain Text
# A
## B
### C
## D
```

生成：

```Plain Text
A
├── B
│   └── C
└── D
```

前端维护：

```TypeScript
interface OutlineNode {
  id: string
  level: number
  text: string
  children: OutlineNode[]
}
```

点击：

```Plain Text
OutlineNode
```

通过 heading anchor 滚动。

---

# Daily 自动生成

服务器执行：

```Plain Text
GET 今日完成 Item
+
GET 今日 WorkLog
+
GET 今日 FollowUp
+
GET 今日 Waiting
```

生成：

```Markdown
# 2026-09-22 日报

## 今日完成

### 项管系统

- 完成工单接口

### 用户中心

- 完成客服岗位功能

## 今日工作记录

### 项管系统

- 与产品确认工单分配规则

## 跟进与等待

- [等待] 产品确认规则（产品）
- [跟进] 厂商接口问题（XX 厂商）

## 问题与风险

- 暂无

## 明日计划

- 完成联调
```

模板只有这一版（按项目分组）：

> 文档 2 / 3 / 4 旧版存在三套模板，已统一。

注意：

> 自动生成只是生成草稿，不覆盖用户已有日报。
> 
> 

---

# Daily 生成策略

如果当天日报不存在：

```Plain Text
POST /generate
```

创建日报。

如果日报已经存在：

默认禁止直接覆盖。

返回：

```Plain Text
日报已存在，是否重新生成？
```

未来可以支持：

```Plain Text
追加到现有日报
```

---

## 收工复盘与计划结转（V1\.1 新增）

Daily 页面同时承担"收工复盘"职责。

```Plain Text
每日 eod_review_time（默认 18:00）提醒一次
        ↓
生成日报初稿（按项目分组）
        ↓
处理未完成计划（planned_date < 今天）：
  结转到明天 / 改期 / 转 Waiting / 取消
        ↓
确认明日计划候选
        ↓
保存
```

结转规则：

```Plain Text
未完成且 planned_date < 今天
        ↓
用户显式点击"结转到明天"
        ↓
planned_date = 明天，plan_rank 排在末尾
```

口径修正（V1\.3\.1）：

```Plain Text
不是"只有今天没做完的"
而是"所有欠账"
否则隔一天没打开，昨天的欠账就再也提示不出来了
```

不做自动结转：

> 自动结转会让"今日计划"永远完不成，计划本身的意义消失。
> 必须由用户做一次显式取舍。

---

# Reminder 调度

MVP **取消后端调度**（V1\.3\.3）。

设计意图：

```Plain Text
MVP 没有外部推送渠道（不做邮件 / 短信 / IM）
        ↓
后端扫描并"触发"提醒
        ↓
这个触发不会真正通知到任何人
        ↓
那这次扫描没有意义，还多一个后台调度器
```

改成由客户端驱动：

```Plain Text
打开 App
    ↓
查询：status = 'PENDING' 且 remind_at <= now
    ↓
展示（这就是启动补偿）

App 前台运行时
    ↓
每 30~60 秒查询一次

Tauri 装好后
    ↓
本地通知由客户端调度
```

查询 SQL：

```SQL
SELECT *
FROM reminder
WHERE status = 'PENDING'
  AND remind_at <= CURRENT_TIMESTAMP
ORDER BY remind_at
LIMIT 100;
```

然后：

```Plain Text
PENDING
 ↓
已到期（remind_at <= now）→ 展示
```

状态（V1\.3）：

```Plain Text
PENDING → DISMISSED
PENDING → CANCELLED

启动补偿查询：
  PENDING AND remind_at <= now

不建模"送达"：
  服务器触发 ≠ 通知送达 ≠ 用户看到 ≠ 用户处理
```

---

# Reminder 通知

Web：

```Plain Text
Web Notification
```

Tauri：

```Plain Text
macOS Native Notification
```

提醒必须闭环，不能依赖"用户正好开着 App"：

```Plain Text
通道一  应用内提醒
通道二  桌面 / 浏览器通知
通道三  启动补偿（关键）
        打开 App 时，把已到期但未送达的提醒
        聚合成清单，展示在早晨简报顶部
```

MVP 不承诺"App 关闭时的后台推送"。

如果要实时推送，可以先：

```Plain Text
前端每 30 秒轮询
```

因此"不漏跟"的主保障是：

```Plain Text
启动补偿 + 早晨简报
```

---

# Tauri 能力

Tauri 只负责桌面能力。

## MVP

```Plain Text
Global Shortcut
Notification
Tray
Window
```

---

# 全局快捷键

默认：

```Plain Text
Option + Space
```

触发：

```Plain Text
Quick Capture
```

用户在任何应用中：

```Plain Text
⌥ Space
```

出现：

```Plain Text
┌────────────────────────────┐
│ 快速记录                   │
│                            │
│ 跟进厂商接口问题           │
│                            │
│ Enter 保存                 │
└────────────────────────────┘
```

---

# Tauri 与后端关系

不要让 Tauri 自己操作数据库。

错误：

```Plain Text
Tauri
 ↓
SQLite
```

同时：

```Plain Text
Web
 ↓
PostgreSQL
```

这样会出现两套数据。

MVP：

```Plain Text
Tauri
 ↓
Vue
 ↓
HTTP
 ↓
Spring Boot
 ↓
PostgreSQL
```

数据只有一个来源。

---

# MVP 是否支持离线

第一版：

> 不支持完整离线。
> 
> 

原因：

离线同步需要解决：

```Plain Text
数据冲突
ID生成
版本号
同步队列
删除同步
Markdown冲突
```

这些会显著扩大 MVP。

但是前端可以缓存：

```Plain Text
最近项目
最近事项
用户设置
```

作为体验优化。

## 例外：捕获队列（V1\.3\.4）

```Plain Text
我们不做：离线编辑、双向同步、冲突合并
我们做：  捕获队列（只新增、不修改）
```

设计意图：

```Plain Text
"5 秒记一句"是入口 KPI
断网时如果直接失败，想法就没了

所以：
  输入 → 先写本地队列（IndexedDB / localStorage）
       → 尝试提交
       → 失败/离线时保留，界面显示"待同步"
       → 联网后自动提交，成功则从队列移除
```

为什么它不算"离线同步"：

```Plain Text
只新增，不修改已有数据
  所以不需要：冲突解决 / 版本合并 / 删除同步 / 离线 ID

约束
  提交时禁用按钮，防重复提交
  队列条数与同步状态必须对用户可见
```

## 手机端：只读 + 速记（V1\.3\.4）

```Plain Text
手机端能做
  打开 Today（只读）
  快速记录一句
  查看项目现状（Context 只读）

手机端不做
  整理 Inbox / 编辑事项 / 写日报 / 管理项目
```

技术含义：

```Plain Text
响应式布局从 1280px 下探到约 375px
但只覆盖上述三个界面
桌面端仍是主要工作界面
```

---

# 用户身份

MVP 定位是个人工作台，但系统形态是 Web \+ Tauri \+ 服务端，
所以必须有**最小身份模型**，否则部署后谁都能读写。

定位（V1\.3）：

```Plain Text
单用户自部署
  一个 WorkOS 实例 = 一个用户
```

做法：

```Plain Text
必须有
  本机访问     免登录（本地回环地址）
  非本机访问   必须登录
  登录方式     用户名 + 密码（或一次性令牌）
  会话         Cookie / Token，服务端可撤销

不做
  注册流程 / 多用户
  角色 / 权限 / RBAC
  团队 / 组织 / 租户
  OAuth / SSO
```

数据结构：

```Plain Text
account
------------------------
id
username
password_hash
created_at
updated_at
```

产品约束（设计意图）：

```Plain Text
认证不能成为打开工作台的单点故障

个人工具最糟的体验是：
  "登录坏了，我打不开自己的工作计划"

因此：
  本机访问免登录
  登录凭证可重置（有文档化步骤）
  不做强制改密 / 过期 / 验证码等企业策略
```

---

# 为什么不一开始做用户体系

因为真正验证的是：

> WorkOS 的工作模型是否有价值。
> 
> 

不是：

> 用户注册系统是否完整。
> 
> 

所以 MVP 只做：

```Plain Text
单用户 + 最小认证
```

未来 V2 再扩展：

```Plain Text
User / Organization / Role / Permission
多设备会话管理
```

---

# Docker Compose

备份（V1\.3）：

```Plain Text
deploy/backup/backup.sh
  每日 pg_dump 一次
  写入与数据库不同的存储位置
  保留最近 14 份

验收标准是"能恢复"，不是"执行了备份"
  新库能启动 / 条数一致 / Item 与 Daily 可读
```

导出（V1\.3）：

```Plain Text
GET /api/export?format=markdown
GET /api/export?format=json

Markdown 按项目分目录，人可读
```

```YAML
services:

  workos-server:
    build:
      context: ./workos-server
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/workos
      SPRING_DATASOURCE_USERNAME: workos
      SPRING_DATASOURCE_PASSWORD: workos
    depends_on:
      - postgres

  postgres:
    image: postgres
    environment:
      POSTGRES_DB: workos
      POSTGRES_USER: workos
      POSTGRES_PASSWORD: workos
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

说明（V1\.3）：

> 已经移除 redis 服务与 `SPRING_DATA_REDIS_HOST`。
> Search 用 pg_trgm，会话用本地单用户身份，
> Reminder 由客户端驱动（V1\.3\.3 取消后端调度），
> 没有需要缓存的场景。

---

# 配置管理

Spring Boot：

```YAML
spring:

  datasource:
    url: jdbc:postgresql://localhost:5432/workos
    username: workos
    password: workos

  data:
    redis:
      host: localhost
      port: 6379

  jackson:
    time-zone: Asia/Shanghai
```

---

# 时间处理原则

这是 WorkOS 很重要的一点。

数据库统一：

```Plain Text
UTC
```

前端：

```Plain Text
用户本地时区
```

Java：

```Java
Instant
OffsetDateTime
LocalDate
```

推荐：

```Plain Text
created_at       Instant
updated_at       Instant
remind_at        Instant
occurred_at      Instant
due_at           Instant
next_follow_up_at Instant

daily_date       LocalDate
work_date        LocalDate
planned_date     LocalDate
```

不要把：

```Plain Text
2026-09-21 10:00
```

这种值全部当作 LocalDateTime 存储。

用户时区由 `settings.timezone` 提供，是"今天"的唯一真源：

```Plain Text
Today / 逾期 / 待跟进 / work_date
         ↓
全部按 settings.timezone 计算
         ↓
不要直接使用服务器 UTC 日期
```

时区初始值（V1\.3\.3）：

```Plain Text
❌ 产品默认写死 Asia/Shanghai
   换一台在美国的机器、或浏览器在别的时区
   → 服务器认为今天是 A 日，浏览器认为今天是 B 日
   → Today / 日报 / work_date 全部错位

✅ 首次启动读取系统 / 浏览器时区 → 写进 settings.timezone → 用户可改

我们的用户主要在国内，结果是"自然得到 Asia/Shanghai"，
但"默认值"应该由环境决定，而不是由产品拍定。
```

并引入日界偏移：

```Plain Text
settings.day_boundary_hour

当前时间 < day_boundary_hour
        ↓
"今天" = 前一天
```

V1\.3\.2 修正：

```Plain Text
MVP 默认 day_boundary_hour = 0（自然日）
字段保留，但 MVP 不开放给用户修改，也不进 Settings 界面

原因
  这条规则贯穿 Today / Daily / work_date / 统计
  一旦启用就要处理 23:59 / 00:30 / 时区切换 / 夏令时
  而"凌晨加班要算昨天"这个习惯还没有真实用户验证

  不为一个未验证的使用习惯，给整个系统加时间复杂度
```

Settings 瘦身（V1\.3\.2）：

```Plain Text
MVP 界面上只保留 4 项
  时区
  提醒开关
  收工复盘时间
  快速记录快捷键

其余字段保留默认值，不进界面
  date_format      默认 YYYY-MM-DD
  week_start       默认 MONDAY
  day_boundary_hour 默认 0

原因
  这 3 项目前核心流程几乎没有依赖
  让用户配置它们 = 增加"系统管理员"味道
```

---

# API 返回格式

统一：

```JSON
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误：

```JSON
{
  "code": 40001,
  "message": "事项不存在",
  "data": null
}
```

---

# DTO 分层

不要直接把 Entity 暴露给前端。

例如：

```Plain Text
ItemEntity
   ↓
ItemCreateRequest
ItemUpdateRequest
   ↓
ItemService
   ↓
ItemResponse
```

---

# Mapper

使用 MyBatis\-Plus：

```Java
@Mapper
public interface ItemMapper
        extends BaseMapper<ItemEntity> {
}
```

但业务查询不要全部依赖：

```Java
lambdaQuery()
```

复杂查询建议：

```Plain Text
Mapper XML
```

保证 SQL 可控。

---

# Service 层

例如：

```Java
public interface ItemService {

    Long create(ItemCreateRequest request);

    ItemResponse get(Long id);

    void update(Long id, ItemUpdateRequest request);

    void complete(Long id);

    void waiting(Long id);

    void resume(Long id);

    void cancel(Long id);
}
```

---

# 状态机不要散落 Controller

不要：

```Java
if (item.getStatus() == ...)
```

到处判断。

建议：

```Plain Text
ItemStateMachine
```

例如：

```Java
itemStateMachine.start(item);
itemStateMachine.waiting(item);
itemStateMachine.complete(item);
```

统一管理合法状态转换。

---

# Item 状态转换规则

---

# 业务事件

MVP 可以在内部设计事件模型。

例如：

```Plain Text
ItemCompleted
ItemWaiting
FollowUpCreated
ReminderTriggered
WorkLogCreated
```

第一版不需要 MQ。

使用：

```Plain Text
Spring ApplicationEvent
```

即可。

---

# Item 完成事件

```Plain Text
完成 Item
 ↓
ItemCompletedEvent
 ↓
WorkLogService
 ↓
生成完成记录
```

未来：

```Plain Text
ItemCompleted
 ↓
Daily
 ↓
AI
 ↓
周报
```

这样可以提前为后续扩展留接口。

---

# WorkLog 自动记录原则

不是所有状态变化都必须自动产生 WorkLog。

MVP 自动记录：

```Plain Text
TODO → DOING
TODO/DOING → DONE
```

例如：

```Plain Text
09:20
[系统]
开始处理：工单接口

14:30
[系统]
完成接口开发
```

不再自动记录 Item Created 与进入 Waiting：

> 日报的价值来自"我做了什么"，不是"系统发生了几次状态变更"。
> 自动记录过多会污染日报初稿。

用户可以手工增加：

```Plain Text
16:30
发现厂商接口字段异常
```

---

# 项目 Context 更新

项目 Context 不要求每次修改都产生版本。

MVP：

```Plain Text
稳定信息
  goal
  key_people
  key_links

动态状态
  focus
  blocker
  next_action
  progress_note     原 current_progress（V1.3.2：可选，系统给默认文本）

可选补充
  notes_markdown
```

直接保存当前状态。

Context 保鲜（V1\.1 新增）：

```Plain Text
project_context.updated_at 距今 > 14 天
        ↓
Project 卡片 / 详情页显示"上下文已 N 天未更新"

项目中的 P0/P1 或 DOING 事项被完成
        ↓
轻提示"更新当前重点 / 下一步？"
        ↓
点击进入 Context 编辑，并预填已完成事项标题
```

理由：

> Context 的价值完全依赖它是"当前的"。
> 没有保鲜机制，几周后它就会过期，
> "10 秒恢复上下文"随之失效。

未来 V2 再增加：

```Plain Text
ProjectContextHistory
```

记录：

```Plain Text
09-20
当前重点：接口开发

09-21
当前重点：测试联调
```

---

# Project Context 与 Item 的关系

这是重要设计：

Context 是：

> 当前状态
> 
> 

Item 是：

> 具体事项
> 
> 

例如：

```Plain Text
Project Context

当前重点：
工单自动分配

下一步：
完成联调
```

对应：

```Plain Text
Item

工单自动分配
状态：DOING

工单接口联调
状态：TODO
```

Context 不替代 Task。

---

# Today 的排序规则

Today 使用 6 个互斥分区，"归属优先级即分区顺序"，
一条事项只落入第一个命中的分区，因此各分区不重复计数。

```Plain Text
① 今日计划
     planned_date = 今天 AND status NOT IN (DONE, CANCELLED)

② 逾期
     due_at < 今天起始
     AND status IN (TODO, DOING)
     AND planned_date IS NULL

③ 今日到期
     due_at 在 [今天起始, 明天起始)
     AND status NOT IN (DONE, CANCELLED, WAITING)
     AND planned_date IS NULL

④ 待跟进
     next_follow_up_at <= now
     AND status NOT IN (DONE, CANCELLED)
     AND planned_date IS NULL

⑤ 等待中
     status = WAITING
     AND (next_follow_up_at IS NULL OR next_follow_up_at > now)
     AND planned_date IS NULL

⑥ 待安排
     未计划 + 未完成 + 非 WAITING
     AND priority IN (P0, P1)
     显示上限 5 条 + "查看全部"
```

它不是列表，而是决策队列（V1\.3\.3）：

```Plain Text
每条待安排事项直接给三个动作

[今天做]     → planned_date = 今天，进 ① 今日计划
[排到明天]   → planned_date = 明天
[暂不安排]   → priority 显式降为 P2

为什么需要
  用户有 6 条 P1 未安排时，
  只看到"待安排 6"仍然不知道该做哪个
  必须让"看"直接变成"决定"

为什么"暂不安排"是降优先级而不是隐藏
  隐藏 N 天 = 又一条系统推断规则，且用户会怀疑丢数据
  降为 P2 = 用户看得见后果，符合原则十一
```

改名与规则简化（V1\.3）：

```Plain Text
⑥ 建议 → ⑥ 建议处理 → ⑥ 待安排

定位：
  它不是"推荐"（系统替我决定做什么）
  也不是"过滤"（把所有重要的都列出来）
  它是"捕漏"：把我忘了安排、但其实重要的事捞回来

UI 文案：
  "这条还没安排，要不要放到今天？"

P0 / P1 都无条件出现（V1.3.1 修正）：
  P0 = 用户显式标"紧急"
  P1 = 用户显式标"高"

  入选只看用户的显式标记，
  "due 临近 / 最近有活动"这类推断只用于排序或直接删除。

  为什么修正：
    V1.3 曾写成"P0 或 P1 且 due 临近 ……"
    结果一条"P1 + 无日期"的事项任何条件都不满足，
    在 Today 上完全消失，只能去 Items 里翻
    用户会困惑："我明明标了高优先级，怎么首页没有？"

已删除的规则（V1.3）：
  近 3 天有 activity 或 work_log

  原因：
    "我昨天看了一眼"不等于"我今天要做"
    用"最近看过"当依据，是最容易让用户觉得
    "系统在猜我"的规则
    一旦用户开始怀疑 Today 为什么这么排，
    整个首页的信任就没有了

设计原则：
  Today 必须是可解释的，而不是神秘的
  每条事项出现在 Today，用户都要能一句话说出为什么
```

统计层与展示层分离（V1\.2）：

```Plain Text
统计层（后端）
  分区互斥，一条事项只归属一个主分区
  → 简报数字可直接相加
  → 不需要新增接口字段

展示层（前端）
  允许叠加"次级状态标签"，最多 2 个
  → 由已有字段推导，不由后端返回

标签集合（固定）：
  今日计划
  逾期 N 天
  今天到期
  跟进 HH:mm
  等待 XX
  对方逾期 N 天

取值优先级（最多 2 个，V1.3.1）：
  等待 XX > 跟进 > 对方逾期 > 逾期 > 今天到期 > 今日计划
```

今日计划的容量（V1\.3\.2 修正）：

```Plain Text
旧版：软上限 3 件 + 硬上限 5 件
新版：建议 3 件；超过 3 件轻提示；无硬上限

理由
  "3 / 5" 是设计者的偏好，不是用户的事实
  开发一天可能不止 5 件，PM 一天跟 8 件事也可能合理

轻提示文案
  "今天已经安排 4 件了，确定继续吗？"

持续表达的原则
  "Today 是今天真正准备处理的事，不是所有待办"
```

排序（分区内部）：

```Plain Text
今日计划    plan_rank ASC
逾期        due_at ASC
今日到期    priority ASC, due_at ASC
待跟进      next_follow_up_at ASC
等待中      next_follow_up_at ASC NULLS LAST
待安排      priority ASC, due_at ASC NULLS LAST
```

---

# 早晨简报的构成（V1\.2；V1\.3\.2 并入 Today 顶部）

简报分两段，回答的是"今天需要我做什么决定"，而不只是统计：

```Plain Text
① 需要你决定
  N 项计划未完成（含老化提示） [结转] [改期] [转 Waiting] [取消]
  有 N 个跟进已逾期           [现在跟进]
  有 N 条 Inbox 未整理        [整理]
  有 N 个等待已超对方承诺      [查看]

② 今日概览
  今日计划 x/y
  逾期 / 今日到期 / 待跟进 / 等待中
  Inbox 待整理
```

数据来源：

```Plain Text
需要你决定
  未完成计划：planned_date < 今天 AND status NOT IN (DONE, CANCELLED)
  next_follow_up_at <= now
  status = INBOX 计数
  status = WAITING AND waiting_expected_at < now

今日概览
  Today 六个分区的去重计数
```

"未完成计划"的口径修正（V1\.3\.1）：

```Plain Text
❌ 只查"昨天"
    9/22 计划 3 件 → 9/23 没打开 → 9/24 打开
    按"昨天"口径什么都不提示，3 件欠账永久消失

✅ 查所有欠账
    planned_date < 今天 AND 未完成
    并显示老化程度：
      "2 项未完成（其中 1 条是 3 天前计划的）"
```

V1\.3\.2 补充两条：

```Plain Text
一、简报不是独立模块，而是 Today 顶部的一种状态
    首次打开时显示"需要你决定"
    之后收起，只留 Today 的三块内容
    用户不需要理解"简报"和"Today"是两个东西

二、Today 在界面上归成三块
    今天做什么    → ① 今日计划
    今天别忘了    → ② 逾期 ③ 今日到期 ④ 待跟进
    我在等谁 / 还没安排 → ⑤ 等待中 + ⑥ 待安排

    分区规则不变，只是分组展示
```

V1\.3\.3 调整第三块的命名与顺序：

```Plain Text
"等待中"放在"待安排"之前，不并入"今天别忘了"

原因
  对 PM / 产品这类用户，
  "我在等谁、等了几天、对方该不该给了"是每天必看的信息，
  也是 WorkOS 相对普通待办的头号差异点
  把它降级到"还没安排"那一堆里，会削弱核心价值
```

---

# Overdue 判断

如果：

```Plain Text
due_at < 今天起始
AND status IN (TODO, DOING)
```

则：

```Plain Text
OVERDUE
```

注意：

> OVERDUE 不应该成为数据库永久状态。
> 
> 

它是计算状态。

为什么只统计 TODO / DOING：

> 已经交给别人的事项，用"自己的截止日期"判逾期会误导用户，
> 应该用"待跟进"来表达。

---

# Follow\-up 到期判断

如果：

```Plain Text
next_follow_up_at <= now
```

且 Item 未完成：

```Plain Text
FOLLOW_UP_DUE
```

页面进入：

```Plain Text
待跟进
```

---

# Waiting 超时判断

如果：

```Plain Text
status = WAITING
AND due_date < today
```

显示：

```Plain Text
Waiting Overdue
```

例如：

```Plain Text
⏳ 厂商问题
预计回复：昨天
已经等待：3天
```

---

# Search 语法 V1

MVP 支持普通关键词：

```Plain Text
工单
```

V1\.1 可以增加：

```Plain Text
project:项管系统
status:waiting
priority:P1
tag:bug
```

不在第一版强制实现高级搜索语法。

---

# 前端状态管理

Pinia Store：

```Plain Text
stores
│
├── app.ts
├── project.ts
├── item.ts
├── today.ts
├── daily.ts
├── reminder.ts
└── search.ts
```

不要创建：

```Plain Text
每一个组件一个 Store
```

Store 按业务领域划分。

---

# API Client

```Plain Text
api
│
├── request.ts
├── project.ts
├── item.ts
├── worklog.ts
├── daily.ts
├── reminder.ts
└── search.ts
```

例如：

```TypeScript
export function createItem(
  request: ItemCreateRequest
) {
  return request.post<ItemResponse>(
    '/api/items',
    request
  )
}
```

---

# TypeScript 类型

前后端不要依赖手工复制类型。

后续可以通过：

```Plain Text
OpenAPI
 ↓
TypeScript Generator
 ↓
自动生成 API Types
```

MVP 初期可以手写。

---

# 测试策略

## Backend

```Plain Text
JUnit 5
Mockito
Spring Boot Test
Testcontainers
```

特别推荐：

> PostgreSQL 使用 Testcontainers。
> 
> 

不要用 H2 模拟 PostgreSQL。

因为 WorkOS 使用：

- PostgreSQL Date

- FTS

- Index

- JSON

- PostgreSQL 特性

H2 容易产生环境差异。

---

# Backend 测试层级

```Plain Text
Unit Test
    ↓
Service Test
    ↓
Repository Test
    ↓
Controller Test
    ↓
Integration Test
```

MVP 至少覆盖：

```Plain Text
Item 状态机
Item CRUD
Project CRUD
Follow-up
Waiting
Reminder
Daily Generate
Search
```

---

# 前端测试

MVP：

```Plain Text
Vitest
Playwright
```

重点测试：

```Plain Text
快速创建
Item 状态转换
项目切换
日报编辑
搜索
快捷操作
```

---

# 核心验收场景

## 场景一：快速记录

```Plain Text
⌥ Space
→ 输入
→ Enter
```

要求：

> 5 秒内完成。
> 
> 

---

## 场景二：任务

```Plain Text
创建 Task
→ 设置 P1
→ 设置今天
→ Doing
→ Done
```

要求：

状态、完成时间、工作记录正确。

---

## 场景三：Waiting

```Plain Text
Task
→ Waiting
→ 设置厂商
→ 设置 09-23
```

要求：

09\-23 出现在 Follow\-up / Waiting 中。

---

## 场景四：日报

```Plain Text
完成三个 Task
→ Work Log
→ Generate Daily
```

要求：

自动生成日报草稿。

---

## 场景五：上下文

```Plain Text
进入 Project
→ 查看 Context
→ 查看当前任务
→ 查看 Waiting
→ 查看最近 WorkLog
```

目标：

用户 10 秒内理解项目当前状态。

---

# 开发阶段拆分

## Sprint 1：基础框架

### 后端

```Plain Text
Spring Boot
PostgreSQL
MyBatis-Plus
统一响应
异常处理
Flyway
```

### 前端

```Plain Text
Vue
Vite
TypeScript
Ant Design Vue
Router
Pinia
Layout
```

---

# Sprint 2：Project \+ Item

完成：

```Plain Text
Project
Item
Status
Priority
Tag
```

页面：

```Plain Text
Projects
Items
Item Drawer
```

---

# Sprint 3：Today \+ Inbox

完成：

```Plain Text
Today
Inbox
Quick Capture
Context
```

这是 MVP 第一条真正可用链路。

---

# Sprint 4：Follow\-up \+ Waiting

完成：

```Plain Text
Follow-up
Waiting
Due
Overdue
```

---

# Sprint 5：WorkLog \+ Daily

完成：

```Plain Text
WorkLog
Daily
Markdown
Preview
Outline
```

---

# Sprint 6：Reminder \+ Calendar

完成：

```Plain Text
Reminder
Calendar
Today aggregation
```

---

# Sprint 7：Search

完成：

```Plain Text
Global Search
Project Search
Item Search
Daily Search
WorkLog Search
```

---

# Sprint 8：Tauri

完成：

```Plain Text
Tauri
Global Shortcut
Notification
Tray
Mac Build
```

---

# MVP 研发优先级

V1\.3 补充的三项实现要点：

```Plain Text
一、首次使用种子数据
  空库启动时**不写任何数据**，先显示引导页：
    [ 用示例体验一下 ]  → 写入示例项目 + 3 条示例事项 + 示例 Context
    [ 直接开始工作 ]    → 干净的空白工作区

  V1.3.3 修正：示例数据不能默认混进真实工作区
    第一次打开就看到"假数据"，用户会分不清哪个是自己的
    对个人工具来说，"混着假数据"比"空着"更糟
  （产品依据见文档 1 第 6.17 与 §37.13）

  三条实现细节（V1.3.1）
    (1) 相对日期生成
        示例事项的 due_at / planned_date 以"今天"为基准
        否则用户第二天打开会看到一堆"逾期"的示例
    (2) 可识别 + 可清除
        示例项目标 code = DEMO
        提供"清空示例数据"按钮
    (3) 埋点排除
        K1 / K4 的指标样本必须排除 demo 数据

二、Inbox 治理
  提供查询：
    Inbox 待整理数量
    Inbox 最老条目的天数
  Inbox 顶部提供"整理最老的一条"入口（V1.3.2）
  Inbox 顶部提供"先都收下"入口（V1.3.4）
  超过阈值时进入"需要你决定"
  目的：Inbox 不治理就会变成第二个垃圾场

  "先都收下"的语义
    把当前 Inbox 的项一次性设为 TODO / P2 / 无项目
    整理完成条件随之放宽：
      status != INBOX（不再要求 projectId != null）

  背景
    Inbox 堆 30 条时，逐条整理要 15 分钟、60~90 次判断
    只给"每条都要决定"的出口，用户就会直接放弃

三、带"人"的文案（V1.3.2 修正）
  Follow-up 的提醒文案要带上具体的人，但不替用户断言承诺：
    "该催 {waiting_for} 了：{title}"
    "{related_person} · {due_at} 前 · {title}"
  禁止："你答应 {人} ……"
  目的：有具体的人 + 时间，比抽象事项名更能推动行动

四、默认行为矩阵
  以文档 1「默认行为矩阵」为准
  研发直接按那张表写默认值
  任何默认行为的改动，都要回到那张表更新

五、4 个核心指标的埋点
  以文档 1 的 K1~K4 为准：
    捕获延迟（P50/P95）
    漏跟率
    日报准备时间（排除中途离开）

  K3（上下文恢复时间）**不做埋点**：
    它是可用性测试指标
    验收方式：选 5~10 个真实项目，让用户离开 ≥24 小时后重新打开，
    要求答出"当前重点 / 阻塞 / 下一步 / 最近工作 / 在等谁"，
    记录完成时间与答对率（≤10 秒且基本答对为通过）

六、用户维护成本预算（V1.3.3）
  以文档 1「用户维护成本预算」为准
    记录 ≤5s / 整理 ≤30s / 完成 ≈0 / 等待 ≤10s
    跟进 ≤5s / 记一句 ≤15s / 日报 ≤3min

  任何新功能上线前，先估它给用户加了多少秒
  超过上限 → 要么改交互，要么砍掉

七、原则十一（显式优先于推断）
  入选只能用显式事实：
    用户设了 planned_date 就是"今天做"
    用户设了 P1 就是"重要"
    用户进入 WAITING 就是"在等别人"
  推断只允许用于"排序"，不允许用于"入选"
```

八、Item 列表返回"最近一条记录"（V1\.3\.4）

```Plain Text
为什么
  用户做到一半被叫走，回来后要知道"我刚才做到哪了"
  Item 没有进度字段，但 Drawer 里的记录/活动已经有这条信息
  → 把它提到卡片上，不加字段

接口影响
  GET /items 与 /today 返回的每个 Item 需附带：
    lastLogContent    最近一条工作记录内容（可为空）
    lastLogAt         最近一条记录时间
    lastActivityType  没有记录时，回退到最近一条 Activity 的类型

展示规则
  有工作记录 → 显示工作记录
  否则       → 显示最近一条 Activity 摘要（如"状态 TODO → DOING"）
  都没有     → 该行不显示
```

九、Daily 增加"本周"聚合（V1\.3\.4）

```Plain Text
GET /dailies?weekStart=2026-09-21
  返回本周每天的：
    完成事项（按项目分组）
    工作记录
    跟进与等待的变化

只读聚合，不做周报生成
```

十、删除语义（V1\.3\.4）

```Plain Text
DELETE /items/{id}
  仅允许：status = INBOX 且从未产生记录
  其余情况：返回 ITEM_DELETE_NOT_ALLOWED
           引导用户走"取消"

界面文案
  已整理过 → 菜单里叫"取消事项"，并说明"会留在所有记录里"
  未整理   → 菜单里叫"删除这条记录"
```

十一、「继续上次」（V1\.3\.5）

```Plain Text
存哪里
  本地（localStorage）：lastOpenedItemId / lastOpenedAt
  不上服务端 —— 它是会话状态，不是业务事实
  放服务端会引出"多设备以哪个为准"，MVP 不值得回答

显示条件
  距上次打开 ≥ 30 分钟
  且该事项未完成 / 未取消

行为
  点击 → 打开该事项 Drawer（停在描述区）
  项目级操作 → 打开项目页
```

十二、Item 卡片上的"还剩 N 步"（V1\.3\.5）

```Plain Text
为什么
  研发被打断后要回答"我写到哪了"
  description 本来就支持 Markdown checklist
  → 复用已有数据，不加字段、不做子任务

实现
  前端解析 description 中未勾选的 checklist 项数
  > 0 时在卡片显示"还剩 N 步"

接口影响（重要）
  GET /items 与 /today 返回的每个 Item 必须包含 description
  否则前端算不出未勾选数量

  （与 V1.3.4 的 lastLog* 字段一起加，避免两次改契约）
```

十三、记录占位文案（V1\.3\.5）

```Plain Text
❌ "今天做了什么？"
✅ "做了什么 / 为什么这么决定？"

目的
  用户最容易漏记的是"当时为什么这么定"
  而它正是三天后最容易被追问的东西
  改一个字符串，比新增"决策链"字段便宜得多
```

最终建议：

```Plain Text
P0
│
├── Project
├── Item
├── Inbox
├── Today
├── Task
├── Follow-up
├── Waiting
├── WorkLog
└── Daily

P1
│
├── Markdown
├── Outline
├── Calendar
├── Search
└── Reminder

P2
│
├── Tauri
├── Global Shortcut
└── Native Notification
```

如果研发资源有限：

> **宁愿先做 Web PWA，也不要为了桌面版阻塞核心业务。**
> 
> 

---

# MVP 第一可运行版本

第一阶段甚至可以做到：

```Plain Text
Vue
   ↓
Spring Boot
   ↓
PostgreSQL
```

实现：

```Plain Text
Today
Inbox
Project
Item
Task
Follow-up
Waiting
WorkLog
Daily
```

用户已经可以真正使用。

然后再：

```Plain Text
Markdown
Reminder
Search
Tauri
```

逐步增强。

---

# V2 数据模型预留

虽然 MVP 不开发，但设计上预留：

```Plain Text
user
organization
project_member

item_relation

project_context_history

daily_relation

attachment

comment

notification

integration
```

---

# V2 AI 接入预留

未来：

```Plain Text
AI
 ↓
WorkOS Domain Service
```

AI 不直接操作数据库。

例如：

```Plain Text
POST /api/ai/capture
```

输入：

```Plain Text
“明天跟进一下厂商的接口问题”
```

AI 输出结构化 Item：

```JSON
{
  "type": "TASK",
  "title": "跟进厂商接口问题",
  "nextFollowUpAt": "2026-09-22"
}
```

再由普通 ItemService 保存。

原则：

> **AI 是输入方式，不是核心业务模型。**
> 
> 

---

# 最终架构

```Plain Text
┌─────────────────────┐
                         │       User          │
                         └──────────┬──────────┘
                                    │
                   ┌────────────────┴────────────────┐
                   │                                 │
              Browser                            macOS
                   │                                 │
              Vue 3 + Vite                     Tauri + Vue
                   │                                 │
                   └────────────────┬────────────────┘
                                    │
                                 REST API
                                    │
                      ┌─────────────▼─────────────┐
                      │      Spring Boot 3        │
                      │          Java 21          │
                      ├───────────────────────────┤
                      │ Project                   │
                      │ Item                      │
                      │ Follow-up                 │
                      │ Waiting                   │
                      │ WorkLog                   │
                      │ Daily                     │
                      │ Reminder                  │
                      │ Search                    │
                      └─────────────┬─────────────┘
                                    │
                         ┌──────────┴──────────┐
                         │                     │
                    PostgreSQL               （无其他有状态组件）
                         │
                         │
                 ┌───────▼────────┐
                 │  WorkOS Data   │
                 ├────────────────┤
                 │ Project        │
                 │ Item           │
                 │ WorkLog        │
                 │ Daily          │
                 │ Reminder       │
                 │ Tag            │
                 └────────────────┘
```

---

# 最终 MVP 判断标准

WorkOS MVP 不以“功能数量多”为成功标准。

而是验证以下工作闭环：

```Plain Text
我突然想到一件事情
        ↓
⌥ Space
        ↓
5 秒记录
        ↓
Inbox
        ↓
整理成 Item
        ↓
归属项目
        ↓
设置优先级/日期
        ↓
开始处理
        ↓
如果交给别人
        ↓
Waiting
        ↓
到期
        ↓
Follow-up
        ↓
继续处理
        ↓
Done
        ↓
WorkLog（可选：用户想补一句时才记）
        ↓
Daily
        ↓
项目历史
```

注意（V1\.3\.2）：

```Plain Text
不要把 WorkLog 画成必经步骤
  完成 Item 本身就是事实
  WorkLog 是增强项

否则等于要求用户每件事都来记一句，
而真实结果是不记——最后日报只剩"完成了哪些事项"
```

最终用户每天打开 WorkOS，只需要看到：

```Plain Text
┌─────────────────────────────────────────────┐
│                  TODAY                      │
├─────────────────────────────────────────────┤
│                                             │
│ 🔥 现在最重要                               │
│                                             │
│ 🔴 工单自动分配                             │
│ 🟠 生产问题                                 │
│                                             │
│ 👀 今天需要跟进                             │
│                                             │
│ → 厂商接口问题                              │
│ → 产品需求确认                              │
│ → 测试问题                                  │
│                                             │
│ ⏳ 我正在等待                               │
│                                             │
│ ⏳ 厂商解决方案                              │
│ ⏳ 研发接口                                  │
│                                             │
│ ✓ 今天完成                                  │
│                                             │
│ ✓ 用户中心接口                              │
│ ✓ 客服岗位功能                              │
│                                             │
│ 📝 工作记录                                 │
│                                             │
│ 09:20 完成接口开发                          │
│ 10:30 与产品确认需求                        │
│ 14:20 处理厂商问题                          │
│                                             │
└─────────────────────────────────────────────┘
```

这就是 WorkOS MVP 的核心产品体验。

---

# MVP 开发结论

WorkOS 第一版最终采用：

```Plain Text
前端
Vue 3
TypeScript
Vite
Pinia
Ant Design Vue

桌面
Tauri

后端
Java 21
Spring Boot 3
MyBatis-Plus

数据库
PostgreSQL

缓存
无（MVP 不引入 Redis）

搜索
PostgreSQL pg_trgm

部署
Docker Compose
```

架构原则：

```Plain Text
单体
模块化
API优先
Web优先
Desktop渐进增强
PostgreSQL单数据源
Markdown保存原始内容
结构化字段负责管理
```

核心业务模型：

```Plain Text
Project
   ↓
Context
   ↓
Item
 ┌─┼───────────┐
Task FollowUp Waiting
 └─┼───────────┘
   ↓
WorkLog
   ↓
Daily
   ↓
历史工作资产
```

这套模型能够支持 MVP，同时给后面的：

```Plain Text
AI
团队协作
厂商管理
PMO
第三方集成
项目复盘
```

留下扩展空间。

### 关于 PostgreSQL，再补充一个关键判断

你说的“有没有**开源私有化部署版本**”，这里其实不用找一个所谓的“PostgreSQL 私有化版本”——**PostgreSQL 官方版本身就是那个版本**。官方提供源码和各平台安装包，可以直接自行部署；许可证也是宽松的 PostgreSQL License。\([PostgreSQL](https://www.postgresql.org/about/licence/?utm_source=chatgpt.com)\)

因此 WorkOS 后面完全可以做到：

```Plain Text
个人电脑
    ↓
Docker Compose
    ↓
PostgreSQL

公司内网
    ↓
Docker / K8s
    ↓
PostgreSQL

个人云服务器
    ↓
Docker Compose
    ↓
PostgreSQL
```

不需要依赖某个云厂商的 PostgreSQL 服务。

### 关于 Vue，我建议正式定下来

我会把之前 PRD 中的 React 修改成：

> **Vue 3 \+ TypeScript \+ Vite**
> 
> 

而且 **Tauri \+ Vue** 是官方支持的组合，Tauri 官方的创建工具直接提供 Vue 模板。\([Tauri](https://tauri.app/start/frontend/?utm_source=chatgpt.com)\)

所以你作为 Java 后端开发，可以把整个项目控制在自己比较熟悉的工程模式里：

```Plain Text
WorkOS
                │
       ┌────────┴────────┐
       │                 │
   workos-web        workos-server
       │                 │
 Vue3/TS/Vite       Java 21
       │            Spring Boot 3
       │            MyBatis-Plus
       │                 │
       └──── REST ───────┤
                         │
                     PostgreSQL
```

**我尤其建议现在先不要引入 Redis、OpenSearch、MQ。** WorkOS 是你自己的工作工具，MVP 最大风险不是性能，而是**产品模型设计错了以后返工**。先把 `Project → Context → Item → Waiting/Follow-up → WorkLog → Daily` 这个闭环真正跑通，比堆技术组件重要得多。

下一步最值得继续做的是把这份研发设计再往下压一层，直接形成 **《WorkOS MVP 数据库设计 \+ OpenAPI 接口规范 \+ Vue 页面原型/组件设计》**，尤其把 **Today、Inbox、Project Context、Item Drawer、Daily 编辑器** 五个核心页面做到字段级和交互级，这样基本就可以正式开工了。
