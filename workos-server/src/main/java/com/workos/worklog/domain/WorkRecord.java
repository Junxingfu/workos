package com.workos.worklog.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** 不可变人工工作事实；归属冻结在记录发生时，不随任务换项目重写。 */
public record WorkRecord(
    Long itemId, Long projectId, LocalDate workDate, Instant occurredAt, String content) {
  public WorkRecord {
    Objects.requireNonNull(workDate, "工作日期不能为空");
    Objects.requireNonNull(occurredAt, "发生时间不能为空");
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("工作记录不能为空");
    }
    content = content.trim();
  }
}
