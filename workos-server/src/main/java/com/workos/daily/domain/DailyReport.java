package com.workos.daily.domain;

import com.workos.shared.domain.ConcurrentChange;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** 日报聚合：正文和生成快照分别保存，只有明确应用快照才覆盖正文。 */
public final class DailyReport {
  /** 持久化快照；外部只能读取，修改必须调用业务行为。 */
  public record State(
      Long id,
      LocalDate dailyDate,
      String content,
      String generatedContent,
      String status,
      Instant createdAt,
      Instant updatedAt,
      Integer version) {}

  /** 聚合身份；创建时未分配，首次保存后确定。 */
  private Long id;

  /** 工作区业务日期；每个日期只有一份日报。 */
  private LocalDate dailyDate;

  /** 用户表达（事实真源是 Item / WorkLog） */
  private String content;

  /** 依据人工工作记录和完成事项生成的快照；生成本身不覆盖用户正文。 */
  private String generatedContent;

  /** 日报保存状态：编辑为 DRAFT，确认应用为 CONFIRMED；不代表事项执行状态。 */
  private String status;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  private Integer version;

  /** 从仓储快照重建已有聚合；新增业务对象应使用命名工厂方法。 */
  public DailyReport(State state) {
    id = state.id();
    dailyDate = state.dailyDate();
    content = state.content();
    generatedContent = state.generatedContent();
    status = state.status();
    createdAt = state.createdAt();
    updatedAt = state.updatedAt();
    version = state.version();
  }

  /** 返回不可变状态快照，供仓储保存或应用读取；不开放字段 setter。 */
  public State snapshot() {
    return new State(
        id, dailyDate, content, generatedContent, status, createdAt, updatedAt, version);
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

  /** 按业务日期创建草稿；唯一日期约束由仓储保证。 */
  public static DailyReport draft(LocalDate day, Instant now) {
    Objects.requireNonNull(day, "日报日期不能为空");
    return new DailyReport(new State(null, day, "# 今日工作\n\n", null, "DRAFT", now, now, 0));
  }

  /** 保存用户编辑的正文，保留已有生成快照供再次比较。 */
  public void write(String text, Instant now) {
    content = Objects.requireNonNull(text);
    status = "DRAFT";
    updatedAt = now;
  }

  /** 只替换生成快照，保留用户已经编辑的正文。 */
  public void generate(String snapshot, Instant now) {
    generatedContent = Objects.requireNonNull(snapshot);
    updatedAt = now;
  }

  /** 保存用户审阅后的内容并标为已确认；应用层负责检查编辑版本。 */
  public void apply(String reviewedText, Instant now) {
    content = Objects.requireNonNull(reviewedText);
    status = "CONFIRMED";
    updatedAt = now;
  }
}
