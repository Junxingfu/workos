-- 丰富示例：仅用于独立示例/验收库。重复运行补缺，不清空或覆盖已有体验记录。
BEGIN;
DO $$ BEGIN
    IF current_database() !~ '^workos_(demo|test|usability)' THEN
        RAISE EXCEPTION '示例数据只能写入 workos_demo / workos_test / workos_usability 系列数据库';
    END IF;
END $$;
SET LOCAL TIME ZONE 'Asia/Shanghai';

INSERT INTO project(name, code, description, status, sort_order) VALUES
('基础平台升级', 'DEMO_PLATFORM', '统一组件、网关与公共接口的迭代', 'ACTIVE', 1),
('政务门户上线', 'DEMO_PORTAL', '门户联调、业务验收与上线准备', 'ACTIVE', 2),
('厂商协同接入', 'DEMO_VENDOR', '接入进度与外部依赖跟踪', 'ACTIVE', 3),
('质量与运维', 'DEMO_QUALITY', '回归、监控、文档和发布验证', 'ACTIVE', 4),
('历史一期交付', 'DEMO_ARCHIVED', '演示归档后的上下文保留和恢复', 'ARCHIVED', 5)
ON CONFLICT (code) DO NOTHING;

INSERT INTO project_context(project_id, goal, focus, blocker, next_action, progress_note, key_people, key_links)
SELECT id, '完成联调并形成可复用交付清单',
       CASE code WHEN 'DEMO_PLATFORM' THEN 'code2name 统一升级与兼容回归' WHEN 'DEMO_PORTAL' THEN '登录与办事入口验收' ELSE '核对接入边界与风险' END,
       CASE code WHEN 'DEMO_VENDOR' THEN '等待厂商提供签名样例' ELSE '' END,
       '核对关键事项，确认下一轮联调时间', '主流程已验证，正在收口异常分支', '平台：李工；测试：王工；厂商：张工', '联调说明与验收记录待补充'
FROM project WHERE code LIKE 'DEMO_%'
ON CONFLICT (project_id) DO NOTHING;

WITH demo AS (
  SELECT n, p.id AS project_id,
    '[示例] ' || (ARRAY['校验网关签名兼容性','核对门户登录回调','确认厂商接入清单','补齐上线回滚步骤','复核字典转换结果','整理联调问题结论','确认验收截图与记录','排查慢查询并复测','更新跨模块接口说明','检查异常分支提示'])[(n-1)%10+1] || ' · 第' || n || '项' AS title,
    (ARRAY['DOING','TODO','WAITING','TODO','TODO','WAITING','INBOX','DONE','CANCELLED','TODO'])[(n-1)%10+1] AS status
  FROM generate_series(1, 40) n
  JOIN project p ON p.code = (ARRAY['DEMO_PLATFORM','DEMO_PORTAL','DEMO_VENDOR','DEMO_QUALITY'])[(n-1)%4+1]
)
INSERT INTO item(project_id, title, description, status, priority, planned_date, plan_rank, due_at,
  next_follow_up_at, waiting_for, waiting_since, waiting_expected_at, completed_at, cancelled_at, created_at, updated_at)
SELECT project_id, title,
  E'背景：本轮跨团队联调需要保留关键结论。\n- [x] 确认输入与边界\n- [ ] 验证异常分支\n- [ ] 记录验收结论',
  status, CASE WHEN n%7=0 THEN 'P0' WHEN n%3=0 THEN 'P1' ELSE 'P2' END,
  CASE WHEN n<=3 THEN CURRENT_DATE WHEN n%10=4 THEN CURRENT_DATE-2 WHEN n%10=5 THEN CURRENT_DATE+3 ELSE NULL END,
  CASE WHEN n<=3 THEN n ELSE NULL END,
  CASE WHEN n%10=5 THEN CURRENT_TIMESTAMP-INTERVAL '2 days' WHEN n%10=0 THEN CURRENT_DATE+INTERVAL '17 hours' ELSE NULL END,
  CASE WHEN status='WAITING' AND n%2=1 THEN CURRENT_TIMESTAMP-INTERVAL '2 hours' WHEN status='WAITING' THEN CURRENT_DATE+INTERVAL '1 day 10 hours' ELSE NULL END,
  CASE WHEN status='WAITING' THEN CASE WHEN n%2=1 THEN '厂商张工' ELSE '测试王工' END ELSE NULL END,
  CASE WHEN status='WAITING' THEN CURRENT_TIMESTAMP-INTERVAL '3 days' ELSE NULL END,
  CASE WHEN status='WAITING' THEN CURRENT_DATE+INTERVAL '18 hours' ELSE NULL END,
  CASE WHEN status='DONE' THEN CURRENT_TIMESTAMP-INTERVAL '1 hour' ELSE NULL END,
  CASE WHEN status='CANCELLED' THEN CURRENT_TIMESTAMP-INTERVAL '1 day' ELSE NULL END,
  CURRENT_TIMESTAMP-(n || ' hours')::interval, CURRENT_TIMESTAMP-(n || ' minutes')::interval
FROM demo WHERE NOT EXISTS (SELECT 1 FROM item WHERE item.title=demo.title);

INSERT INTO item(project_id, title, description, status, priority, planned_date)
SELECT id, '[示例] 统一升级 code2name 到 1.0.4', '同一份升级任务，同时支撑门户上线与厂商接入；三个项目共享验收结论。', 'TODO', 'P1', CURRENT_DATE
FROM project WHERE code='DEMO_PLATFORM'
AND NOT EXISTS (SELECT 1 FROM item WHERE title='[示例] 统一升级 code2name 到 1.0.4');

INSERT INTO item_project_link(item_id, project_id)
SELECT i.id, p.id FROM item i CROSS JOIN project p
WHERE i.title='[示例] 统一升级 code2name 到 1.0.4' AND p.code IN ('DEMO_PORTAL','DEMO_VENDOR') AND p.id<>i.project_id
ON CONFLICT DO NOTHING;

INSERT INTO item(project_id, title, description, item_type, status, completed_at)
SELECT id, '[示例] 接入会议结论备忘', '统一使用测试环境签名参数；不要把测试凭据写入工作记录。', 'NOTE', 'DONE', CURRENT_TIMESTAMP
FROM project WHERE code='DEMO_VENDOR'
AND NOT EXISTS (SELECT 1 FROM item WHERE title='[示例] 接入会议结论备忘');

INSERT INTO item(project_id, title, description, status, completed_at)
SELECT id, '[示例] 一期交付验收已完成', '归档项目的已完成事项仍然可查询。', 'DONE', CURRENT_TIMESTAMP-INTERVAL '7 days'
FROM project WHERE code='DEMO_ARCHIVED'
AND NOT EXISTS (SELECT 1 FROM item WHERE title='[示例] 一期交付验收已完成');

INSERT INTO item_activity(item_id, activity_type, to_status, remark, actor, occurred_at)
SELECT id, 'CREATED', status, '示例事项，供流程体验', 'SYSTEM', created_at FROM item i
WHERE title LIKE '[示例]%' AND NOT EXISTS (SELECT 1 FROM item_activity a WHERE a.item_id=i.id);

INSERT INTO work_log(project_id, item_id, work_date, occurred_at, content)
SELECT project_id, id, CURRENT_DATE-(id%7)::int,
       CURRENT_TIMESTAMP-((id%7)::text || ' days')::interval,
       '已核对输入参数和关键异常分支；下一步与对接人确认验收结果。' FROM item i
WHERE title LIKE '[示例]%' AND item_type='TASK' AND status IN ('DOING','WAITING','DONE')
AND NOT EXISTS (SELECT 1 FROM work_log w WHERE w.item_id=i.id);

INSERT INTO daily(daily_date, content)
SELECT CURRENT_DATE-n, '# 工作记录' || E'\n\n' || '- 整理联调问题、确认外部依赖、更新项目现状。'
FROM generate_series(1,6) n ON CONFLICT (daily_date) DO NOTHING;
COMMIT;
