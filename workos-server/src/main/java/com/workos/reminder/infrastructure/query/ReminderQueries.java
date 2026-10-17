package com.workos.reminder.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.reminder.application.query.ReminderQuery;
import com.workos.reminder.application.readmodel.ReminderResp;
import com.workos.reminder.infrastructure.mapping.ReminderConverter;
import com.workos.reminder.infrastructure.persistence.entity.ReminderPO;
import com.workos.reminder.infrastructure.persistence.mapper.ReminderMapper;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reminder 专用查询投影；只读 SQL 与视图装配独立于聚合写入。 */
@Service
@RequiredArgsConstructor
public class ReminderQueries implements ReminderQuery {

  private static final String STATUS_PENDING = "PENDING";

  private static final String STATUS_DISMISSED = "DISMISSED";

  private static final String KIND_ITEM = "ITEM";

  private final ReminderMapper reminderMapper;

  private final ReminderConverter reminderConverter;

  private final TimeZoneHolder timeZoneHolder;

  public List<ReminderResp> pending() {
    Instant now = timeZoneHolder.now();
    List<ReminderPO> poList =
        reminderMapper.selectList(
            new LambdaQueryWrapper<ReminderPO>()
                .eq(ReminderPO::getStatus, STATUS_PENDING)
                .le(ReminderPO::getRemindAt, now)
                .orderByAsc(ReminderPO::getRemindAt));
    return poList.stream().map(reminderConverter::toResp).toList();
  }

  public ReminderPO requireReminder(Long id) {
    ReminderPO po = Objects.isNull(id) ? null : reminderMapper.selectById(id);
    if (Objects.isNull(po)) {
      throw new BizException(ResponseCode.REMINDER_NOT_FOUND);
    }
    return po;
  }

  public ReminderResp detail(Long id) {
    return reminderConverter.toResp(requireReminder(id));
  }
}
