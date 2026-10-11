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

