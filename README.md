# WorkOS

个人工作上下文管理与事项追踪工具（MVP）。

一句话定位：把"我答应过什么、我在等谁、我做到哪了"变成每天打开就能看清的东西。

## 仓库结构

```
workos/
├── workos-server/      后端（Spring Boot 3 + MyBatis-Plus + PostgreSQL）
├── workos-web/         前端（Vite + Vue3 + TS + Pinia）
├── deploy/db/          数据库初始化脚本
├── docs/               文档（运维与部署手册等）
└── docker-compose.yml  本地 PostgreSQL
```

- 后端 / 数据库 / 部署 / 排障 / 备份恢复 / 验收清单：[docs/运维与部署手册.md](docs/运维与部署手册.md)
- 前端启动 / 构建 / Nginx 部署 / 故障排查：[docs/前端运维手册.md](docs/前端运维手册.md)
- 前端技术选型 / 分层 / 设计决策 / 扩展指引：[docs/前端架构说明.md](docs/前端架构说明.md)
- 产品需求：[docs/1-WorkOS MVP 产品需求文档.md](docs/1-WorkOS%20MVP%20产品需求文档.md)
- 本次可用性评估、规则修订与验收：[docs/改造评估与验收.md](docs/改造评估与验收.md)
- 前端视觉与交互规范：`workos-web/AGENTS.md` 与 `docs/archive/前端规范.md`

## 技术栈与约束

后端：

- Java 17、Spring Boot 3.2.4、MyBatis-Plus 3.5.7、MapStruct 1.5.5.Final、Lombok
- 统一响应 `BaseResponse<T>`，分页 `BasePageResult<T>`（total / pageNo / pageSize / dataList）
- 目录分层：`controller` / `service` + `service.impl` / `converter` / `data.entity`（PO）/ `data.mapper` / `model.request`（Req）/ `model.response`（Resp）/ `model.dto`（DTO）
- 类注释统一 `@Class / @Author / @Date / @Desc`，方法注释统一 `@Method / @Param / @Return / @Desc`

不引入：Redis、MQ、WebSocket、微服务、AI、Tag、子任务、Calendar、图片上传。

## 产品模型（冻结基线；本次修订见改造评估与验收）

```
Item 类型只有两种：TASK / NOTE
Follow-up 是能力（next_follow_up_at 非空），不是类型
Waiting 是状态，Reminder 是独立对象
```

状态机（唯一状态变更入口 `POST /api/items/{id}/transitions`）：

```
INBOX            → TODO / CANCELLED
TODO / DOING / WAITING → DOING / TODO / WAITING / DONE / CANCELLED（按表判定）
DONE / CANCELLED → TODO
NOTE 只允许 INBOX / DONE / CANCELLED
```

Today 六分区按 ①→⑥ 顺序判断，命中即停止，保证互斥：

```
① Plan            planned_date = 今天 且未完成
② Overdue         due_at < 今天起始 且 status in (TODO, DOING) 且无计划日期
③ Today           due_at 在今天 且 status not in (DONE, CANCELLED, WAITING) 且无计划日期
④ FollowUp        next_follow_up_at <= now 且未完成 且无计划日期
⑤ Waiting         status = WAITING 且未到跟进时间 且无计划日期
⑥ PendingDecision 无计划日期 且 status not in (DONE, CANCELLED, WAITING) 且 priority in (P0, P1)，上限 5
```

界面上归成三块：今天做什么 / 今天别忘了 / 我在等谁 + 还没安排。

## 本地启动

```bash
# 1. 起数据库（首次会自动执行 deploy/db/01-init.sql）
docker compose up -d

# 2. 起后端
cd workos-server && mvn spring-boot:run

# 3. 起前端
cd workos-web && npm install && npm run dev
```

后端默认 `http://localhost:8080`，Swagger 在 `/swagger-ui.html`；前端默认 `http://localhost:5173`，`/api` 由 Vite 代理到后端。

前端页面：`/today`（今天做什么 / 今天别忘了 / 我在等谁 + 还没安排）、`/inbox`（待整理）、`/projects`（项目与项目现状）、`/daily`（日报）。今天页输入标题并回车即可直接加入今日计划；全局「记录事项」可选今天做、待办、待整理，`⌘/Ctrl + Shift + K` 打开。全部事项支持行内「安排今天」。

数据库连接可用环境变量覆盖：`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` / `SERVER_PORT`。

## 接口清单（MVP）

```
GET    /api/today                        Today 聚合（六分区 + 计数）
POST   /api/today/plan/reorder           调整今日计划顺序

GET    /api/inbox                        待整理列表（含 oldestDays）
POST   /api/inbox/accept-all             先都收下（保留已有项目与优先级；任务转 TODO，笔记归档）

GET    /api/items                        全部事项分页
POST   /api/items                        创建事项
POST   /api/items/quick-capture          快速记录
GET    /api/items/{id}                   详情
PUT    /api/items/{id}                   属性修改（不允许改状态）
DELETE /api/items/{id}                   删除（仅未整理）
POST   /api/items/{id}/transitions       状态转换（唯一入口）
POST   /api/items/{id}/follow-up-actions 跟进动作三选一
POST   /api/items/{id}/plan              加入计划
DELETE /api/items/{id}/plan              移出计划
GET    /api/items/{id}/activities        系统事件

GET    /api/projects                     项目分页
POST   /api/projects                     创建项目
GET    /api/projects/{id}                详情（含现状 + 统计）
PUT    /api/projects/{id}                修改
POST   /api/projects/{id}/archive        归档（不做物理删除）
POST   /api/projects/{id}/restore        恢复
GET    /api/projects/{id}/context        项目现状
PUT    /api/projects/{id}/context        更新项目现状

POST   /api/work-logs                    记一句
GET    /api/work-logs                    查询（itemId / projectId / workDate）

GET    /api/dailies?weekStart=           本周只读聚合
GET    /api/dailies/{date}               获取（首次自动创建）
PUT    /api/dailies/{date}               更新
POST   /api/dailies/{date}/generate      生成快照
POST   /api/dailies/{date}/apply-generated 确认后应用

POST   /api/reminders                    创建提醒
PUT    /api/reminders/{id}               稍后提醒（改 remindAt）
POST   /api/reminders/{id}/dismiss       忽略这次
GET    /api/reminders/pending            启动补偿清单

GET    /api/settings                     获取设置
PUT    /api/settings                     更新设置

GET    /api/search                       全局搜索（q / types / 分页）
GET    /api/follow-ups                   等待与跟进工作台聚合
```

## 关键实现约定

- 时间：数据库统一存 `TIMESTAMPTZ`（UTC），业务日期按 `settings.timezone` 计算；`day_boundary_hour` 默认 0（自然日）
- 时区唯一真源：`settings` 表 + `TimeZoneHolder`（启动加载，PUT 后立即生效）
- 乐观锁：可编辑实体带 `version`，PUT / apply 冲突返回版本冲突错误码
- 空值语义：`PUT` 中 null 表示"不修改"；清空跟进时间用 `clearNextFollowUp = true`
- Daily 的 `generated_content` 是快照，不覆盖 `content`；用户点"应用"后才覆盖
- Reminder 三态 `PENDING / DISMISSED / CANCELLED`，无 `FIRED`；MVP 后端不做调度，由客户端驱动
- WorkLog 无 `source`；系统状态变化只写 `item_activity`，不进日报

## 与文档 6 的一处偏差（已决策）

文档 6 的 `ApiResponse.code` 是字符串错误码（`ITEM_NOT_FOUND`）。本仓库按既有工程风格统一为
`BaseResponse<Integer code>` + `ResponseCode` 枚举，`0` 表示成功；错误码含义与文档 6 一一对应
（见 `common/enums/ResponseCode.java`）。如需严格对齐文档 6 的字符串错误码，改 `BaseResponse.code` 类型即可，
业务层不受影响。

## 备份

```bash
docker exec workos-postgres pg_dump -U workos workos | gzip > backup_$(date +%F).sql.gz
```
