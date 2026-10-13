# WorkOS

个人工作上下文与事项追踪工具：记录要做的事，安排今天，跟进等待，恢复项目上下文，沉淀工作记录与日报。

## 文档

统一入口：[docs/README.md](docs/README.md)。只有产品与交互、技术设计、开发规范、运行与验收四份标准；历史版本通过 Git 追溯。

## 仓库

| 目录 | 内容 |
|---|---|
| workos-web | Vue 3 / TypeScript / Vite / Ant Design Vue |
| workos-server | Java 17 / Spring Boot 3 / MyBatis-Plus |
| deploy/db | PostgreSQL 初始化、增量升级、独立示例数据 |
| docs | 当前产品与开发上下文 |

## 本地开始

macOS 推荐在根目录执行 `./scripts/workos start`，由系统当前登录会话托管前后端，关闭终端后继续运行。Docker/Colima 需先启动；脚本会启动 PostgreSQL 容器、检查库结构并打包后端。

`./scripts/workos status` 查看状态，`./scripts/workos logs` 查看日志，`./scripts/workos stop` 停止前后端；修改后端代码后用 `./scripts/workos restart` 重新打包。重启电脑或退出登录后再次 start。

访问 `http://localhost:5173`。已有数据库升级、独立示例工作区和测试方法见[运行与验收](docs/运行与验收.md)。

今天页一句话可直接加入计划；全局“记录事项”支持今天做、待办、待整理，快捷键为 `⌘/Ctrl + Shift + K`。事项支持一个主项目和多个关联项目，共享进度；详情中的状态流转辅助图可直接执行下一步。
