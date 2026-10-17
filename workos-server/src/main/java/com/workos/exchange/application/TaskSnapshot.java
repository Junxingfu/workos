package com.workos.exchange.application;

import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemSource;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Value;

/** 表格边界的只读快照，不能作为聚合保存。 */
@Value
public class TaskSnapshot {
  Long id;
  Long projectId;
  String title;
  String description;
  ItemType itemType;
  ItemStatus status;
  ItemPriority priority;
  LocalDate plannedDate;
  Integer planRank;
  Instant dueAt;
  Instant nextFollowUpAt;
  String waitingFor;
  Instant waitingSince;
  Instant waitingExpectedAt;
  String relatedPerson;
  Instant completedAt;
  Instant cancelledAt;
  ItemSource source;
  Instant createdAt;
  Instant updatedAt;
  Integer version;
}
