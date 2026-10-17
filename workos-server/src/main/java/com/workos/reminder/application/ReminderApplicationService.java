package com.workos.reminder.application;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.reminder.domain.Reminder;
import com.workos.reminder.domain.ReminderRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 手动提醒的应用用例；提醒事实不回写事项状态。 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReminderApplicationService {
  private final ReminderRepository repository;
  private final TimeZoneHolder clock;

  /** 创建手动提醒并持久化；返回提醒身份。 */
  public long schedule(String kind, Long itemId, String title, Instant at) {
    Reminder reminder = Reminder.schedule(kind, itemId, title, at, clock.now());
    repository.save(reminder);
    return reminder.snapshot().id();
  }

  /** 加载提醒并由聚合检查可否改期，再保存。 */
  public void reschedule(long id, Instant at) {
    Reminder reminder = require(id);
    reminder.reschedule(at);
    repository.save(reminder);
  }

  /** 处理提醒并保存确认时间，不回写事项状态。 */
  public void dismiss(long id) {
    Reminder reminder = require(id);
    reminder.dismiss(clock.now());
    repository.save(reminder);
  }

  private Reminder require(long id) {
    Reminder reminder = repository.find(id);
    if (reminder == null) {
      throw new BizException(ResponseCode.REMINDER_NOT_FOUND);
    }
    return reminder;
  }
}
