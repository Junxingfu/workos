package com.workos.worklog.application;

import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.ProjectReferences;
import com.workos.item.domain.WorkItemRepository;
import com.workos.worklog.domain.WorkRecord;
import com.workos.worklog.domain.WorkRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 记录工作用例：以事项主项目作为归集事实，明确拒绝冲突的归属输入。 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkLogApplicationService {
  private final WorkRecordRepository records;
  private final WorkItemRepository items;
  private final TimeZoneHolder clock;

  /** 冻结记录当时的主项目与业务日；拒绝与事项主项目冲突的显式归属。 */
  public long record(Long itemId, Long requestedProject, LocalDate day, Instant at, String text) {
    Long main = requestedProject;
    if (itemId != null) {
      main = items.get(itemId).details().projects().mainProjectId();
      if (requestedProject != null && !requestedProject.equals(main)) {
        throw new IllegalArgumentException("事项工作记录按主项目归集");
      }
    }
    items.requireProjects(new ProjectReferences(main, null));
    Instant occurred = at == null ? clock.now() : at;
    return records.append(
        new WorkRecord(itemId, main, day == null ? clock.toDate(occurred) : day, occurred, text));
  }
}
