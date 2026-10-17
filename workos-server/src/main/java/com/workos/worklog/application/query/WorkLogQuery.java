package com.workos.worklog.application.query;

import com.workos.worklog.application.readmodel.WorkLogResp;
import java.time.LocalDate;
import java.util.List;

/** WorkLog 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface WorkLogQuery {
  /** 按事项、记录当时主项目或业务日读取人工事实。 */
  List<WorkLogResp> list(Long itemId, Long projectId, LocalDate workDate);
}
