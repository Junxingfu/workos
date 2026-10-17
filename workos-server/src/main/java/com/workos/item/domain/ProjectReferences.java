package com.workos.item.domain;

import java.util.List;
import java.util.Objects;

/** 项目归属值对象：仅持有项目标识，不把项目聚合或其他项目任务装进事项。 */
public record ProjectReferences(Long mainProjectId, List<Long> relatedProjectIds) {
  public ProjectReferences {
    List<Long> requested = relatedProjectIds == null ? List.<Long>of() : relatedProjectIds;
    if (requested.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("关联项目不能为空");
    }
    relatedProjectIds =
        requested.stream().filter(id -> !id.equals(mainProjectId)).distinct().toList();
    if (relatedProjectIds.size() > 20) {
      throw new IllegalArgumentException("关联项目最多 20 个");
    }
    if (mainProjectId == null && !relatedProjectIds.isEmpty()) {
      throw new IllegalArgumentException("请先选择主项目；清空主项目前请先移除关联项目");
    }
  }
}
