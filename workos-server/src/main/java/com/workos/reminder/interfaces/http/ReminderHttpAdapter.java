package com.workos.reminder.interfaces.http;

import com.workos.reminder.application.ReminderApplicationService;
import com.workos.reminder.application.query.ReminderQuery;
import com.workos.reminder.application.readmodel.ReminderResp;
import com.workos.reminder.interfaces.request.ReminderCreateReq;
import com.workos.reminder.interfaces.request.ReminderUpdateReq;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reminder 接口适配器：转换输入并调用用例，读取由查询投影负责。 */
@Component
@RequiredArgsConstructor
public class ReminderHttpAdapter {
  private final ReminderQuery queries;
  private final ReminderApplicationService commands;

  /** 创建提醒 */
  public Long create(ReminderCreateReq req) {
    return commands.schedule(req.getKind(), req.getItemId(), req.getTitle(), req.getRemindAt());
  }

  /** 稍后提醒 */
  public ReminderResp reschedule(Long id, ReminderUpdateReq req) {
    commands.reschedule(id, req.getRemindAt());
    return queries.detail(id);
  }

  /** 忽略这次 */
  public ReminderResp dismiss(Long id) {
    commands.dismiss(id);
    return queries.detail(id);
  }

  /** 启动补偿：PENDING 且到点的提醒 */
  public List<ReminderResp> pending() {
    return queries.pending();
  }
}
