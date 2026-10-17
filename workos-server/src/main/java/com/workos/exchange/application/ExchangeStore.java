package com.workos.exchange.application;

import java.time.Instant;
import java.util.List;

/** 批量交换的查询和幂等账本端口；任务写入必须经过事项用例。 */
public interface ExchangeStore {
  record ProjectRef(Long id, String name, String status) {
    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getStatus() {
      return status;
    }
  }

  record Link(Long itemId, Long projectId) {
    public Long getItemId() {
      return itemId;
    }

    public Long getProjectId() {
      return projectId;
    }
  }

  List<TaskSnapshot> tasks();

  TaskSnapshot task(long id);

  List<ProjectRef> projects();

  List<Link> links(List<Long> ids);

  String completed(String fingerprint);

  void record(String fingerprint, String result, Instant completedAt);
}
