package com.workos.settings.infrastructure;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.settings.domain.WorkspaceSettings;
import com.workos.settings.domain.WorkspaceSettingsRepository;
import com.workos.settings.infrastructure.persistence.entity.SettingsPO;
import com.workos.settings.infrastructure.persistence.mapper.SettingsMapper;
import com.workos.shared.domain.ConcurrentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** WorkspaceSettings 的 MyBatis-Plus 适配器；显式写 null，防止清空被 ORM 的非空策略忽略。 */
@Repository
@RequiredArgsConstructor
public class MyBatisWorkspaceSettingsRepository implements WorkspaceSettingsRepository {
  private final SettingsMapper mapper;

  @Override
  public WorkspaceSettings find(Long key) {
    SettingsPO entity = mapper.selectById(key);
    return entity == null
        ? null
        : new WorkspaceSettings(
            new WorkspaceSettings.State(
                entity.getId().longValue(),
                entity.getTimezone(),
                entity.getDateFormat(),
                entity.getWeekStart(),
                entity.getDayBoundaryHour(),
                entity.getMorningBriefTime(),
                entity.getEodReviewTime(),
                entity.getDesktopNotification(),
                entity.getQuickCaptureShortcut(),
                entity.getUpdatedAt(),
                entity.getVersion()));
  }

  @Override
  public void save(WorkspaceSettings aggregate) {
    WorkspaceSettings.State state = aggregate.snapshot();
    SettingsPO entity = new SettingsPO();
    entity.setId(state.id().shortValue());
    entity.setTimezone(state.timezone());
    entity.setDateFormat(state.dateFormat());
    entity.setWeekStart(state.weekStart());
    entity.setDayBoundaryHour(state.dayBoundaryHour());
    entity.setMorningBriefTime(state.morningBriefTime());
    entity.setEodReviewTime(state.eodReviewTime());
    entity.setDesktopNotification(state.desktopNotification());
    entity.setQuickCaptureShortcut(state.quickCaptureShortcut());
    entity.setUpdatedAt(state.updatedAt());
    entity.setVersion(state.version());
    if (state.id() == null) {
      mapper.insert(entity);
      aggregate.persisted(entity.getId().longValue(), entity.getVersion());
      return;
    }
    LambdaUpdateWrapper<SettingsPO> update =
        new LambdaUpdateWrapper<SettingsPO>()
            .eq(SettingsPO::getId, state.id())
            .eq(SettingsPO::getVersion, state.version())
            .set(SettingsPO::getVersion, state.version() + 1)
            .set(SettingsPO::getTimezone, state.timezone())
            .set(SettingsPO::getDateFormat, state.dateFormat())
            .set(SettingsPO::getWeekStart, state.weekStart())
            .set(SettingsPO::getDayBoundaryHour, state.dayBoundaryHour())
            .set(SettingsPO::getMorningBriefTime, state.morningBriefTime())
            .set(SettingsPO::getEodReviewTime, state.eodReviewTime())
            .set(SettingsPO::getDesktopNotification, state.desktopNotification())
            .set(SettingsPO::getQuickCaptureShortcut, state.quickCaptureShortcut())
            .set(SettingsPO::getUpdatedAt, state.updatedAt());
    if (mapper.update(null, update) != 1) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
    aggregate.persisted(state.id(), state.version() + 1);
  }
}
