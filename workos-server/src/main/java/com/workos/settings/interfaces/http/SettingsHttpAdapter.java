package com.workos.settings.interfaces.http;

import com.workos.settings.application.SettingsApplicationService;
import com.workos.settings.application.readmodel.SettingsResp;
import com.workos.settings.domain.WorkspaceSettings;
import com.workos.settings.interfaces.request.SettingsUpdateReq;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 设置业务实现：启动即加载时区，保证"今天"口径唯一 */
@Slf4j
@Component
@DependsOn("flyway")
@RequiredArgsConstructor
public class SettingsHttpAdapter {

  /** 单行设置固定主键 */
  private static final Short SINGLE_ROW_ID = 1;

  private final SettingsApplicationService commands;

  /** 启动加载时区，避免"今天"口径与库内配置不一致 */
  @PostConstruct
  public void init() {
    commands.initializeClock();
  }

  /** 获取设置 */
  /** 获取设置 */
  public SettingsResp get() {
    WorkspaceSettings.State state = commands.get();
    SettingsResp result = new SettingsResp();
    result.setTimezone(state.timezone());
    result.setDateFormat(state.dateFormat());
    result.setWeekStart(state.weekStart());
    result.setDayBoundaryHour(state.dayBoundaryHour().intValue());
    result.setMorningBriefTime(state.morningBriefTime());
    result.setEodReviewTime(state.eodReviewTime());
    result.setDesktopNotification(state.desktopNotification());
    result.setQuickCaptureShortcut(state.quickCaptureShortcut());
    return result;
  }

  /** 更新设置：null 不修改，时区变更立即生效 */
  @Transactional(rollbackFor = Exception.class)
  /** 更新设置并刷新时区真源 */
  public SettingsResp update(SettingsUpdateReq req) {
    commands.update(
        new WorkspaceSettings.Revision(
            req.getTimezone(),
            req.getDateFormat(),
            req.getWeekStart(),
            req.getDayBoundaryHour(),
            req.getMorningBriefTime(),
            req.getEodReviewTime(),
            req.getDesktopNotification(),
            req.getQuickCaptureShortcut()));
    return get();
  }
}
