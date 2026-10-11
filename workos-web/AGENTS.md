# WorkOS Frontend Agent Instructions

> 适用范围：workos-web/（本目录及其子目录）
> 产品版本：MVP / PRD V1.3.5
> 前端：Vue 3 + TypeScript + Vite + Ant Design Vue

本文件是本仓库前端工作的最高优先级约束。前端规范有两份形态，用哪份取决于你在做什么：

| 文件 | 内容 | 什么时候读 |
|---|---|
| ../docs/archive/前端Agent规则.md | **Agent 执行规则**：硬约束（N1~N41）、契约、可直接跑的验收命令 | 写代码前先读这份，交付前按它自检 |
| ../docs/archive/前端规范.md | **完整规范**：含每条规则的原因、设计判断依据、实现对照 | 需要做设计决策，或想知道「为什么这么定」时读 |

两份文档放在 `docs/archive/`，**该目录存放的是前端设计资料，不等于「全部废弃」**——目录里的 `README.md` 标明了哪两份现行。同目录的 DESIGN.md / UX_RULES.md / VISUAL_REVIEW.md / 旧版-前端设计打包.md 是历史版本，内容已并入 `前端规范.md`，**不要作为依据**。

冲突时的优先级：

```
产品需求 > ../docs/archive/前端规范.md > 既有实现 > Agent 自由发挥
```

核心提醒：

- 这些文档是为了**限制**自由发挥，不是为了让 Agent 自由设计。
- 不要新增 PRD 未定义的实体、导航、状态、流程、指标、推荐机制。
- 不要在前端推导业务规则（逾期、Today 分区、等待天数、状态合法性）。
- 信息价值 > 视觉装饰；操作更快 > 视觉更漂亮。

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

如果 PRD / `../docs/archive/前端规范.md` 没有要求：

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
2. `../docs/archive/前端Agent规则.md`（执行规则）
3. `../docs/archive/前端规范.md`（完整规范与依据）
4. 当前已有组件和页面
5. Ant Design Vue 官方能力
6. Agent 自己的判断

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
13. 按 `../docs/archive/前端规范.md` 第 8 节自检

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
