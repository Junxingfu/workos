package com.workos.daily.domain;

import java.time.LocalDate;

/** DailyReport 聚合仓储；领域声明存取语义，基础设施实现版本条件写入。 */
public interface DailyReportRepository {
  /** 按业务日期读取聚合；不存在时返回 null，由用例决定创建或报错。 */
  DailyReport find(LocalDate key);

  /** 插入或按旧版本更新整个聚合；成功后回填身份和新版本。 */
  void save(DailyReport aggregate);
}
