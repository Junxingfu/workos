package com.workos.reminder.application.query;

import com.workos.reminder.application.readmodel.ReminderResp;
import java.util.List;

/** Reminder 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface ReminderQuery {
  /** 读取已到提醒时间且尚未处理的手动提醒。 */
  List<ReminderResp> pending();

  /** 读取手动提醒详情。 */
  ReminderResp detail(Long id);
}
