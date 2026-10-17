package com.workos.item.application;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 事项生命周期用例：事务、版本与协作编排；状态规则由 WorkItem 执行。 */
@Service
@RequiredArgsConstructor
public class ItemLifecycleService {
  private final WorkItemRepository repository;
  private final TimeZoneHolder clock;
  private final FollowUpJournal journal;

  /** 加载聚合并检查调用方的期望版本；仓储保存时再次用版本条件防并发覆盖。 */
  private WorkItem load(long id, Integer version) {
    WorkItem item = repository.get(id);
    if (version != null && version != item.state().version()) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
    return item;
  }

  /** 编排一次状态流转；状态与审计事件一起提交，合法性由聚合判断。 */
  @Transactional
  public void transition(ItemCommands.Transition command) {
    WorkItem item = load(command.itemId(), command.expectedVersion());
    try {
      item.transition(
          command.target(),
          command.waitingFor(),
          command.expectedAt(),
          command.followUpAt(),
          command.remark(),
          clock.now());
    } catch (IllegalArgumentException e) {
      throw new BizException(ResponseCode.ITEM_INVALID_TRANSITION, e.getMessage());
    }
    repository.save(item);
  }

  /** 缺省日期使用工作区今天，分配当日顺序后交给聚合验证。 */
  @Transactional
  public void plan(ItemCommands.Plan command) {
    WorkItem item = load(command.itemId(), command.expectedVersion());
    LocalDate date = command.date() == null ? clock.today() : command.date();
    try {
      item.plan(date, clock.today(), repository.nextPlanRank(date), clock.now());
    } catch (IllegalArgumentException e) {
      throw new BizException(ResponseCode.PARAM_ERROR, e.getMessage());
    }
    repository.save(item);
  }

  /** 跟进状态变更与附带人工记录同事务提交；任何一步失败都回滚。 */
  @Transactional
  public void followUp(ItemCommands.FollowUp command) {
    WorkItem item = load(command.itemId(), command.expectedVersion());
    Instant now = clock.now();
    final String note;
    try {
      note =
          item.followUp(command.outcome(), command.target(), command.nextAt(), command.note(), now);
    } catch (IllegalArgumentException e) {
      throw new BizException(ResponseCode.PARAM_ERROR, e.getMessage());
    }
    repository.save(item);
    if (note != null) {
      journal.append(command.itemId(), note, now);
    }
  }

  /** 修订等待对象或催办日期；清空语义通过命令明确表达。 */
  @Transactional
  public void reviseTracking(ItemCommands.ReviseTracking command) {
    WorkItem item = load(command.itemId(), command.expectedVersion());
    try {
      item.reviseTracking(command.waitingFor(), command.nextAt(), command.clearNext(), clock.now());
    } catch (IllegalArgumentException e) {
      throw new BizException(ResponseCode.PARAM_ERROR, e.getMessage());
    }
    repository.save(item);
  }

  /** 移出计划但保留执行状态；不把移出 Today 误解为退回未整理。 */
  @Transactional
  public void unplan(long id) {
    WorkItem item = load(id, null);
    item.unplan(clock.now());
    repository.save(item);
  }
}
