package com.workos.item.application;

import java.time.Instant;

/** 跟进用例所需的记录端口；实现负责按当时主项目归集，领域不依赖工作记录表。 */
public interface FollowUpJournal {
  void append(long itemId, String content, Instant occurredAt);
}
