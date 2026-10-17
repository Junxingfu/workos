package com.workos.project.domain;

import com.workos.shared.domain.ConcurrentChange;
import java.time.Instant;
import java.util.Objects;

/** 项目现状聚合：独立版本避免修改现状与归档项目互相冲突；仅引用项目 ID。 */
public final class ProjectBrief {
  /** 持久化快照；外部只能读取，修改必须调用业务行为。 */
  public record State(
      Long id,
      Long projectId,
      String goal,
      String keyPeople,
      String keyLinks,
      String focus,
      String blocker,
      String nextAction,
      String progressNote,
      String notesMarkdown,
      Instant updatedAt,
      Integer version) {}

  /** 聚合身份；创建时未分配，首次保存后确定。 */
  private Long id;

  /** 现状所属项目 ID，唯一；现状与项目基本信息使用独立版本。 */
  private Long projectId;

  /** 目标（稳定） */
  private String goal;

  /** 关键人（稳定） */
  private String keyPeople;

  /** 关键链接（稳定） */
  private String keyLinks;

  /** 当前焦点（动态） */
  private String focus;

  /** 当前阻塞 */
  private String blocker;

  /** 下一步 */
  private String nextAction;

  /** 进展备注 */
  private String progressNote;

  /** 现状补充笔记，使用 Markdown，可为空。 */
  private String notesMarkdown;

  /** 最近一次保存的时间点。 */
  private Instant updatedAt;

  /** 现状自身的乐观锁版本，避免与项目基本信息编辑相互冲突。 */
  private Integer version;

  /** 从仓储快照重建已有聚合；新增业务对象应使用命名工厂方法。 */
  public ProjectBrief(State state) {
    id = state.id();
    projectId = state.projectId();
    goal = state.goal();
    keyPeople = state.keyPeople();
    keyLinks = state.keyLinks();
    focus = state.focus();
    blocker = state.blocker();
    nextAction = state.nextAction();
    progressNote = state.progressNote();
    notesMarkdown = state.notesMarkdown();
    updatedAt = state.updatedAt();
    version = state.version();
  }

  /** 返回不可变状态快照，供仓储保存或应用读取；不开放字段 setter。 */
  public State snapshot() {
    return new State(
        id,
        projectId,
        goal,
        keyPeople,
        keyLinks,
        focus,
        blocker,
        nextAction,
        progressNote,
        notesMarkdown,
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
      String goal,
      String keyPeople,
      String keyLinks,
      String focus,
      String blocker,
      String nextAction,
      String progressNote,
      String notesMarkdown) {}

  /** 为尚无现状的项目创建版本为 0 的空白内容，首次保存时分配身份。 */
  public static ProjectBrief empty(long projectId, Instant now) {
    return new ProjectBrief(
        new State(null, projectId, null, null, null, null, null, null, null, null, now, 0));
  }

  /** 修订项目现状；独立版本使现状编辑不与项目基本信息修改争锁。 */
  public void revise(Revision change, Instant now) {
    if (change.goal() != null) {
      goal = change.goal();
    }
    if (change.keyPeople() != null) {
      keyPeople = change.keyPeople();
    }
    if (change.keyLinks() != null) {
      keyLinks = change.keyLinks();
    }
    if (change.focus() != null) {
      focus = change.focus();
    }
    if (change.blocker() != null) {
      blocker = change.blocker();
    }
    if (change.nextAction() != null) {
      nextAction = change.nextAction();
    }
    if (change.progressNote() != null) {
      progressNote = change.progressNote();
    }
    if (change.notesMarkdown() != null) {
      notesMarkdown = change.notesMarkdown();
    }
    updatedAt = now;
    if (id == null) {
      version = 1;
    }
  }
}
