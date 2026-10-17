package com.workos.item.domain;

import java.time.Instant;
import java.util.Objects;

/** 事项内容与归属的不可变值；标题必填，项目仅以标识引用。 */
public record ItemDetails(
    String title,
    String description,
    ProjectReferences projects,
    ItemPriority priority,
    Instant dueAt,
    String relatedPerson,
    ItemSource source) {
  public ItemDetails {
    if (title == null || title.isBlank() || title.trim().length() > 500) {
      throw new IllegalArgumentException("标题不能为空且不能超过 500 字");
    }
    title = title.trim();
    if (description != null && description.length() > 20000) {
      throw new IllegalArgumentException("描述不能超过 20000 字");
    }
    projects = Objects.requireNonNullElseGet(projects, () -> new ProjectReferences(null, null));
    priority = Objects.requireNonNullElse(priority, ItemPriority.P2);
    source = Objects.requireNonNullElse(source, ItemSource.MANUAL);
  }
}
