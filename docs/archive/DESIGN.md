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

