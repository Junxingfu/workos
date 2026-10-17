package com.workos.item.infrastructure;

import com.workos.item.application.FollowUpJournal;
import com.workos.worklog.application.WorkLogApplicationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 任务执行到人工记录的本地适配器；复用记录用例，并参与同一个事务。 */
@Component
@RequiredArgsConstructor
public class MyBatisFollowUpJournal implements FollowUpJournal {
  private final WorkLogApplicationService records;

  public void append(long itemId, String content, Instant occurredAt) {
    records.record(itemId, null, null, occurredAt, content);
  }
}
