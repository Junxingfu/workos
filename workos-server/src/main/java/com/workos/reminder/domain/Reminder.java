package com.workos.reminder.domain;

import com.workos.shared.domain.ConcurrentChange;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** 手动提醒聚合：安排、延后和忽略均不改变任务进度。 */
public final class Reminder {
  /** 持久化快照；外部只能读取，修改必须调用业务行为。 */
  public record State(
      Long id,
      String kind,
      Long itemId,
      String title,
      Instant remindAt,
      String status,
      Instant triggeredAt,
      Instant acknowledgedAt,
      Instant createdAt,
      Integer version) {}

  /** 聚合身份；创建时未分配，首次保存后确定。 */
  private Long id;

  /** ITEM / DAILY / STANDALONE */
  private String kind;

  /** 关联事项 ID；独立提醒可为空，提醒的处理不改变事项状态。 */
  private Long itemId;

  /** 用户可见标题。 */
  private String title;

  /** 下次展示该提醒的时间点，查询待处理提醒时与当前时间比较。 */
  private Instant remindAt;

  /** PENDING / DISMISSED / CANCELLED */
  private String status;

  /** MVP 不写入，字段保留 */
  private Instant triggeredAt;

  /** 用户处理提醒的时间点；未处理时为空。 */
  private Instant acknowledgedAt;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  private Instant createdAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  private Integer version;

  /** 从仓储快照重建已有聚合；新增业务对象应使用命名工厂方法。 */
  public Reminder(State state) {
    id = state.id();
    kind = state.kind();
    itemId = state.itemId();
    title = state.title();
    remindAt = state.remindAt();
    status = state.status();
    triggeredAt = state.triggeredAt();
    acknowledgedAt = state.acknowledgedAt();
    createdAt = state.createdAt();
    version = state.version();
  }

  /** 返回不可变状态快照，供仓储保存或应用读取；不开放字段 setter。 */
  public State snapshot() {
    return new State(
        id, kind, itemId, title, remindAt, status, triggeredAt, acknowledgedAt, createdAt, version);
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

  /** 创建待处理提醒，校验类型、标题与提醒时间。 */
  public static Reminder schedule(
      String kind, Long itemId, String title, Instant when, Instant now) {
    if (title == null || title.isBlank() || title.length() > 500) {
      throw new IllegalArgumentException("提醒标题不能为空且不超过 500 字");
    }
    Objects.requireNonNull(when, "提醒时间不能为空");
    String type = kind == null || kind.isBlank() ? "ITEM" : kind;
    if (!Set.of("ITEM", "DAILY", "STANDALONE").contains(type)) {
      throw new IllegalArgumentException("提醒类型无效");
    }
    return new Reminder(new State(null, type, itemId, title, when, "PENDING", null, null, now, 0));
  }

  private void requirePending() {
    if (!"PENDING".equals(status)) {
      throw new IllegalArgumentException("该提醒已处理");
    }
  }

  /** 只允许待处理提醒改时间，已经处理的提醒不能重新延后。 */
  public void reschedule(Instant when) {
    requirePending();
    remindAt = Objects.requireNonNull(when);
  }

  /** 处理待办提醒并记录确认时间；不完成关联事项。 */
  public void dismiss(Instant now) {
    requirePending();
    status = "DISMISSED";
    acknowledgedAt = now;
  }
}
