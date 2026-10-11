# WorkOS 前端 Agent 规则

```yaml
适用路径: workos-web/**
优先级:   产品 PRD > 本文件 > 既有实现 > 你的判断
来源:     同目录 前端规范.md（v2.0 工作界面重构规范）的机械投影
冲突处理: 与 前端规范.md 冲突时以它为准，并顺手修正本文件
不要读:   同目录的 DESIGN.md / UX_RULES.md / VISUAL_REVIEW.md / 旧版-前端设计打包.md（已废弃）
路径约定: 文中 src/... 均相对 workos-web/；命令在 workos-web/ 目录下执行
```

## 0. 使用方式

```text
改任何页面前   → §1 硬约束 + §2 视觉与布局契约 + §4 对应页面契约
改 ItemRow/抽屉 → §1 + §3
交付前         → §7 验收，全部通过才算完成
需要设计判断时 → 读同目录 前端规范.md
```

取舍顺序永远是：**信息架构 > 布局 > 密度 > 操作路径 > 状态表达 > 视觉层级 > 动画 > 装饰**。

判断不了就选更简单、更少维护的方案。不新增 PRD 没有的实体、导航、状态、流程、指标、推荐机制。

---

## 1. 硬约束（违反即返工）

### 视觉

| # | 规则 | 怎么判定违反 |
|---|---|---|
| N1 | 禁止渐变背景 | `linear-gradient` / `radial-gradient` |
| N2 | 禁止玻璃拟态 | `backdrop-filter`、半透明卡片、发光边框 |
| N3 | 阴影只允许 Drawer / Modal / Popover / Dropdown（唯一例外：事项行选中态的 `inset` 左侧色条） | 事项行、区块、Sidebar、按钮上出现其他 `box-shadow` |
| N4 | 事项行、区块、页面圆角为 0 | 事项行出现 `border-radius` |
| N5 | 禁止卡片化 | Item 一张卡、Section 一张卡、项目/日报一张卡 |
| N6 | 禁止 KPI 与大数字 | 无行动价值的统计块、`P0 12 / Doing 10` |
| N7 | 禁止 Emoji 当 UI、禁止混用图标库 | 界面出现 emoji 或非 Ant Design Icons |
| N8 | 禁止装饰性动画 | 页面飞入、列表错峰入场、自动滚动、自动跳转 |
| N9 | 禁止 AI 味与拟人文案 | ✨ / 🚀 / 「准备好开始高效的一天了吗」 |
| N10 | 禁止给整行整卡染色 | 状态只用符号 + 文字 + 轻量颜色 |
| N11 | 禁止深色模式 | MVP 不实现（Token 可留） |
| N12 | 禁止复制 Linear 的视觉皮肤与品牌语言 | 只在密度、快捷操作、键盘体验上借鉴 |

### 布局与密度

| # | 规则 | 怎么判定违反 |
|---|---|---|
| N13 | 内容区最大 1200px 居中，Sidebar 208px | 无限拉伸、宽屏铺满 |
| N14 | 1080px 宽度下不允许横向滚动 | 出现横向滚动条 |
| N15 | 事项行高度落在 52~68px | 单行行高 < 52 或 > 68 |
| N16 | 事项行默认最多两行 | 第二行堆了三件以上事实 |
| N17 | 页面 Header 最多 1 个主要操作 | `[刷新][导出][新增][筛选][更多]` 全堆上去 |
| N18 | 区块不是卡片，只有标题行 + 分隔线 | 区块出现背景块 + 圆角 + 阴影 |
| N19 | 行内动作不得改变行高 | 出现动作时行高跳动 |

### 组件与交互

| # | 规则 | 怎么判定违反 |
|---|---|---|
| N20 | 高频动作必须在行内完成 | 完成 / 开始 / 等待 / 跟进 只能进 Drawer 做 |
| N21 | 行内动作可选值只能来自后端 `allowedTransitions` | 前端写死状态机 |
| N22 | Drawer 只做深度编辑与上下文查看 | 把 Drawer 当所有操作的唯一入口 |
| N23 | Drawer 宽度 520~560px，底部有随状态变化的主要动作 | 宽度 700px+、底部只有「保存」 |
| N24 | 禁止 Drawer 顶部彩色状态条 | 用整条颜色表示状态 |
| N25 | 首屏之后的刷新不得整页变灰 | `<a-spin :spinning="loading">` 包住已渲染内容 |
| N26 | loading 按钮保持原文字 + spinner | 按钮文字变成「Loading...」、宽度跳动 |
| N27 | 任何快捷操作必须有鼠标等价入口 | 只能靠键盘完成 |
| N28 | 长列表不得劫持上下方向键 | 今天 / 全部事项里用 `ArrowUp/Down` 做选择 |
| N29 | 所有可交互元素必须有可见 focus | `outline: none` 后不补 |
| N30 | 高风险动作必须二次确认 | 删除 / 取消 / 覆盖日报 直接执行 |
| N31 | 普通动作不得加确认弹窗 | 状态切换 / 计划 / 跟进 弹确认 |
| N32 | 打开页面、刷新、点普通事项不产生 Toast | 到处弹提示 |

### 业务规则

| # | 规则 | 怎么判定违反 |
|---|---|---|
| N33 | 前端不得推导业务规则 | 自行算逾期 / 为什么进 Today / 等待天数 / 今天是哪天 |
| N34 | 状态变更只能 `POST /items/{id}/transitions` | 出现带 `status` 的 `PUT /items/{id}` |
| N35 | 不做乐观更新 | 先改本地状态再等服务器；失败后 UI 停在成功态 |
| N36 | 刷新不得覆盖用户正在编辑的输入 | 编辑框内容被 GET 结果重写 |
| N37 | 不得静默丢弃未保存内容 | 关闭 / 切日期 / 离开页面不确认也不保存 |
| N38 | ID 一律 `string` | `parseInt(id)` / `Number(id)` |
| N39 | 不得在页面里直接调 axios | `axios.` 出现在 `src/pages/**`、`src/components/**` |
| N40 | 界面不出现技术枚举 | 用户可见文案里有 `INBOX / TODO / DOING / WAITING / CANCELLED` |
| N41 | 不得擅自换技术栈 | 引入 React / Tailwind / Element Plus / 图表库 |

---

## 2. 视觉与布局契约

### 2.1 令牌（唯一来源 `src/styles/main.css`）

```text
主色      #4F46E5 · hover #4338CA · 浅底 #EEF2FF
文字      #1F2329 / #646A73 / #8F959E
边框      #E5E6EB / #F0F1F3
背景      #F7F8FA（页面） · #FFFFFF（工作区） · #F5F7FA（hover）
语义      success #16A34A · warning #D97706 · danger #DC2626 · info #2563EB
字号      标题 24/28/600 · 区块 15/22/600 · 事项 14/20/500 · 正文 14/22/400 · 元信息 12/18/400
间距      4px 栅格；页面 32 · 区块 28 · 区块标题到内容 12 · 事项行内边距 10
圆角      控件 6 · Popover 8 · Drawer/Modal 10 · 事项行/区块/页面 0
```

主色必须通过 `a-config-provider` 的 `token.colorPrimary` 注入（否则 antd 组件仍是默认蓝），CSS 变量只是给自定义样式用。

### 2.2 骨架

```text
Sidebar 208px → Main（内容 max 1200px 居中）
今天页：左主区 ~820px + 右栏 ~300px，间距 32px
```

### 2.3 状态与优先级

```text
状态符号  ○ 待办/待整理 · ◐ 进行中 · ◷ 等待中 · ✓ 已完成 · × 已取消
         符号必须有 aria-label + title（用户语言）
优先级    ● P0（danger）· ● P1（warning）· P2/P3 弱化，不加圆点
行样式    默认透明 · hover #F5F7FA · selected #EEF2FF + 左侧 2px 主色条
          focus outline: 2px solid rgba(79,70,229,.25)
```

---

## 3. ItemRow 与 Drawer 契约

### 3.1 ItemRow（`src/components/ItemCard.vue`）

```text
第一行  [符号] [标题]                 [项目] [日期] [优先级] [行内动作]
第二行  最多两条事实：等待 / 跟进 / 逾期 / 剩余步数（都没有才退化为描述摘要）
第三行  页面注入的 rowDetail（收件箱整理面板）
```

- 展示组件：不改状态机、不判断逾期、不决定 Today 归属。
- 行内动作常驻 DOM、默认 `opacity: 0`，hover / `:focus-within` / 选中时出现。
- 行内动作：完成（含 `DONE`）、开始/恢复（含 `DOING`）、重新打开（已完成且含 `TODO`）、等待（含 `WAITING`）、跟进（到点或等待中）、安排今天（页面传 `decisionMode`）。
- 提交中只让被点控件 loading，同组其他按钮禁用。

### 3.2 Drawer（`src/components/ItemDrawer.vue`）

```text
宽度 540px
头部   事项标题 + 「项目 · 优先级 · 状态」
正文   标题（可编辑）→ 事实行 → 等待与跟进 → 描述 → 我在等谁 → 到点跟进 → 记一句 → 历史 → 删除
底部   随状态变化的主要动作：待办/进行中→完成事项 · 等待中→恢复处理 · 已完成→重新打开
```

- 不显示 ID / createdAt / version / source。
- 属性即时保存；标题描述失焦保存；保存中禁用状态按钮，避免抢 `version`。
- 刷新事实不得覆盖正在编辑的文本；关闭不丢输入。
- 取消、删除必须二次确认。

---

## 4. 页面契约

### 4.1 今天 `/today`

```text
Header  今天 · 9月26日 · 星期六 · [继续上次]
左主区  今日计划（N/3）→ 今天别忘了（逾期→今日到期→待跟进）→ 还没安排（最多 5 条，行内「安排今天」）
右栏    等待中 + 最近工作（不是 KPI）
```

- 顺序不可调换；每条都要能解释为什么入选。
- 排序由后端给，前端不重排。
- 逾期用 danger 文字/符号，不整行变红。

### 4.2 收件箱 `/inbox`

```text
收件箱 · N 个待处理
记录一个新的事情… Enter ↵
[先都收下]
○ 标题                    10:32    整理
```

- 快速记录只要标题。
- 行内「整理」原位展开，**只允许**：项目、优先级、收下、不做、打开详情；默认收起，一次只展开一条。
- 收下 → 转 TODO；不做 → 转已取消（二次确认）；先都收下 → 批量转 TODO，不得自动填项目/优先级/日期。
- **排期不放进收件箱。**

### 4.3 全部事项 `/items`

- 默认「未完成」；筛选：状态 / 优先级 / 项目 + 搜索。
- 行内可直接完成 / 开始 / 等待 / 跟进。

### 4.4 项目 `/projects`、`/projects/:id`

```text
列表：项目名 + 状态 / 当前：xxx / 进行中 N · 等待 N · 最近活动
详情：标题+状态 → 当前重点 / 当前进展 / 下一步 → 阻塞 / 目标 → 联系人 / 链接 / 备注
      → 进行中（可操作列表）→ 等待中 → 归档
```

- 第一屏是现状字段，不是数字。
- 不做 Kanban / Sprint / Burn-down / Story Point / Velocity / 完成率。

### 4.5 日报 `/daily`

- `← 日期 →` 翻页；生成只产生快照，「应用」才覆盖正文。
- 有未保存修改时切日期或离开必须先确认。

---

## 5. 反馈契约

```text
空态    直接说明为什么为空 + 下一步动作，不用插画
加载    很快→直接展示 · 稍慢→Skeleton（模拟真实布局）· 首屏→允许整块 loading · 后续→只允许局部 loading
错误    「发生了什么 + 现在怎么办」，统一 fail(error)
Toast   只有完成 / 归档 / 取消 / 批量变更使用，带撤销；打开页面、刷新、点普通事项不弹
动画    hover 100~120ms · button 100~150ms · popover 120~160ms · drawer 160~200ms
        禁止列表错峰入场；支持 prefers-reduced-motion
```

---

## 6. 工程契约

```text
数据流   页面 → api/index.ts → api/client.ts → Axios（页面不直接调 axios）
错误     统一 fail(error)
ID       string，禁止 parseInt / Number
时间     dayjs(iso) 展示；提交 toISOString()；纯日期 YYYY-MM-DD；禁止 iso.substring
状态     MVP 不用 Pinia 存页面数据
SFC      <script setup lang="ts"> + <template> + <style scoped>
复用     重复两次先观察，三次且语义稳定才抽；只抽行为，不抽「Base 壳子」
组件结构 components（ItemCard / ItemDrawer / PageHeader）+ composables（useListActions）
```

---

## 7. 验收

### 7.1 静态自检（在 `workos-web/` 下执行）

```bash
npx vue-tsc --noEmit     # 不得新增类型错误
npx vite build           # 必须通过
```

基线：改动前已知 2 个历史类型错误（`src/api/client.ts` 的拦截器签名、`vite.config.ts` 缺 node 类型）。只能减少，不能增加。

应输出为空：

```bash
rg -n "axios\." src/pages src/components          # N39
rg -n "linear-gradient|backdrop-filter" src       # N1 N2
rg -n "parseInt\(|Number\([^)]*[Ii]d\b" src       # N38
```

需要逐条人工确认：

```bash
rg -n "transition\(" src/pages src/components     # 状态变更唯一入口，且带 version
rg -n "updateItem\(" src/pages src/components     # 确认没有传 status；ItemDrawer 的 patch(payload) 要额外确认
rg -n "'(INBOX|TODO|DOING|WAITING|DONE|CANCELLED)'" src/pages src/components
#  只允许：类型映射表 key、筛选器定义、与 allowedTransitions 的比较
rg -n "border-radius" src/styles/main.css         # 事项行/区块必须 0
rg -n "box-shadow" src/styles/main.css
#  唯一允许：.item-row--selected 的 inset 2px 0 0 左侧色条
#  其他位置出现即违反 N3
```

### 7.2 浏览器验收

1. 每个路由收集 `console error / pageerror / HTTP ≥ 400` —— 必须为 0。
2. 检查 `1280×800`、`1440×900`、`1728×1117`，以及 100% / 125% / 150% 缩放；不得有横向滚动。
3. 主要流程真的点一遍（用临时数据，结束必须清理）。
4. 检查：行内动作可用、点击瞬间列表不清空、动作后焦点仍在列表、抽屉输入不被刷新覆盖、关抽屉不丢输入。
5. 检查长标题、大量数据、空态、错误态。

### 7.3 提交前必答

```text
1. 3 秒内知道现在该做什么吗？
2. 能不能快速扫描 10 个 Item？
3. 完成一个 Item 够快吗？
4. 是否必须打开 Drawer 才能完成简单操作？→ 是就返工
5. 离开项目两天后回来，能快速恢复上下文吗？
6. 页面有没有明显的「后台系统感」？
```

---

## 8. 后端硬约束（不要试图绕过）

```text
状态机由后端决定，例如 DONE → CANCELLED 不允许；完成后的撤销只能回到「待办」
只有「待整理」状态可以物理删除（否则 code 3005，提示改用取消）
接口用 HTTP 200 + body.code 表示业务结果，code != 0 视为失败
```

前端不因这些约束放宽校验或伪造状态；需要更细的语义时找后端加，不要在前端补。

---

## 9. 实施顺序

```text
Phase 1 设计基础（颜色 / 字体 / 间距 / 圆角 / 阴影 / 控件 / Focus / 布局 / Sidebar）
Phase 2 Item 系统（ItemRow / 行内动作 / Drawer / 状态 / 优先级）
Phase 3 今天（信息层级、密度、工作流）
Phase 4 收件箱（Capture、Triage、Inline Action）
Phase 5 项目（List、Context、Active Work、Waiting、Recent Activity）
Phase 6 全部事项 / 日报
```

不要一上来同时改所有页面。每个阶段都要在浏览器里截图检查，不能只看构建通过。
