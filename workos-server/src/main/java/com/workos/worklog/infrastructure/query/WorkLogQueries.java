package com.workos.worklog.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.worklog.application.query.WorkLogQuery;
import com.workos.worklog.application.readmodel.WorkLogResp;
import com.workos.worklog.infrastructure.mapping.WorkLogConverter;
import com.workos.worklog.infrastructure.persistence.entity.WorkLogPO;
import com.workos.worklog.infrastructure.persistence.mapper.WorkLogMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** WorkLog 专用查询投影；只读 SQL 与视图装配独立于聚合写入。 */
@Service
@RequiredArgsConstructor
public class WorkLogQueries implements WorkLogQuery {

  private final WorkLogMapper workLogMapper;

  private final WorkLogConverter workLogConverter;

  public List<WorkLogResp> list(Long itemId, Long projectId, LocalDate workDate) {
    List<WorkLogPO> poList =
        workLogMapper.selectList(
            new LambdaQueryWrapper<WorkLogPO>()
                .eq(Objects.nonNull(itemId), WorkLogPO::getItemId, itemId)
                .eq(Objects.nonNull(projectId), WorkLogPO::getProjectId, projectId)
                .eq(Objects.nonNull(workDate), WorkLogPO::getWorkDate, workDate)
                .orderByDesc(WorkLogPO::getOccurredAt));
    return poList.stream().map(workLogConverter::toResp).toList();
  }
}
