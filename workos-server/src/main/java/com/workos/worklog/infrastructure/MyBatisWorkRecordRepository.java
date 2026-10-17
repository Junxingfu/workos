package com.workos.worklog.infrastructure;

import com.workos.worklog.domain.WorkRecord;
import com.workos.worklog.domain.WorkRecordRepository;
import com.workos.worklog.infrastructure.persistence.entity.WorkLogPO;
import com.workos.worklog.infrastructure.persistence.mapper.WorkLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 工作记录存储适配器；不会根据当前关联项目复制记录。 */
@Repository
@RequiredArgsConstructor
public class MyBatisWorkRecordRepository implements WorkRecordRepository {
  private final WorkLogMapper mapper;

  public long append(WorkRecord record) {
    WorkLogPO po =
        WorkLogPO.builder()
            .itemId(record.itemId())
            .projectId(record.projectId())
            .workDate(record.workDate())
            .occurredAt(record.occurredAt())
            .content(record.content())
            .build();
    mapper.insert(po);
    return po.getId();
  }
}
