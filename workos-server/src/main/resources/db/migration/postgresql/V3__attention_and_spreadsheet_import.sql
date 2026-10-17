-- 通知阅读状态独立于任务；时间用 ISO Instant 文本，两个数据库保持一致。
CREATE TABLE IF NOT EXISTS attention_receipt (
    notification_key VARCHAR(240) PRIMARY KEY,
    read_at VARCHAR(40),
    snoozed_until VARCHAR(40)
);
-- 同一批 Excel 的重试不重复创建任务；结果和业务写入同事务提交。
CREATE TABLE IF NOT EXISTS spreadsheet_import (
    fingerprint VARCHAR(64) PRIMARY KEY,
    completed_at VARCHAR(40) NOT NULL,
    result_json TEXT NOT NULL
);
