package com.workos.item.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 修改意图：null 保留，clear 标记显式清空，避免部分更新丢失数据。 */
public record ItemRevision(
    String title,
    String description,
    Long projectId,
    List<Long> relatedProjects,
    boolean clearProject,
    ItemType type,
    ItemPriority priority,
    LocalDate plannedDate,
    Instant dueAt,
    boolean clearDue,
    String waitingFor,
    Instant followUpAt,
    boolean clearFollowUp,
    String relatedPerson) {}
