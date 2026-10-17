package com.workos.daily.application.query;

import com.workos.daily.application.readmodel.DailyResp;
import java.time.LocalDate;
import java.util.List;

/** Daily 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface DailyQuery {
  /** 读取从指定日期开始的七天已有日报，不创建草稿。 */
  List<DailyResp> week(LocalDate date);
}
