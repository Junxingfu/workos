package com.workos.item.application;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.ItemDetails;
import com.workos.item.domain.ItemRevision;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 事项创建、修订、整理和排序用例；只编排聚合，不操作 PO/Mapper。 */
@Service
@RequiredArgsConstructor
@Transactional
public class ItemEditingService {
  private final WorkItemRepository repository;
  private final TimeZoneHolder clock;

  /** 检查项目引用，调用聚合工厂并保存；创建与初始计划在同一事务。 */
  public long create(
      ItemDetails details,
      ItemType type,
      ItemStatus status,
      LocalDate date,
      Instant followUp,
      String waitingFor) {
    repository.requireProjects(details.projects());
    WorkItem item =
        WorkItem.create(
            details,
            type,
            status,
            date,
            followUp,
            waitingFor,
            clock.today(),
            date == null ? 0 : repository.nextPlanRank(date),
            clock.now());
    repository.save(item);
    return item.state().id();
  }

  /** 核对编辑版本，再由聚合验证属性修订；关联项目与内容共同提交。 */
  public void revise(long id, Integer expected, ItemRevision revision) {
    WorkItem item = repository.get(id);
    if (expected != null && !Objects.equals(expected, item.state().version())) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
    try {
      item.revise(
          revision,
          clock.today(),
          revision.plannedDate() == null ? 0 : repository.nextPlanRank(revision.plannedDate()),
          clock.now());
    } catch (IllegalArgumentException error) {
      throw new BizException(ResponseCode.PARAM_ERROR, error.getMessage());
    }
    repository.requireProjects(item.details().projects());
    repository.save(item);
  }

  /** 加载聚合并验证删除资格，仓储删除时仍检查版本。 */
  public void remove(long id) {
    WorkItem item = repository.get(id);
    item.ensureCanDelete();
    repository.remove(item);
  }

  /** 批量收下未整理事项；任务进入待办、笔记归档，不重置原项目归属。 */
  public int accept(List<Long> ids) {
    List<WorkItem> items = repository.inbox(ids);
    for (WorkItem item : items) {
      item.transition(
          item.state().type() == ItemType.NOTE ? ItemStatus.DONE : ItemStatus.TODO,
          null,
          null,
          null,
          "先都收下",
          clock.now());
      repository.save(item);
    }
    return items.size();
  }

  /** 在一个事务内按输入顺序重排；重复 ID 拒绝，其他日期的事项不迁入。 */
  public void reorder(LocalDate date, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    if (ids.stream().distinct().count() != ids.size()) {
      throw new IllegalArgumentException("计划事项不能重复");
    }
    int rank = 1;
    for (long id : ids) {
      WorkItem item = repository.get(id);
      item.reorder(date, rank++, clock.now());
      repository.save(item);
    }
  }
}
