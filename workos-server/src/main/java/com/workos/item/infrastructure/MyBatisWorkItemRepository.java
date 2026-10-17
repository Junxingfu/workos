package com.workos.item.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.item.domain.ActivityType;
import com.workos.item.domain.FollowUpOutcome;
import com.workos.item.domain.ItemDetails;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ProjectReferences;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemEvent;
import com.workos.item.domain.WorkItemRepository;
import com.workos.item.infrastructure.persistence.entity.ItemActivityPO;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemActivityMapper;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.persistence.mapper.ItemProjectMapper;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 事项聚合仓储的 MyBatis 实现；在应用事务内保存事项、关联项目和审计事件，以版本条件阻止覆盖。 */
@Repository
@RequiredArgsConstructor
public class MyBatisWorkItemRepository implements WorkItemRepository {
  private final ItemMapper items;
  private final ItemActivityMapper activities;
  private final ItemProjectMapper links;
  private final ProjectMapper projects;

  public WorkItem get(long id) {
    ItemPO entity = items.selectById(id);
    if (entity == null) {
      throw new BizException(ResponseCode.ITEM_NOT_FOUND);
    }
    return new WorkItem(
        new WorkItem.State(
            entity.getId(),
            entity.getVersion(),
            entity.getItemType(),
            entity.getStatus(),
            entity.getWaitingFor(),
            entity.getWaitingSince(),
            entity.getWaitingExpectedAt(),
            entity.getNextFollowUpAt(),
            entity.getCompletedAt(),
            entity.getCancelledAt(),
            entity.getPlannedDate(),
            entity.getPlanRank()),
        new ItemDetails(
            entity.getTitle(),
            entity.getDescription(),
            new ProjectReferences(
                entity.getProjectId(),
                links.findByItems(List.of(id)).stream().map(link -> link.getProjectId()).toList()),
            entity.getPriority(),
            entity.getDueAt(),
            entity.getRelatedPerson(),
            entity.getSource()));
  }

  public int nextPlanRank(LocalDate date) {
    return Math.toIntExact(
            items.selectCount(new LambdaQueryWrapper<ItemPO>().eq(ItemPO::getPlannedDate, date)))
        + 1;
  }

  public void save(WorkItem item) {
    if (item.events().isEmpty()) {
      return;
    }
    WorkItem.State state = item.state();
    ItemDetails details = item.details();
    if (state.id() == 0) {
      ItemPO po =
          ItemPO.builder()
              .title(details.title())
              .description(details.description())
              .projectId(details.projects().mainProjectId())
              .priority(details.priority())
              .dueAt(details.dueAt())
              .relatedPerson(details.relatedPerson())
              .source(details.source())
              .itemType(state.type())
              .status(state.status())
              .waitingFor(state.waitingFor())
              .waitingSince(state.waitingSince())
              .waitingExpectedAt(state.waitingExpectedAt())
              .nextFollowUpAt(state.nextFollowUpAt())
              .completedAt(state.completedAt())
              .cancelledAt(state.cancelledAt())
              .plannedDate(state.plannedDate())
              .planRank(state.planRank())
              .version(0)
              .build();
      items.insert(po);
      if (!details.projects().relatedProjectIds().isEmpty()) {
        links.addForItem(po.getId(), details.projects().relatedProjectIds());
      }
      for (WorkItemEvent event : item.events()) {
        activities.insert(toActivity(po.getId(), event));
      }
      item.identify(po.getId());
      return;
    }
    // 显式 set 包括 null：用户清空截止/等待字段也必须落库，不能依赖 ORM 的非空更新策略。
    LambdaUpdateWrapper<ItemPO> update =
        new LambdaUpdateWrapper<ItemPO>()
            .eq(ItemPO::getId, state.id())
            .eq(ItemPO::getVersion, state.version())
            .set(ItemPO::getVersion, state.version() + 1)
            .set(ItemPO::getStatus, state.status())
            .set(ItemPO::getItemType, state.type())
            .set(ItemPO::getTitle, details.title())
            .set(ItemPO::getDescription, details.description())
            .set(ItemPO::getProjectId, details.projects().mainProjectId())
            .set(ItemPO::getPriority, details.priority())
            .set(ItemPO::getDueAt, details.dueAt())
            .set(ItemPO::getRelatedPerson, details.relatedPerson())
            .set(ItemPO::getWaitingFor, state.waitingFor())
            .set(ItemPO::getWaitingSince, state.waitingSince())
            .set(ItemPO::getWaitingExpectedAt, state.waitingExpectedAt())
            .set(ItemPO::getNextFollowUpAt, state.nextFollowUpAt())
            .set(ItemPO::getCompletedAt, state.completedAt())
            .set(ItemPO::getCancelledAt, state.cancelledAt())
            .set(ItemPO::getPlannedDate, state.plannedDate())
            .set(ItemPO::getPlanRank, state.planRank())
            .set(ItemPO::getUpdatedAt, item.events().get(item.events().size() - 1).at());
    if (items.update(null, update) != 1) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
    links.removeForItem(state.id());
    if (!details.projects().relatedProjectIds().isEmpty()) {
      links.addForItem(state.id(), details.projects().relatedProjectIds());
    }
    for (WorkItemEvent event : item.events()) {
      activities.insert(toActivity(state.id(), event));
    }
    // 只有整个聚合及事件写入成功后，才确认内存版本并清空待保存事件。
    item.markPersisted();
  }

  @Override
  public void remove(WorkItem item) {
    if (items.delete(
            new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getId, item.state().id())
                .eq(ItemPO::getVersion, item.state().version()))
        != 1) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
  }

  @Override
  public List<WorkItem> inbox(List<Long> ids) {
    return items
        .selectList(
            new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getStatus, ItemStatus.INBOX)
                .in(ids != null && !ids.isEmpty(), ItemPO::getId, ids))
        .stream()
        .map(entity -> get(entity.getId()))
        .toList();
  }

  @Override
  public void requireProjects(ProjectReferences references) {
    LinkedHashSet<Long> ids = new LinkedHashSet<>(references.relatedProjectIds());
    if (references.mainProjectId() != null) {
      ids.add(references.mainProjectId());
    }
    if (!ids.isEmpty() && projects.selectBatchIds(ids).size() != ids.size()) {
      throw new BizException(ResponseCode.PROJECT_NOT_FOUND);
    }
  }

  /** 领域事件映射到既有活动读模型，表结构与客户端保持兼容。 */
  private ItemActivityPO toActivity(long id, WorkItemEvent event) {
    ItemActivityPO.ItemActivityPOBuilder<?, ?> builder =
        ItemActivityPO.builder().itemId(id).actor("USER").occurredAt(event.at());
    if (event instanceof WorkItemEvent.Created) {
      builder.activityType(ActivityType.CREATED).remark("创建事项");
    } else if (event instanceof WorkItemEvent.DetailsRevised) {
      builder.activityType(ActivityType.FIELD_CHANGED).remark("事项内容或归属已更新");
    } else if (event instanceof WorkItemEvent.StatusChanged changed) {
      builder
          .activityType(ActivityType.STATUS_CHANGED)
          .fromStatus(changed.from().name())
          .toStatus(changed.to().name())
          .remark(changed.remark());
    } else if (event instanceof WorkItemEvent.WaitingEnded ended) {
      builder
          .activityType(ActivityType.WAITING_ENDED)
          .fromStatus(ItemStatus.WAITING.name())
          .toStatus(ended.to().name())
          .remark("已等待 " + ended.days() + " 天");
    } else if (event instanceof WorkItemEvent.PlanChanged planned) {
      builder
          .activityType(ActivityType.PLAN_CHANGED)
          .remark(planned.date() == null ? "移出计划" : "排到 " + planned.date());
    } else if (event instanceof WorkItemEvent.FollowUpRecorded followed) {
      String remark =
          followed.outcome() == FollowUpOutcome.STILL_WAITING
              ? "已跟进，更新下次跟进时间"
              : followed.outcome() == FollowUpOutcome.ABANDONED ? "暂时不跟" : "跟进完成";
      builder.activityType(ActivityType.FIELD_CHANGED).remark(remark);
    } else if (event instanceof WorkItemEvent.TrackingRevised) {
      builder.activityType(ActivityType.FIELD_CHANGED).remark("等待或跟进信息已更新");
    } else {
      throw new IllegalArgumentException("未支持的领域事件：" + event.getClass().getSimpleName());
    }
    return builder.build();
  }
}
