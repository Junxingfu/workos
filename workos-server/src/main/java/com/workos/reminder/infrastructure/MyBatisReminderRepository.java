package com.workos.reminder.infrastructure;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.reminder.domain.Reminder;
import com.workos.reminder.domain.ReminderRepository;
import com.workos.reminder.infrastructure.persistence.entity.ReminderPO;
import com.workos.reminder.infrastructure.persistence.mapper.ReminderMapper;
import com.workos.shared.domain.ConcurrentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Reminder 的 MyBatis-Plus 适配器；显式写 null，防止清空被 ORM 的非空策略忽略。 */
@Repository
@RequiredArgsConstructor
public class MyBatisReminderRepository implements ReminderRepository {
  private final ReminderMapper mapper;

  @Override
  public Reminder find(Long key) {
    ReminderPO entity = mapper.selectById(key);
    return entity == null
        ? null
        : new Reminder(
            new Reminder.State(
                entity.getId(),
                entity.getKind(),
                entity.getItemId(),
                entity.getTitle(),
                entity.getRemindAt(),
                entity.getStatus(),
                entity.getTriggeredAt(),
                entity.getAcknowledgedAt(),
                entity.getCreatedAt(),
                entity.getVersion()));
  }

  @Override
  public void save(Reminder aggregate) {
    Reminder.State state = aggregate.snapshot();
    ReminderPO entity = new ReminderPO();
    entity.setId(state.id());
    entity.setKind(state.kind());
    entity.setItemId(state.itemId());
    entity.setTitle(state.title());
    entity.setRemindAt(state.remindAt());
    entity.setStatus(state.status());
    entity.setTriggeredAt(state.triggeredAt());
    entity.setAcknowledgedAt(state.acknowledgedAt());
    entity.setCreatedAt(state.createdAt());
    entity.setVersion(state.version());
    if (state.id() == null) {
      mapper.insert(entity);
      aggregate.persisted(entity.getId(), entity.getVersion());
      return;
    }
    LambdaUpdateWrapper<ReminderPO> update =
        new LambdaUpdateWrapper<ReminderPO>()
            .eq(ReminderPO::getId, state.id())
            .eq(ReminderPO::getVersion, state.version())
            .set(ReminderPO::getVersion, state.version() + 1)
            .set(ReminderPO::getKind, state.kind())
            .set(ReminderPO::getItemId, state.itemId())
            .set(ReminderPO::getTitle, state.title())
            .set(ReminderPO::getRemindAt, state.remindAt())
            .set(ReminderPO::getStatus, state.status())
            .set(ReminderPO::getTriggeredAt, state.triggeredAt())
            .set(ReminderPO::getAcknowledgedAt, state.acknowledgedAt())
            .set(ReminderPO::getCreatedAt, state.createdAt());
    if (mapper.update(null, update) != 1) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
    aggregate.persisted(state.id(), state.version() + 1);
  }
}
