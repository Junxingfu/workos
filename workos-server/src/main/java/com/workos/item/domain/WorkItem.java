package com.workos.item.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 事项聚合：生命周期与计划的业务一致性边界，不依赖 Spring、HTTP 或数据库。 */
public final class WorkItem {
  /** 生命周期快照；与 ItemDetails 共同组成一个事项，版本统一保护两者。 */
  public record State(
      long id,
      int version,
      ItemType type,
      ItemStatus status,
      String waitingFor,
      Instant waitingSince,
      Instant waitingExpectedAt,
      Instant nextFollowUpAt,
      Instant completedAt,
      Instant cancelledAt,
      LocalDate plannedDate,
      Integer planRank) {}

  /** 当前生命周期快照，每个行为完成后整体替换。 */
  private State state;

  /** 内容与项目身份引用；与生命周期一起保存。 */
  private ItemDetails details;

  /** 当前事务尚未落库的领域事实；保存成功后清空，防止重复记录。 */
  private final List<WorkItemEvent> events = new ArrayList<>();

  /** 仅重建生命周期，供状态规则预演使用；持久化用例必须提供完整 ItemDetails。 */
  public WorkItem(State state) {
    this.state = Objects.requireNonNull(state);
  }

  /** 仓储重建现有聚合，不能用于创建绕过工厂的新事项。 */
  public WorkItem(State state, ItemDetails details) {
    this(state);
    this.details = Objects.requireNonNull(details);
  }

  /** 读取不可变内容；仅有生命周期的预演实例不能参与内容保存。 */
  public ItemDetails details() {
    return Objects.requireNonNull(details, "缺少事项内容");
  }

  /** 新建工厂：首次状态、等待、关闭时间与计划必须同时有效。 */
  public static WorkItem create(
      ItemDetails details,
      ItemType type,
      ItemStatus status,
      LocalDate plannedDate,
      Instant followUp,
      String waitingFor,
      LocalDate today,
      int rank,
      Instant now) {
    validateType(type, status);
    WaitingPeriod waiting =
        status == ItemStatus.WAITING ? new WaitingPeriod(waitingFor, now, null) : null;
    WorkItem item =
        new WorkItem(
            new State(
                0,
                0,
                type,
                status,
                waiting == null ? null : waiting.person(),
                waiting == null ? null : now,
                null,
                followUp,
                status == ItemStatus.DONE ? now : null,
                status == ItemStatus.CANCELLED ? now : null,
                null,
                null),
            details);
    if (plannedDate != null) {
      item.plan(plannedDate, today, rank, now);
    }
    item.events.add(new WorkItemEvent.Created(0, now));
    return item;
  }

  private static void validateType(ItemType type, ItemStatus status) {
    Objects.requireNonNull(type, "事项类型不能为空");
    Objects.requireNonNull(status, "事项状态不能为空");
    if (type == ItemType.NOTE
        && !Set.of(ItemStatus.INBOX, ItemStatus.DONE, ItemStatus.CANCELLED).contains(status)) {
      throw new IllegalArgumentException("笔记不支持此任务状态");
    }
  }

  /** 一次修订作为一个行为执行；所有候选值先校验，成功后整体替换。 */
  public void revise(ItemRevision change, LocalDate today, int rank, Instant now) {
    ItemDetails previousDetails = details();
    ProjectReferences projectReferences =
        new ProjectReferences(
            change.clearProject()
                ? null
                : change.projectId() == null
                    ? previousDetails.projects().mainProjectId()
                    : change.projectId(),
            change.relatedProjects() == null
                ? previousDetails.projects().relatedProjectIds()
                : change.relatedProjects());
    ItemDetails next =
        new ItemDetails(
            change.title() == null ? previousDetails.title() : change.title(),
            change.description() == null ? previousDetails.description() : change.description(),
            projectReferences,
            change.priority() == null ? previousDetails.priority() : change.priority(),
            change.clearDue()
                ? null
                : change.dueAt() == null ? previousDetails.dueAt() : change.dueAt(),
            change.relatedPerson() == null
                ? previousDetails.relatedPerson()
                : change.relatedPerson(),
            previousDetails.source());
    ItemType type = change.type() == null ? state.type() : change.type();
    validateType(type, state.status());
    State previousState = state;
    // 临时聚合用于验证复合修订，异常时原实例和待保存事件都不发生改变。
    WorkItem candidate =
        new WorkItem(
            new State(
                previousState.id(),
                previousState.version(),
                type,
                previousState.status(),
                previousState.waitingFor(),
                previousState.waitingSince(),
                previousState.waitingExpectedAt(),
                previousState.nextFollowUpAt(),
                previousState.completedAt(),
                previousState.cancelledAt(),
                type == ItemType.NOTE ? null : previousState.plannedDate(),
                type == ItemType.NOTE ? null : previousState.planRank()),
            next);
    if (change.plannedDate() != null && !change.plannedDate().equals(previousState.plannedDate())) {
      candidate.plan(change.plannedDate(), today, rank, now);
    }
    if (change.waitingFor() != null || change.followUpAt() != null || change.clearFollowUp()) {
      candidate.reviseTracking(
          change.waitingFor(), change.followUpAt(), change.clearFollowUp(), now);
    }
    details = next;
    state = candidate.state;
    events.addAll(candidate.events);
    events.add(new WorkItemEvent.DetailsRevised(previousState.id(), now));
  }

  /** 物理删除仅限未整理事项；已开始管理的事项使用取消行为保留历史。 */
  public void ensureCanDelete() {
    if (state.status() != ItemStatus.INBOX) {
      throw new IllegalArgumentException("只能删除待整理事项");
    }
  }

  /** 排序只修改同一天的计划顺序，不改变计划归属。 */
  public void reorder(LocalDate date, int rank, Instant now) {
    if (!Objects.equals(state.plannedDate(), date)) {
      return;
    }
    if (rank < 1) {
      throw new IllegalArgumentException("计划顺序必须为正数");
    }
    changePlan(date, rank, now);
  }

  /** 首次插入后分配身份；创建审计由仓储落库，避免再次提交临时 ID 的事件。 */
  public void identify(long id) {
    if (state.id() != 0) {
      throw new IllegalStateException("已有事项不能重新分配身份");
    }
    State previousState = state;
    state =
        new State(
            id,
            0,
            previousState.type(),
            previousState.status(),
            previousState.waitingFor(),
            previousState.waitingSince(),
            previousState.waitingExpectedAt(),
            previousState.nextFollowUpAt(),
            previousState.completedAt(),
            previousState.cancelledAt(),
            previousState.plannedDate(),
            previousState.planRank());
    events.clear();
  }

  /** 读取生命周期快照，外部不能通过它直接修改聚合。 */
  public State state() {
    return state;
  }

  /** 返回待保存事件的只读副本；事件记录已发生的事实。 */
  public List<WorkItemEvent> events() {
    return List.copyOf(events);
  }

  /** 校验流转能力，统一维护等待周期与关闭时间，并记录前后状态。 */
  public void transition(
      ItemStatus to,
      String waitingFor,
      Instant expected,
      Instant followUp,
      String remark,
      Instant now) {
    State previousState = state;
    if (!new TransitionPolicy()
        .allowedTargets(previousState.status(), previousState.type())
        .contains(to)) {
      throw new IllegalArgumentException("不允许从 " + previousState.status() + " 变更为 " + to);
    }
    boolean leaving = previousState.status() == ItemStatus.WAITING;
    WaitingPeriod entering =
        to == ItemStatus.WAITING
            ? new WaitingPeriod(
                waitingFor == null ? previousState.waitingFor() : waitingFor, now, expected)
            : null;
    if (leaving) {
      long days =
          previousState.waitingSince() == null
              ? 0
              : new WaitingPeriod(
                      previousState.waitingFor(),
                      previousState.waitingSince(),
                      previousState.waitingExpectedAt())
                  .elapsedDays(now);
      events.add(new WorkItemEvent.WaitingEnded(previousState.id(), to, days, now));
    }
    boolean reopen = to == ItemStatus.TODO || to == ItemStatus.INBOX;
    state =
        new State(
            previousState.id(),
            previousState.version(),
            previousState.type(),
            to,
            leaving ? null : entering != null ? entering.person() : previousState.waitingFor(),
            leaving ? null : entering != null ? entering.since() : previousState.waitingSince(),
            leaving
                ? null
                : entering != null ? entering.expectedAt() : previousState.waitingExpectedAt(),
            leaving ? null : followUp == null ? previousState.nextFollowUpAt() : followUp,
            reopen ? null : to == ItemStatus.DONE ? now : previousState.completedAt(),
            reopen ? null : to == ItemStatus.CANCELLED ? now : previousState.cancelledAt(),
            previousState.plannedDate(),
            previousState.planRank());
    events.add(
        new WorkItemEvent.StatusChanged(
            previousState.id(), previousState.status(), to, remark, now));
  }

  /** 仅活动任务可安排到今天或以后；计划日期不等同于任务状态。 */
  public void plan(LocalDate date, LocalDate today, int rank, Instant now) {
    if (date == null || date.isBefore(today)) {
      throw new IllegalArgumentException("计划日期不能早于今天");
    }
    if (state.type() != ItemType.TASK
        || !Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.WAITING)
            .contains(state.status())) {
      throw new IllegalArgumentException("当前状态不能加入计划");
    }
    changePlan(date, rank, now);
  }

  /** 清除计划日期和排序，不改变执行状态。 */
  public void unplan(Instant now) {
    changePlan(null, null, now);
  }

  /** 跟进是任务行为，不能让已关闭任务留下新的催办时间。返回人工跟进记录的正文。 */
  public String followUp(
      FollowUpOutcome outcome, ItemStatus target, Instant nextAt, String note, Instant now) {
    requireActiveTask();
    if (outcome == null) {
      throw new IllegalArgumentException("跟进结果不能为空");
    }
    String who = state.waitingFor();
    String content = note == null || note.isBlank() ? null : note.trim();
    if (outcome == FollowUpOutcome.STILL_WAITING) {
      Instant next = nextAt == null ? now.plus(Duration.ofDays(2)) : nextAt;
      if (!next.isAfter(now)) {
        throw new IllegalArgumentException("下次跟进时间必须晚于现在");
      }
      reviseTracking(null, next, false, now, false);
      events.add(new WorkItemEvent.FollowUpRecorded(state.id(), outcome, next, now));
      return content != null
          ? content
          : who == null || who.isBlank() ? "已跟进，等待对方回复" : "已催 " + who + "，对方还没回";
    }
    if (outcome == FollowUpOutcome.RESOLVED) {
      ItemStatus to =
          target == null
              ? state.status() == ItemStatus.WAITING ? ItemStatus.DOING : state.status()
              : target;
      if (!Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.DONE).contains(to)) {
        throw new IllegalArgumentException("收到结果后请选择待办、进行中或已完成");
      }
      if (to != state.status()) {
        transition(to, null, null, null, "跟进完成" + (content == null ? "" : "：" + content), now);
      }
    }
    reviseTracking(null, null, true, now, false);
    events.add(new WorkItemEvent.FollowUpRecorded(state.id(), outcome, null, now));
    if (outcome == FollowUpOutcome.ABANDONED) {
      return null;
    }
    return content != null ? content : who == null || who.isBlank() ? "跟进已闭环" : who + " 已给出结果";
  }

  /** 属性编辑也必须遵守等待不变量，不能通过通用更新绕开聚合。 */
  public void reviseTracking(String person, Instant nextAt, boolean clearNext, Instant now) {
    reviseTracking(person, nextAt, clearNext, now, true);
  }

  private void reviseTracking(
      String person, Instant nextAt, boolean clearNext, Instant now, boolean record) {
    State previousState = state;
    String who = person == null ? previousState.waitingFor() : person.trim();
    if (who != null && who.isBlank()) {
      who = null;
    }
    if (previousState.status() == ItemStatus.WAITING) {
      who =
          new WaitingPeriod(
                  who,
                  previousState.waitingSince() == null ? now : previousState.waitingSince(),
                  previousState.waitingExpectedAt())
              .person();
    }
    Instant followUp = clearNext ? null : nextAt == null ? previousState.nextFollowUpAt() : nextAt;
    state =
        new State(
            previousState.id(),
            previousState.version(),
            previousState.type(),
            previousState.status(),
            who,
            previousState.waitingSince(),
            previousState.waitingExpectedAt(),
            followUp,
            previousState.completedAt(),
            previousState.cancelledAt(),
            previousState.plannedDate(),
            previousState.planRank());
    if (record) {
      events.add(new WorkItemEvent.TrackingRevised(state.id(), now));
    }
  }

  private void requireActiveTask() {
    if (state.type() != ItemType.TASK
        || !Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.WAITING)
            .contains(state.status())) {
      throw new IllegalArgumentException("只有已整理且未关闭的任务可以跟进");
    }
  }

  /** 仓储成功保存后确认版本，避免同一实例再次保存时重复写入事件。 */
  public void markPersisted() {
    State previousState = state;
    state =
        new State(
            previousState.id(),
            previousState.version() + 1,
            previousState.type(),
            previousState.status(),
            previousState.waitingFor(),
            previousState.waitingSince(),
            previousState.waitingExpectedAt(),
            previousState.nextFollowUpAt(),
            previousState.completedAt(),
            previousState.cancelledAt(),
            previousState.plannedDate(),
            previousState.planRank());
    events.clear();
  }

  private void changePlan(LocalDate date, Integer rank, Instant now) {
    State previousState = state;
    state =
        new State(
            previousState.id(),
            previousState.version(),
            previousState.type(),
            previousState.status(),
            previousState.waitingFor(),
            previousState.waitingSince(),
            previousState.waitingExpectedAt(),
            previousState.nextFollowUpAt(),
            previousState.completedAt(),
            previousState.cancelledAt(),
            date,
            rank);
    events.add(
        new WorkItemEvent.PlanChanged(previousState.id(), previousState.plannedDate(), date, now));
  }
}
