package com.workos.settings.domain;

import com.workos.shared.domain.ConcurrentChange;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** 工作区设置聚合：业务日与提醒偏好作为一致配置保存。 */
public final class WorkspaceSettings {
  /** 持久化快照；外部只能读取，修改必须调用业务行为。 */
  public record State(
      Long id,
      String timezone,
      String dateFormat,
      String weekStart,
      Short dayBoundaryHour,
      String morningBriefTime,
      String eodReviewTime,
      Boolean desktopNotification,
      String quickCaptureShortcut,
      Instant updatedAt,
      Integer version) {}

  /** 工作区单例标识，固定为 1。 */
  private Long id;

  /** IANA 时区标识，用于统一 Today、工作日与日报日期的计算。 */
  private String timezone;

  /** 日期展示格式偏好；不改变数据库日期类型。 */
  private String dateFormat;

  /** 每周起始日偏好，MONDAY 或 SUNDAY。 */
  private String weekStart;

  /** 业务日切换小时，允许 0–23；当前界面未开放编辑，默认 0。 */
  private Short dayBoundaryHour;

  /** 晨间摘要时间偏好，HH:mm；目前仅保存配置，尚未接入调度。 */
  private String morningBriefTime;

  /** 日终回顾时间偏好，HH:mm；目前仅保存配置，尚未接入调度。 */
  private String eodReviewTime;

  /** 桌面通知偏好；保存配置不代表已接入系统通知调度。 */
  private Boolean desktopNotification;

  /** 快捷记录键位偏好；系统热键是否注册由桌面适配器决定。 */
  private String quickCaptureShortcut;

  /** 最近一次保存的时间点。 */
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  private Integer version;

  /** 从仓储快照重建已有聚合；新增业务对象应使用命名工厂方法。 */
  public WorkspaceSettings(State state) {
    id = state.id();
    timezone = state.timezone();
    dateFormat = state.dateFormat();
    weekStart = state.weekStart();
    dayBoundaryHour = state.dayBoundaryHour();
    morningBriefTime = state.morningBriefTime();
    eodReviewTime = state.eodReviewTime();
    desktopNotification = state.desktopNotification();
    quickCaptureShortcut = state.quickCaptureShortcut();
    updatedAt = state.updatedAt();
    version = state.version();
  }

  /** 返回不可变状态快照，供仓储保存或应用读取；不开放字段 setter。 */
  public State snapshot() {
    return new State(
        id,
        timezone,
        dateFormat,
        weekStart,
        dayBoundaryHour,
        morningBriefTime,
        eodReviewTime,
        desktopNotification,
        quickCaptureShortcut,
        updatedAt,
        version);
  }

  /** 仓储保存成功后回填身份与版本；业务调用方不应自行修改版本。 */
  public void persisted(Long identity, int savedVersion) {
    id = identity;
    version = savedVersion;
  }

  /** 校验调用方看到的版本；不一致时拒绝覆盖较新的修改。 */
  public void expect(Integer expected) {
    if (expected != null && !Objects.equals(expected, version)) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
  }

  public record Revision(
      String timezone,
      String dateFormat,
      String weekStart,
      Integer dayBoundaryHour,
      String morningBriefTime,
      String eodReviewTime,
      Boolean desktopNotification,
      String quickCaptureShortcut) {}

  /** 生成工作区初始偏好；默认业务日按上海时区零点切换。 */
  public static WorkspaceSettings defaults(Instant now) {
    return new WorkspaceSettings(
        new State(
            1L,
            "Asia/Shanghai",
            "YYYY-MM-DD",
            "MONDAY",
            (short) 0,
            "09:00",
            "18:00",
            true,
            "Alt+Space",
            now,
            0));
  }

  /** 全部候选值校验通过后一次性修改，避免出现部分有效的设置。 */
  public void revise(Revision change, Instant now) {
    if (change.timezone() != null) {
      ZoneId.of(change.timezone());
    }
    if (change.dayBoundaryHour() != null
        && (change.dayBoundaryHour() < 0 || change.dayBoundaryHour() > 23)) {
      throw new IllegalArgumentException("日界线必须为 0–23");
    }
    if (change.weekStart() != null) {
      DayOfWeek.valueOf(change.weekStart());
    }
    if (change.morningBriefTime() != null) {
      LocalTime.parse(change.morningBriefTime());
    }
    if (change.eodReviewTime() != null) {
      LocalTime.parse(change.eodReviewTime());
    }
    if (change.timezone() != null) {
      timezone = change.timezone();
    }
    if (change.dateFormat() != null) {
      dateFormat = change.dateFormat();
    }
    if (change.weekStart() != null) {
      weekStart = change.weekStart();
    }
    if (change.dayBoundaryHour() != null) {
      dayBoundaryHour = change.dayBoundaryHour().shortValue();
    }
    if (change.morningBriefTime() != null) {
      morningBriefTime = change.morningBriefTime();
    }
    if (change.eodReviewTime() != null) {
      eodReviewTime = change.eodReviewTime();
    }
    if (change.desktopNotification() != null) {
      desktopNotification = change.desktopNotification();
    }
    if (change.quickCaptureShortcut() != null) {
      quickCaptureShortcut = change.quickCaptureShortcut();
    }
    updatedAt = now;
  }
}
