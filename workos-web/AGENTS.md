# WorkOS 前端开发入口

继承根目录 AGENTS.md。前端规范唯一来源：`../docs/开发规范.md`；产品和交互语义见 `../docs/产品与交互.md`。

- Vue 3 + TypeScript + Vite + Ant Design Vue；使用现有设计令牌与紧凑列表。
- 页面 → api/index.ts → api/client.ts；不在页面直接使用 axios。
- 后端是状态、Today 分区、等待天数、逾期和业务日的事实源；流转按钮来自 allowedTransitions。
- 高频动作可见且行内可用，详细上下文进入 Drawer；保留未保存输入。
- 所有 ID 用 string；写请求带 version，成功后重新获取事实。
- `npm run build` 必须通过；受影响流程用真实后端在浏览器验证，包括窄桌面、长标题、错误与空态。
- 丰富测试数据可以保留，但示例工作区必须与工作数据隔离并可识别。
