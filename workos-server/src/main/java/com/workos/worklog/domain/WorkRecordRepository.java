package com.workos.worklog.domain;

/** 只追加人工事实；查询投影独立实现。 */
public interface WorkRecordRepository {
  /** 追加不可变人工事实并返回记录 ID；不得改写历史归属。 */
  long append(WorkRecord record);
}
