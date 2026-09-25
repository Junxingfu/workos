可以。你给的这份前端架构说明已经足够作为**工程约束基线**，所以这次我不再重新设计技术栈，而是把它上升成一套 **Codex 可执行的 UI/UX 设计约束体系**。

有一个重要原则我先定下来：

> **这些文档不是为了让 Codex“自由发挥设计”，而是为了限制 Codex 的自由发挥。**
>
> Codex 的职责是把既定产品模型实现成高质量界面，而不是重新发明 WorkOS 的产品结构、业务规则和视觉语言。

下面这套可以直接放进仓库：

```text
workos-web/
├── AGENTS.md
└── design/
    ├── DESIGN.md
    ├── UX_RULES.md
    └── VISUAL_REVIEW.md
```

其中 `Today / Inbox / Project / Item` 的页面级规则已经写进去。

---

# WorkOS Frontend Agent Instructions

> 适用范围：`workos-web/`
>
> 产品版本：MVP / PRD V1.3.5
>
> 前端：Vue 3 + TypeScript + Vite + Ant Design Vue
>
> 本文件是 Codex / Coding Agent 的最高优先级前端工作约束之一。

---

# 1. 你正在开发什么

WorkOS 不是传统 Todo List，也不是企业管理后台。

WorkOS 是：

> **一个以项目上下文为核心、以事项追踪为主线、以工作记录和日报为沉淀的个人工作操作系统。**

目标用户包括：

* 项目经理
* 产品经理
* 研发人员
* 测试人员
* 需要同时处理多个项目、多个事项、多人协作关系的技术/业务人员

用户的典型工作环境：

* 多项目并行
* 高频上下文切换
* 临时会议
* 临时需求
* 紧急问题
* 被别人临时拉通
* 等待其他人提供信息
* 持续跟进外部人员
* 工作时间被频繁打断
* 加班后需要恢复当天上下文

因此：

> **WorkOS UI 的第一目标不是“看起来漂亮”，而是降低用户恢复工作上下文的成本。**

---

# 2. 最高优先级原则

实现任何页面之前，必须遵守以下原则。

## 2.1 不重新发明产品

不要自行增加：

* 新的业务实体
* 新的一级导航
* 新的任务状态
* 新的任务类型
* 新的工作流
* 新的 Dashboard 指标
* 新的推荐机制
* 新的 AI 功能
* 新的复杂筛选体系

如果 PRD / UX_RULES 没有要求：

> **不要自行增加。**

---

# 2.2 不自行推导业务规则

前端不是业务规则事实源。

禁止在前端自行决定：

* Item 是否逾期
* Item 为什么进入 Today
* Item 是否属于 Waiting
* Waiting 已经等待多少天
* Today 的分区归属
* 状态是否允许转换
* 哪些 Item 应该被推荐
* 项目是否完成
* 日界线
* 用户“今天”是哪一天

后端返回事实。

前端负责：

```text
事实
 ↓
展示
 ↓
用户操作
 ↓
发送请求
 ↓
重新获取事实
```

---

# 2.3 不为了“视觉丰富”增加 UI

禁止为了让页面看起来丰富而增加：

* KPI 卡片
* 大数字
* 无意义统计
* 渐变背景
* 装饰性插画
* 大面积阴影
* 玻璃拟态
* 发光效果
* 3D 元素
* 无意义动画
* 彩色背景
* Emoji 作为主要 UI 图标

WorkOS 是生产力工具。

> **信息价值 > 视觉装饰。**

---

# 2.4 不使用 Generic SaaS Dashboard Pattern

禁止把 WorkOS 做成：

```text
┌────────┐ ┌────────┐ ┌────────┐
│  12    │ │  23    │ │  8     │
│ Tasks  │ │ Done   │ │ Waiting│
└────────┘ └────────┘ └────────┘

┌───────────────────────────────┐
│ Recent Activities             │
└───────────────────────────────┘
```

这是一种通用 SaaS Dashboard 模式，不符合 WorkOS。

WorkOS 首页应该回答：

```text
我今天要做什么？
什么事情不能忘？
我在等谁？
还有什么没有安排？
我上次做到哪里？
```

---

# 3. 设计参考优先级

实现 UI 时，参考顺序：

1. WorkOS PRD V1.3.5
2. `design/UX_RULES.md`
3. `design/DESIGN.md`
4. `design/VISUAL_REVIEW.md`
5. 当前已有组件和页面
6. Ant Design Vue 官方能力
7. Agent 自己的判断

发生冲突时：

> **产品需求 > UX 规则 > Design System > Agent 自由发挥**

---

# 4. 技术栈不可擅自改变

当前 MVP 固定：

```text
Vue 3
TypeScript
Vite
Vue Router
Pinia
Ant Design Vue
Axios
Dayjs
```

禁止因为视觉设计需要而引入：

* React
* Tailwind
* Element Plus
* Bootstrap
* Material UI
* UnoCSS
* CSS Framework
* 新的状态管理库
* 新的图表库

除非明确收到开发者指令。

---

# 5. 页面开发原则

每个页面必须首先回答：

```text
这个页面的唯一主要任务是什么？
```

然后按照：

```text
页面目标
 ↓
信息层级
 ↓
主要动作
 ↓
次要动作
 ↓
详情操作
```

进行设计。

不要：

```text
组件
 ↓
卡片
 ↓
卡片
 ↓
按钮
 ↓
按钮
 ↓
不知道为什么存在的统计信息
```

---

# 6. 页面密度

WorkOS 是高频工作工具。

默认页面应该：

* 紧凑
* 高信息密度
* 低装饰
* 少空白
* 清晰分组
* 快速扫描

但不能变成拥挤的后台表格。

目标：

> **用户可以快速扫描，而不是需要逐字阅读。**

---

# 7. 页面必须支持“扫描”

用户进入页面时，应该可以通过：

```text
标题
状态
优先级
项目
时间
Waiting / Follow-up
```

快速理解事项。

不要要求用户打开详情才能知道：

* 这是什么
* 属于什么项目
* 当前状态
* 是否紧急
* 是否需要跟进

---

# 8. ItemCard 约束

`ItemCard.vue` 是展示组件。

它负责：

* 标题
* 类型
* 状态
* 优先级
* 项目
* 时间
* 次级语义标签
* 基本操作入口

它不负责：

* 修改业务状态机
* 自行判断逾期
* 自行决定 Today 归属
* 自行改变业务规则

---

# 9. ItemDrawer 约束

`ItemDrawer.vue` 是主要事项操作中心。

用户点击事项后：

> **优先打开 Drawer，而不是跳转一个新的详情页面。**

Drawer 中负责：

```text
标题
基本属性
状态
项目
日期
Waiting
Follow-up
描述
Checklist（如果已有能力）
记一句
Activity
```

操作应该围绕：

> **“我现在需要对这件事做什么？”**

而不是展示所有数据库字段。

---

# 10. 状态变更

状态变更只能：

```text
POST /api/items/{id}/transitions
```

前端不得：

```text
PUT /api/items/{id}
{
  status: ...
}
```

允许的状态按钮来自后端：

```text
allowedTransitions
```

前端不得自行复制状态机。

---

# 11. 写操作后的刷新策略

MVP 不做乐观更新。

统一：

```text
用户操作
 ↓
API
 ↓
成功
 ↓
重新 GET
 ↓
重新渲染
```

不要：

```text
用户操作
 ↓
先修改本地状态
 ↓
猜测服务器结果
 ↓
再纠正
```

---

# 12. API 使用规范

所有 HTTP 请求：

```text
页面
 ↓
api/index.ts
 ↓
api/client.ts
 ↓
Axios
```

禁止：

```ts
axios.get(...)
axios.post(...)
```

直接出现在页面组件中。

---

# 13. 错误处理

统一：

```ts
fail(error)
```

禁止每个页面自行设计错误提示体系。

---

# 14. TypeScript

后端 ID 为 Long，前端统一：

```ts
id: string
```

禁止：

```ts
parseInt(id)
Number(id)
```

禁止使用数字与 ID 比较。

---

# 15. 时间

时间规则：

```text
Instant
 ↓
后端 UTC
 ↓
前端 Dayjs
 ↓
本地展示
```

绝对时间：

```ts
dayjs(iso)
```

提交：

```ts
dayjsValue.toISOString()
```

纯日期：

```text
YYYY-MM-DD
```

禁止：

```ts
iso.substring(...)
```

禁止自行判断“今天”。

---

# 16. Pinia

目前不使用 Pinia Store 保存页面数据。

不要为了“架构完整”创建：

```text
todayStore
itemStore
projectStore
inboxStore
```

除非真正出现跨页面共享状态需求。

---

# 17. 不为了复用而复用

优先级：

```text
重复出现两次
    ↓
先观察

重复出现三次且语义稳定
    ↓
考虑抽组件
```

不要创建：

```text
BaseCard
BasePanel
BaseSection
BaseContainer
BaseLayout
BaseThing
```

然后所有页面都套进去。

---

# 18. SFC 规范

固定：

```vue
<script setup lang="ts">
</script>

<template>
</template>

<style scoped>
</style>
```

---

# 19. 完成一个页面之后必须做什么

不能仅以：

```text
npm run build
```

作为完成标准。

必须：

1. 启动前端
2. 访问页面
3. 查看真实后端数据
4. 检查主要用户流程
5. 检查视觉层级
6. 检查空状态
7. 检查 Loading
8. 检查 Error
9. 检查长标题
10. 检查大量数据
11. 检查 Drawer
12. 检查键盘操作
13. 按 `VISUAL_REVIEW.md` 自检

---

# 20. 最终完成标准

一个页面只有同时满足以下条件才算完成：

```text
产品正确
+
业务规则正确
+
信息层级清晰
+
交互路径短
+
视觉一致
+
状态完整
+
真实数据可用
+
没有明显 AI UI Slop
```

---

# 21. 如果不确定

当 Agent 无法确定某个 UI 应该如何设计时：

不要自行增加功能。

优先选择：

> **更简单、更直接、更少维护成本的方案。**

WorkOS 的核心原则：

> **用户只记录事实一次，系统负责重新组织。**

因此每增加一个输入框，都应该问：

> “这个信息是否已经可以从已有事实得到？”

如果可以：

> 不要让用户再次输入。

---

# 22. 最终设计理念

WorkOS 不应该让用户感到：

> “我又多了一个系统要维护。”

而应该让用户感到：

> “我刚才做到哪里，我现在应该干什么，我在等谁，我接下来要跟谁联系——它都帮我整理好了。”

这就是前端设计的最终目标。

---

# WorkOS Design System

> WorkOS MVP V1.3.5
>
> 本文定义 WorkOS 的视觉语言、布局、组件、颜色、Typography、密度和页面设计原则。
>
> 本文不是业务 PRD。
>
> 本文的作用是：**让不同 Agent / 不同开发人员实现出来的页面仍然像同一个产品。**

---

# 1. Design Philosophy

WorkOS 是：

> Personal Work Operating System

不是：

* Todo App
* Project Management Dashboard
* ERP
* Jira Clone
* Notion Clone
* AI Dashboard

设计目标：

```text
减少认知负担
降低上下文恢复成本
提高扫描速度
减少重复输入
缩短操作路径
```

---

# 2. Visual Keywords

WorkOS 的视觉关键词：

```text
Calm
Professional
Dense
Focused
Quiet
Fast
Structured
Predictable
```

中文：

```text
克制
专业
紧凑
专注
安静
快速
结构清晰
行为可预测
```

---

# 3. Anti-pattern

以下视觉效果默认禁止。

## 3.1 禁止渐变

禁止：

```css
background: linear-gradient(...)
```

除非未来有明确产品需求。

---

## 3.2 禁止玻璃拟态

禁止：

```text
backdrop-filter
大量透明卡片
发光边框
玻璃背景
```

---

## 3.3 禁止巨型 Dashboard Card

不要：

```text
┌──────────────────────────┐
│                          │
│           12             │
│       Today's Tasks      │
│                          │
└──────────────────────────┘
```

信息应该直接进入工作流。

---

# 4. Color System

颜色主要表达语义。

建议基础色：

```text
Background
#FFFFFF

Page background
#F7F8FA

Primary text
#1F2329

Secondary text
#646A73

Tertiary text
#8F959E

Border
#E5E6EB
```

语义颜色：

```text
Primary
#1677FF

Success
#52C41A

Warning
#FAAD14

Danger
#FF4D4F
```

注意：

> 不要求所有页面必须使用所有颜色。

颜色只在有语义时出现。

---

# 5. Priority Colors

Priority 不应该使用巨大彩色标签。

推荐：

```text
P0 → Danger
P1 → Warning / Strong emphasis
P2 → Neutral
P3 → Tertiary
```

推荐：

```text
P0
```

而不是：

```text
████████ P0 URGENT ████████
```

---

# 6. Status Colors

```text
INBOX
Neutral

TODO
Neutral / Primary

DOING
Primary

WAITING
Warning

DONE
Success

CANCELLED
Tertiary
```

状态应该通过：

```text
文字
icon
轻量颜色
```

表达。

不要给整个卡片染色。

---

# 7. Typography

页面标题：

```text
24px
font-weight: 600
```

Section：

```text
16px
font-weight: 600
```

Item title：

```text
14px
font-weight: 500
```

Body：

```text
14px
font-weight: 400
```

Metadata：

```text
12px
font-weight: 400
```

原则：

> 字体大小差异用于建立层级，而不是装饰。

---

# 8. Spacing

采用 4px 基础 spacing。

常用：

```text
4
8
12
16
24
32
40
48
```

页面：

```text
Page padding: 24px
Section gap: 24px
Item gap: 8~12px
Card internal padding: 16px
```

不要随意出现：

```text
13px
17px
19px
27px
31px
```

除非 Ant Design Vue 本身组件要求。

---

# 9. Radius

整体保持克制。

推荐：

```text
Small control: 6px
Card: 8px
Dialog/Drawer content: 8~10px
Pill: 999px
```

不要：

```text
border-radius: 20px
border-radius: 24px
```

让整个产品看起来像移动端 AI SaaS。

---

# 10. Shadow

默认优先：

```text
border
```

而不是：

```text
heavy shadow
```

阴影仅用于：

* Drawer
* Dropdown
* Popover
* Modal

普通 Item 不需要明显阴影。

---

# 11. Icons

统一使用现有 icon system。

推荐使用 Ant Design Icons。

禁止：

* Emoji
* 混用多个 icon library
* 自己找一套 SVG
* 用文字模拟 icon

Icon 应该：

```text
16px
24px
```

根据语境选择。

---

# 12. Layout

WorkOS MVP 桌面布局：

```text
┌──────────────────────────────────────────────────────┐
│ Sidebar │ Main                                      │
│         │                                           │
│         │ Page Header                               │
│         │                                           │
│         │ Content                                   │
│         │                                           │
└──────────────────────────────────────────────────────┘
```

默认内容宽度：

```text
1080px 左右
```

不做无限宽 Dashboard。

---

# 13. Sidebar

一级导航：

```text
Today
Inbox
Items
Projects
Daily
```

Search：

```text
⌘K
```

作为全局操作，不作为一级导航。

Sidebar 原则：

* 不使用巨大 Logo
* 不堆统计数字
* 不放复杂树
* 不放大量二级菜单
* 当前页面必须明显
* Inbox 数量可以显示
* 其他数量只有有实际价值时才显示

---

# 14. Header

页面 Header 应包含：

```text
页面标题
必要的上下文
主要操作
```

不要：

```text
标题 + 5 个按钮 + 4 个统计 + 3 个 Dropdown
```

主要操作最多 1 个视觉 Primary Action。

---

# 15. Card

Card 是信息分组工具，不是默认容器。

错误：

```text
页面
 ├── Card
 │   ├── Card
 │   └── Card
 ├── Card
 └── Card
```

正确：

```text
页面
 ├── Section
 │   └── Item list
 ├── Section
 │   └── Item list
 └── Section
     └── Item list
```

---

# 16. Item List

WorkOS 核心信息应该更多使用：

```text
List
```

而不是：

```text
Card Grid
```

推荐：

```text
┌──────────────────────────────────────────────┐
│ □ 完成工单自动分配       P1   项管系统      │
│   接口联调 · 今天 18:00                     │
└──────────────────────────────────────────────┘
```

---

# 17. ItemCard

ItemCard 默认：

```text
第一行：
[checkbox/state] [title]                 [priority]

第二行：
[project] [status/time/context]

第三行：
必要时显示：
[waiting] [follow-up] [overdue] [remaining steps]
```

但不要让每个 Item 都出现三行。

原则：

> **只有有价值的信息才显示。**

---

# 18. Item 次级标签

前端根据事实字段组合展示。

最多：

```text
2 个
```

优先表达：

```text
Waiting
Follow-up
Overdue
Due today
Planned
Remaining steps
```

不要一次显示：

```text
P1
TODO
项目A
今天
未完成
跟进
等待
3 steps
Task
Manual
```

---

# 19. Drawer

Item Drawer 是核心交互。

推荐：

```text
Drawer
────────────────────────────

标题

P1   DOING   项管系统

描述

────────────────────────────
项目
状态
优先级
计划日期

────────────────────────────
Waiting / Follow-up

────────────────────────────
记一句

────────────────────────────
Activity
```

操作区域应保持稳定。

---

# 20. Empty State

Empty State 不应该是：

```text
🎉
Nothing here!
```

应该告诉用户：

```text
这里为什么为空
现在应该做什么
```

例如 Today：

> 今天还没有计划事项
> 从“还没安排”中选择今天要做的工作。

Inbox：

> 收件箱是空的
> 临时想到的事情可以直接记在这里。

Waiting：

> 当前没有等待中的事项。

---

# 21. Loading

优先：

```text
Skeleton
```

或：

```text
局部 loading
```

不要整个页面：

```text
Loading...
```

---

# 22. Error

错误应该：

```text
告诉用户发生了什么
告诉用户下一步
```

例如：

> 保存失败，请重试。

而不是：

> Error 500.

---

# 23. Motion

MVP 动效非常克制。

允许：

* Drawer transition
* Popover
* Dropdown
* Toast
* 列表轻微状态变化

禁止：

* 页面飞入
* 大面积动画
* 自动滚动
* 自动跳转
* 装饰性动画

WorkOS 的目标是：

> **快，而不是炫。**

---

# 24. Today Page Design

Today 是 WorkOS 的首页，也是最高优先级页面。

## 24.1 页面目标

打开页面后：

> **30 秒内知道今天应该关注什么。**

页面回答：

```text
今天做什么？
今天别忘了什么？
我在等谁？
还有什么没有安排？
```

---

## 24.2 页面结构

固定结构：

```text
Today
│
├── Today Header
│
├── Today Plan
│
├── Don't Forget
│   ├── Overdue
│   ├── Due Today
│   └── Follow-up
│
└── Waiting / Unplanned
    ├── Waiting
    └── Unplanned
```

对应产品语言：

```text
今天做什么

今天别忘了

我在等谁 / 还没安排
```

---

## 24.3 Today Header

显示：

```text
今天 · 9月25日
```

如果有需要用户处理的关键事项，可以显示轻量提示：

```text
2 项计划未完成
1 个跟进已逾期
3 条待整理
```

不要设计成巨大 Summary Card。

---

# 25. Today：今天做什么

这是页面最高视觉优先级。

```text
今天做什么

计划 · 2/3

□ 完成工单自动分配
  项管系统 · P1

□ 完成接口联调
  用户中心 · P1

[+ 添加今天计划]
```

默认最多突出：

```text
3 件
```

更多内容不应该抢夺主视线。

---

# 26. Today：今天别忘了

三个语义：

```text
逾期
今日到期
待跟进
```

排序：

```text
Overdue
↓
Due Today
↓
Follow-up
```

每一项保持紧凑。

---

# 27. Today：等待

Waiting 重点回答：

```text
我在等谁？
等什么？
什么时候应该有结果？
是否已经超过预期？
```

例如：

```text
我在等谁

等待厂商提供接口文档
项管系统
厂商A · 预计今天

等待产品确认客户范围
项管系统
产品 · 已超过预计时间 1 天
```

---

# 28. Today：还没安排

这是捕漏区。

语义不是：

> 系统推荐你做这个。

而是：

> **你已经承认这是正式工作，但还没有决定什么时候做。**

因此文案：

```text
还没安排

PMO 项目进度同步
P1 · 项管系统

还没安排，要不要放到今天？

[今天安排]
```

最多展示：

```text
5 条
```

其余：

```text
查看全部
```

---

# 29. Inbox Page Design

Inbox 的定位：

> **临时记录的处理队列。**

不是长期任务列表。

---

## 29.1 Header

```text
Inbox                         7

[整理最老的一条]    [先都收下]
```

---

## 29.2 快速输入

页面顶部保持极低摩擦：

```text
记录一件事……
```

输入：

```text
跟进厂商接口问题
```

Enter：

```text
创建
```

只要求标题。

---

# 30. Inbox 两种处理模式

## 模式 A：逐条整理

适合需要认真决定的事项。

```text
标题

项目
类型
优先级
日期
跟进
等待

[保存]
```

---

## 模式 B：先都收下

批量：

```text
status = TODO
priority = P2
project = null
```

用户无需逐条做完整决策。

这是 Inbox 防止成为“黑洞”的关键能力。

---

# 31. Inbox Keyboard

允许：

```text
Enter
Tab
1 / 2 / 3 / 4
P
D
F
W
```

但：

> 键盘是加速器，不是唯一入口。

所有关键操作必须仍然有鼠标入口。

---

# 32. Project Page Design

Projects 页面回答：

> **我有哪些项目？每个项目目前处于什么状态？**

---

## 32.1 Project List

不要做成大型 Dashboard Card Grid。

推荐：

```text
Projects

项管系统
ACTIVE
当前重点：工单自动分配
未完成 8 · Waiting 2 · 今日 3

用户中心
ACTIVE
当前重点：权限模型
未完成 5 · Waiting 1 · 今日 1
```

---

# 33. Project Detail

Project Detail 是 WorkOS 的第二核心页面。

目标：

> **10 秒恢复项目上下文。**

固定结构：

```text
项目名称
状态

当前目标
当前重点
当前进展
当前阻塞
下一步

────────────────

事项
Waiting
Follow-up
WorkLog
Daily
```

---

# 34. Project Context

Context 必须优先结构化展示：

```text
当前目标
当前重点
当前进展
当前阻塞
下一步
关键联系人
备注
```

不要把整个 Project Context 做成一块 Markdown 编辑器。

用户进入项目时首先看到的是：

```text
结构化信息
```

而不是：

```text
一大段 Markdown
```

---

# 35. Project Detail 的核心视觉优先级

优先：

```text
当前重点
当前阻塞
下一步
```

其次：

```text
当前进展
目标
```

再次：

```text
联系人
备注
```

---

# 36. Item Page / Item Drawer

MVP 不需要为 Item 单独做复杂详情页面。

核心入口：

```text
ItemCard
 ↓
ItemDrawer
```

---

## 36.1 Drawer 第一屏

用户打开 Drawer 后第一眼应该看到：

```text
标题

项目 · 状态 · 优先级

描述

当前需要做什么
```

而不是：

```text
ID
createdAt
updatedAt
source
version
```

---

# 37. Item 操作

主要动作：

```text
状态
计划
Waiting
Follow-up
完成
```

次要动作：

```text
记一句
查看历史
编辑描述
```

危险动作：

```text
删除
```

应该明显降低视觉权重。

---

# 38. Waiting Interaction

点击：

```text
在等别人
```

打开轻量表单：

```text
在等谁 *
预计时间
下次跟进时间
```

用户不应该先：

```text
修改状态
保存
再打开 Follow-up
再保存
```

而应该一次完成。

---

# 39. Follow-up Interaction

Follow-up 是能力，不是 Item 类型。

UI 文案使用：

```text
跟进
```

而不是：

```text
创建 Follow-up
```

例如：

```text
下次跟进

2026-09-26 10:00

[保存]
```

---

# 40. Visual Hierarchy

任何页面都必须遵循：

```text
Primary
 ↓
Secondary
 ↓
Metadata
 ↓
Technical details
```

用户看到：

```text
我要做什么
```

优先于：

```text
系统记录了什么
```

---

# 41. Final Design Rule

如果一个设计方案在：

```text
视觉上更漂亮
```

但：

```text
操作更慢
信息更难扫描
认知成本更高
```

选择：

> **操作更快的方案。**

WorkOS 的审美不是装饰。

WorkOS 的审美是：

> **让复杂工作变得安静、清晰、可控。**

---

下面这两个文件我建议合并成一个较大的 Agent 规则块放入仓库，因为它们本质上都是**交互约束 + 验收规则**。如果你希望物理文件严格拆开，直接按下面两个一级标题拆成两个 `.md` 即可。

# design/UX_RULES.md

# WorkOS UX Rules

> 本文件定义 WorkOS 的交互规则。
>
> DESIGN.md 解决“长什么样”。
>
> 本文件解决“应该怎么用”。

---

# 1. 核心 UX 原则

WorkOS 的核心 UX 目标：

> **让用户少想、少填、少维护，但始终知道系统为什么这样展示。**

---

# 2. 三秒原则

用户进入一个页面后：

### 3 秒

知道：

```text
我在哪里？
```

### 10 秒

知道：

```text
我应该关注什么？
```

### 30 秒

知道：

```text
我今天应该做什么？
```

---

# 3. 上下文恢复原则

WorkOS 的核心竞争力不是：

> 管理更多任务。

而是：

> **更快恢复被打断之前的工作上下文。**

因此任何页面都应该帮助用户回答：

```text
我上次做到哪里？
为什么做到这里？
下一步是什么？
有没有人在等我？
我是不是在等别人？
```

---

# 4. 不让用户重复输入事实

核心规则：

> **能从已有事实推导出来，就不要让用户再次输入。**

例如：

已经完成 Item：

```text
不要强制 WorkLog
```

已经有：

```text
nextFollowUpAt
```

不要强制再次创建同时间 Reminder。

已经完成：

```text
Item
```

Daily 可以直接使用它。

---

# 5. Quick Capture

Quick Capture 必须：

```text
< 5 秒
```

理想：

```text
输入标题
↓
Enter
↓
Inbox
```

不要要求：

```text
项目
优先级
日期
状态
负责人
标签
```

---

# 6. Inbox

Inbox 的心理模型：

> **我还没有决定怎么处理这件事。**

不要把 Inbox 做成：

> “任务列表”。

---

# 7. Inbox 与“还没安排”的区别

必须通过 UI 文案直接解释：

### Inbox

> 这件事我还没决定怎么处理。

### 还没安排

> 我已经承认这是一件正式的工作，但还没决定什么时候做。

这是两个不同心理状态。

---

# 8. Inbox 不允许成为黑洞

如果 Inbox 数量增加：

不要：

```text
继续要求用户逐条整理
```

必须提供：

```text
整理最老的一条
```

和：

```text
先都收下
```

---

# 9. “先都收下”

语义：

```text
status = TODO
priority = P2
project = null
```

不要偷偷：

```text
自动分项目
自动定日期
自动设优先级
自动推荐
```

---

# 10. Today

Today 是：

> **决策面**

不是：

> 全部重要事项的过滤结果。

---

# 11. Today 的入选必须可解释

Today 中每一条事项：

用户应该能够回答：

> **“为什么它出现在这里？”**

例如：

```text
今日计划
```

因为用户显式安排。

或者：

```text
逾期
```

因为 dueAt 已经过期。

或者：

```text
今日到期
```

因为 dueAt 在今天。

或者：

```text
待跟进
```

因为 nextFollowUpAt 到期。

或者：

```text
等待中
```

因为状态为 WAITING。

或者：

```text
还没安排
```

因为符合后端定义的捕漏规则。

---

# 12. Today 不使用黑箱推荐

MVP 禁止：

```text
AI 推荐你今天做这个
```

也禁止：

```text
根据最近打开次数
根据用户行为
根据“感觉重要”
```

决定 Item 是否进入 Today。

排序可以优化。

入选必须基于明确事实。

---

# 13. Today 排序

排序可以由后端完成。

前端：

> 不重新排序。

如果后端已经提供排序：

```text
直接展示。
```

---

# 14. Today 的视觉优先级

顺序固定：

```text
今天做什么
↓
今天别忘了
↓
我在等谁 / 还没安排
```

不要把：

```text
Waiting
```

做得比：

```text
Today Plan
```

更突出。

---

# 15. Today Plan

默认突出：

```text
3 件
```

不是硬限制。

如果后端返回更多：

可以显示：

```text
3 / 7
```

但视觉上重点仍然是用户当前选定的计划。

---

# 16. Overdue

逾期必须明显。

但不要：

```text
整张卡红色
```

推荐：

```text
红色 icon
+
红色 metadata
```

---

# 17. Waiting

Waiting 不只是：

```text
status = WAITING
```

用户真正关心：

```text
谁
+
什么
+
预计什么时候
+
什么时候我要再次推动
```

所以 Waiting UI 应该突出：

```text
waitingFor
waitingExpectedAt
nextFollowUpAt
```

---

# 18. Follow-up

Follow-up 不是一种 Item。

不要出现：

```text
Type = FOLLOW_UP
```

用户看到：

```text
跟进
```

即可。

---

# 19. Waiting + Follow-up 可以同时存在

例如：

```text
等待厂商接口文档

等待对象：
厂商A

预计：
今天

下一次跟进：
明天 10:00
```

这完全合理。

---

# 20. Project

项目详情必须帮助用户：

> **10 秒恢复上下文。**

第一屏必须出现：

```text
当前重点
当前阻塞
下一步
```

---

# 21. Project 不要变成第二个 Jira

不要在 Project Detail 首页放：

```text
复杂 Kanban
Sprint
Burn-down
Story Point
Velocity
大量统计
```

这些不属于 MVP 核心。

---

# 22. Item

Item 是：

> 一个需要进入用户注意力系统并最终闭环的事项。

不是：

```text
一个技术步骤
```

也不是：

```text
一个数据库记录
```

---

# 23. Item 操作原则

用户打开 Item 后：

```text
看事实
↓
做动作
```

而不是：

```text
查看 20 个字段
↓
找到按钮
↓
进入第二页面
↓
继续操作
```

---

# 24. 状态变化

状态操作必须来自：

```text
allowedTransitions
```

用户看到：

```text
开始处理
等待别人
完成
取消
```

而不是：

```text
INBOX
TODO
DOING
WAITING
DONE
CANCELLED
```

技术枚举可以在开发工具里存在。

用户界面应该使用用户语言。

---

# 25. 删除

删除不是主要动作。

优先使用：

```text
取消
归档
```

而不是：

```text
Delete
```

如果存在删除操作：

必须二次确认。

---

# 26. 表单原则

表单字段：

> **只放当前动作所需要的字段。**

例如进入 Waiting：

只问：

```text
在等谁 *
预计时间
下一次跟进
```

不要打开一个完整 Item Edit Form。

---

# 27. Drawer 优先于跳转

Item 操作：

```text
列表
 ↓
Drawer
```

只有需要：

```text
完整编辑
```

才考虑进入完整页面。

---

# 28. 操作成功后的行为

默认：

```text
保存成功
↓
刷新事实
↓
保持用户上下文
```

不要无意义地：

```text
跳转首页
关闭整个页面
跳到项目页
```

---

# 29. 键盘

快捷键是加速器。

不是：

> 用户必须学习的一门语言。

任何快捷操作必须存在鼠标等价入口。

---

# 30. Command Palette

如果未来实现：

```text
⌘K
```

它应该用于：

```text
搜索
快速进入页面
快速创建
快速执行明确动作
```

不要把所有功能都塞进去。

---

# 31. Empty State

Empty State 必须解释：

```text
为什么为空
```

必要时：

```text
下一步是什么
```

例如：

```text
今天还没有计划事项

从“还没安排”中选择今天要做的工作。
```

---

# 32. Loading

页面加载：

```text
Skeleton
```

局部操作：

```text
Button loading
```

不要整个页面闪烁。

---

# 33. Error

错误消息应该：

```text
事实 + 下一步
```

例如：

```text
保存失败，请重试。
```

---

# 34. 防止误操作

高风险操作：

```text
删除
取消
覆盖日报
```

必须有明确确认。

普通操作：

```text
状态切换
计划
跟进
```

尽量不增加确认弹窗。

---

# 35. 用户中断原则

WorkOS 用户经常被打断。

所以：

> **任何 Drawer / Modal / 表单都不能让用户忘记自己刚才在做什么。**

例如关闭 Drawer：

不应该丢失未保存输入。

如果有修改：

提示：

```text
有未保存修改，确定关闭？
```

---

# 36. 加班场景

不要因为晚上 23:00 就自动改变 UI。

不要：

```text
Good night
辛苦了
该休息了
```

WorkOS 是工作工具。

时间只是事实。

---

# 37. 不做拟人化

禁止：

```text
嘿！准备好开始高效的一天了吗？
🎉 今天也要加油！
```

WorkOS 应该：

```text
今天 · 9月25日
```

克制、直接。

---

# 38. 不做 AI 味文案

禁止：

```text
✨ Smart Insights
🚀 Boost your productivity
🧠 AI-powered workflow
```

MVP 没有 AI，就不要模拟 AI。

---

# 39. 页面切换

路由切换应该：

```text
快
稳定
不产生无意义动画
```

---

# 40. 最终 UX 判断

每一个新增 UI 都问：

```text
这个 UI 帮助用户完成什么任务？
```

如果回答不清楚：

> 删除它。

---

# design/VISUAL_REVIEW.md

# WorkOS Visual & UX Review

> 本文件用于 Codex 在完成页面后进行自检。
>
> **页面没有经过本清单检查，不允许认为 UI 工作完成。**

---

# 1. Product Fit

检查：

* [ ] 页面符合 WorkOS 产品定位
* [ ] 没有变成通用 SaaS Dashboard
* [ ] 没有加入 PRD 未要求的业务功能
* [ ] 页面主要任务明确
* [ ] 用户进入页面后不需要猜应该做什么

---

# 2. Visual Quality

检查：

* [ ] 没有渐变背景
* [ ] 没有玻璃拟态
* [ ] 没有大面积阴影
* [ ] 没有巨大 Card
* [ ] 没有无意义统计数字
* [ ] 没有 Emoji UI
* [ ] 没有多套 Icon 风格
* [ ] 没有过多颜色
* [ ] 没有过大的圆角
* [ ] 没有装饰性动画
* [ ] 没有“AI Dashboard”视觉风格

---

# 3. Information Hierarchy

检查：

* [ ] 页面标题明显
* [ ] Primary 信息明显
* [ ] Secondary 信息弱于 Primary
* [ ] Metadata 没有抢夺注意力
* [ ] 用户首先看到的是工作内容，而不是技术字段
* [ ] 重要状态容易扫描
* [ ] 不重要信息没有视觉噪音

---

# 4. Density

检查：

* [ ] 页面没有过度留白
* [ ] 页面没有拥挤
* [ ] Item 可以快速扫描
* [ ] 同一屏能够看到足够多的工作内容
* [ ] 没有为了“高级感”把内容做得巨大
* [ ] Section 间距一致

---

# 5. Consistency

检查：

* [ ] Button 风格一致
* [ ] Input 风格一致
* [ ] Radius 一致
* [ ] Spacing 遵循 4px grid
* [ ] Font hierarchy 一致
* [ ] Icon size 一致
* [ ] Color semantic 一致
* [ ] Drawer 行为一致
* [ ] Toast 行为一致

---

# 6. Today Review

必须检查：

* [ ] 进入 Today 后能立即看到日期
* [ ] “今天做什么”视觉优先级最高
* [ ] Today Plan 明确
* [ ] Overdue 明显但不刺眼
* [ ] Due Today 明确
* [ ] Follow-up 明确
* [ ] Waiting 明确
* [ ] Unplanned 明确
* [ ] 每条 Today Item 都能解释为什么出现
* [ ] 没有黑箱推荐
* [ ] 没有大量无关信息
* [ ] “还没安排”不是第二个 Inbox

---

# 7. Inbox Review

检查：

* [ ] 快速输入明显
* [ ] 输入标题即可创建
* [ ] “整理最老的一条”明显
* [ ] “先都收下”明显
* [ ] 用户不需要逐条完成所有字段
* [ ] Inbox 数量明显
* [ ] Inbox 空状态明确
* [ ] Keyboard 是加速器而不是唯一入口

---

# 8. Project Review

检查：

* [ ] 项目名称明确
* [ ] 项目状态明确
* [ ] 当前目标明确
* [ ] 当前重点明确
* [ ] 当前进展明确
* [ ] 当前阻塞明确
* [ ] 下一步明确
* [ ] 用户 10 秒内可以恢复项目上下文
* [ ] Project Detail 没有变成 Jira
* [ ] 没有无意义 KPI
* [ ] 没有复杂 Dashboard

---

# 9. Item Review

检查：

* [ ] 标题最明显
* [ ] 项目/状态/优先级容易理解
* [ ] 主要动作明显
* [ ] Waiting 操作简单
* [ ] Follow-up 操作简单
* [ ] 不需要打开多个页面完成一个动作
* [ ] Drawer 不显示大量技术字段
* [ ] Delete 不是视觉 Primary Action
* [ ] Activity 与 WorkLog 没有混淆

---

# 10. Interaction Review

检查：

* [ ] Primary action <= 3 次操作完成
* [ ] 普通编辑 <= 3~5 次关键交互
* [ ] 不存在无意义确认弹窗
* [ ] 写操作成功后页面不会突然跳走
* [ ] 写操作后展示的是服务器最新事实
* [ ] Loading 状态明确
* [ ] Error 状态明确
* [ ] Empty 状态明确
* [ ] 长标题不会破坏布局
* [ ] 长项目名称不会破坏布局

---

# 11. Data Review

使用至少以下数据测试：

```text
1 条数据
3 条数据
10 条数据
50 条数据
```

同时测试：

```text
标题很短
标题很长
项目名很长
没有项目
没有日期
有逾期
有 Waiting
有 Follow-up
P0
P1
P2
P3
DONE
CANCELLED
```

---

# 12. State Review

每个页面至少验证：

```text
Loading
Empty
Normal
Large Dataset
Error
```

---

# 13. Responsive Boundary

MVP 不做移动端。

设计目标：

```text
桌面
1080px 内容区域
```

但是：

> 不允许在 1080px 宽度下出现横向滚动。

---

# 14. Accessibility

至少保证：

* [ ] Button 有明确文字或 aria-label
* [ ] Input 有 label / placeholder
* [ ] 键盘可以完成核心操作
* [ ] Focus 状态可见
* [ ] 不仅依赖颜色表达状态
* [ ] 文本对比度可读

---

# 15. AI Slop Detection

如果出现以下情况，必须重新设计：

```text
大量 Card
大量 Badge
大量 Gradient
大量 Icon
大量圆角
大量阴影
巨大标题
巨大数字
无意义统计
装饰性插画
Emoji
“AI味”营销文案
```

尤其出现：

```text
Dashboard
Analytics
Insights
Productivity Score
AI Suggestions
Smart Recommendations
```

而 PRD 没有要求：

> 默认删除。

---

# 16. Final Five Questions

Codex 在提交页面前必须回答：

### 1.

> 用户进入这个页面后，3 秒内知道自己在哪里吗？

### 2.

> 10 秒内知道自己应该关注什么吗？

### 3.

> 主要任务是否可以快速完成？

### 4.

> 页面有没有任何只是为了“看起来高级”而存在的东西？

### 5.

> 如果删掉这个 UI，用户是否真的会损失工作能力？

如果第 4 个答案是“有”：

> 删除。

如果第 5 个答案是“不会”：

> 优先删除。

---

# 17. Final Quality Bar

最终页面应该给人的感觉：

```text
不是：
“哇，好炫。”

而是：

“嗯，这东西挺顺手。”
```

WorkOS 的最终视觉目标：

> **安静、清晰、紧凑、专业。**

最终 UX 目标：

> **让我更快恢复工作，而不是让我管理这个工具。**

### 这套文件放进去之后，建议你给 Codex 的启动指令也固定下来

不要再只说：

> “帮我优化 Today 页面，让它高级一点。”

而是用这种任务格式：

```text
请重新设计并实现 TodayPage.vue。

执行前必须阅读：

1. /AGENTS.md
2. /design/DESIGN.md
3. /design/UX_RULES.md
4. /design/VISUAL_REVIEW.md

严格遵守现有：
- Vue 3
- TypeScript
- Vite
- Ant Design Vue
- Axios
- Dayjs
- 现有 API 契约

不要修改后端 API。
不要增加产品功能。
不要增加新的业务实体。
不要引入新的 UI 框架。

本次目标不是增加功能，而是提升：
1. 信息层级
2. 页面密度
3. 工作扫描效率
4. 上下文恢复效率
5. 操作路径
6. 视觉一致性

严格按照 DESIGN.md 和 UX_RULES.md 中的 Today 页面规则实现。

完成后：

1. npm run build
2. 启动项目
3. 使用真实后端数据检查
4. 检查 Loading / Empty / Error / Normal
5. 检查长标题和多数据
6. 按 VISUAL_REVIEW.md 自检
7. 如果发现 Generic SaaS Dashboard / AI Slop UI，先修改再提交。

不要为了“漂亮”增加任何没有产品价值的视觉元素。
```

**还有一个我认为很重要的后续动作：**你现在已经有这套规则，但**最好再给 Codex 一套“参考页面截图/参考实现”**。因为纯文字规范解决的是“不要做错”，而截图/参考实现解决的是“应该做到什么程度”。

尤其是 `Today`，我建议下一步直接把它做成一张**页面级高保真设计规格**：包括 1080px 页面具体网格、Sidebar 宽度、Header 高度、每个 Section 的间距、Item 行高、按钮位置、Drawer 宽度、空状态、Loading、Hover、Selected、Keyboard Focus，以及真实数据下的 3/10/50 条 Item 视觉表现。这样 Codex 才真正有一个可以照着实现的“标尺”，而不是仅仅拿设计原则自行发挥。
