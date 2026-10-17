package com.workos.worklog.interfaces.http;

import com.workos.worklog.application.WorkLogApplicationService;
import com.workos.worklog.application.query.WorkLogQuery;
import com.workos.worklog.application.readmodel.WorkLogResp;
import com.workos.worklog.interfaces.request.WorkLogCreateReq;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** WorkLog 接口适配器：转换输入并调用用例，读取由查询投影负责。 */
@Component
@RequiredArgsConstructor
public class WorkLogHttpAdapter {
  private final WorkLogQuery queries;
  private final WorkLogApplicationService recordWork;

  /** 创建工作记录 */
  public Long create(WorkLogCreateReq req) {
    return recordWork.record(
        req.getItemId(),
        req.getProjectId(),
        req.getWorkDate(),
        req.getOccurredAt(),
        req.getContent());
  }

  /** 查询工作记录 */
  public List<WorkLogResp> list(Long itemId, Long projectId, LocalDate workDate) {
    return queries.list(itemId, projectId, workDate);
  }
}
