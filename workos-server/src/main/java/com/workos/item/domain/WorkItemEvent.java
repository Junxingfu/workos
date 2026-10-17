package com.workos.item.domain;

import java.time.Instant;
import java.time.LocalDate;

/** 已经发生的业务事实；不携带数据库活动类型，不代表消息已向外部发布。 */
public sealed interface WorkItemEvent {
  long itemId();

  Instant at();

  record Created(long itemId, Instant at) implements WorkItemEvent {}

  record DetailsRevised(long itemId, Instant at) implements WorkItemEvent {}

  record StatusChanged(long itemId, ItemStatus from, ItemStatus to, String remark, Instant at)
      implements WorkItemEvent {}

  record WaitingEnded(long itemId, ItemStatus to, long days, Instant at) implements WorkItemEvent {}

  record PlanChanged(long itemId, LocalDate previousDate, LocalDate date, Instant at)
      implements WorkItemEvent {}

  record FollowUpRecorded(long itemId, FollowUpOutcome outcome, Instant nextAt, Instant at)
      implements WorkItemEvent {}

  record TrackingRevised(long itemId, Instant at) implements WorkItemEvent {}
}
