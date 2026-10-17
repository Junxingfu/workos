package com.workos.item.interfaces.http;

import com.workos.common.model.BasePageResult;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.application.ItemCommands;
import com.workos.item.application.ItemEditingService;
import com.workos.item.application.ItemLifecycleService;
import com.workos.item.application.query.ItemCriteria;
import com.workos.item.application.query.ItemQuery;
import com.workos.item.application.readmodel.ItemActivityResp;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.domain.ItemDetails;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemRevision;
import com.workos.item.domain.ItemSource;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.ProjectReferences;
import com.workos.item.interfaces.request.ItemCreateReq;
import com.workos.item.interfaces.request.ItemFollowUpActionReq;
import com.workos.item.interfaces.request.ItemPlanReorderReq;
import com.workos.item.interfaces.request.ItemPlanReq;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemTransitionReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Item 接口适配器：转换输入并调用用例，读取由查询投影负责。 */
@Component
@RequiredArgsConstructor
public class ItemHttpAdapter {
  private final ItemQuery queries;
  private final ItemEditingService editing;
  private final ItemLifecycleService lifecycle;
  private final TimeZoneHolder timeZoneHolder;

  /** 创建事项 */
  public Long create(ItemCreateReq req) {
    return editing.create(
        new ItemDetails(
            req.getTitle(),
            req.getDescription(),
            new ProjectReferences(req.getProjectId(), req.getRelatedProjectIds()),
            req.getPriority(),
            req.getDueAt(),
            req.getRelatedPerson(),
            req.getSource()),
        Objects.requireNonNullElse(req.getItemType(), ItemType.TASK),
        Objects.requireNonNullElse(req.getStatus(), ItemStatus.INBOX),
        req.getPlannedDate(),
        req.getNextFollowUpAt(),
        req.getWaitingFor());
  }

  /** 快速记录：按入口选择进入未整理、待办或今日计划 */
  public Long quickCapture(ItemQuickCaptureReq req) {
    ItemQuickCaptureReq.Destination destination =
        Objects.requireNonNullElse(req.getDestination(), ItemQuickCaptureReq.Destination.INBOX);
    return editing.create(
        new ItemDetails(
            req.getContent(),
            null,
            new ProjectReferences(req.getProjectId(), null),
            ItemPriority.P2,
            null,
            null,
            ItemSource.QUICK_CAPTURE),
        ItemType.TASK,
        destination == ItemQuickCaptureReq.Destination.INBOX ? ItemStatus.INBOX : ItemStatus.TODO,
        destination == ItemQuickCaptureReq.Destination.TODAY ? timeZoneHolder.today() : null,
        null,
        null);
  }

  /** 事项详情 */
  public ItemResp detail(Long id) {
    return queries.detail(id);
  }

  /** 全部事项分页 */
  public BasePageResult<ItemResp> page(ItemCriteria req) {
    return queries.page(req);
  }

  /** 属性修改（不允许改状态） */
  public Boolean update(Long id, ItemUpdateReq req) {
    editing.revise(
        id,
        req.getVersion(),
        new ItemRevision(
            req.getTitle(),
            req.getDescription(),
            req.getProjectId(),
            req.getRelatedProjectIds(),
            Boolean.TRUE.equals(req.getClearProject()),
            req.getItemType(),
            req.getPriority(),
            req.getPlannedDate(),
            req.getDueAt(),
            Boolean.TRUE.equals(req.getClearDueAt()),
            req.getWaitingFor(),
            req.getNextFollowUpAt(),
            Boolean.TRUE.equals(req.getClearNextFollowUp()),
            req.getRelatedPerson()));
    return true;
  }

  /** 状态转换（唯一入口） */
  public ItemResp transition(Long id, ItemTransitionReq req) {
    lifecycle.transition(
        new ItemCommands.Transition(
            id,
            req.getVersion(),
            req.getToStatus(),
            req.getWaitingFor(),
            req.getWaitingExpectedAt(),
            req.getNextFollowUpAt(),
            req.getRemark()));
    return detail(id);
  }

  /** 跟进动作三选一 */
  public ItemResp followUpAction(Long id, ItemFollowUpActionReq req) {
    lifecycle.followUp(
        new ItemCommands.FollowUp(
            id,
            req.getVersion(),
            req.getOutcome(),
            req.getToStatus(),
            req.getNextFollowUpAt(),
            req.getNote()));
    return detail(id);
  }

  /** 加入今日计划 / 排到某天 */
  public ItemResp plan(Long id, ItemPlanReq req) {
    lifecycle.plan(new ItemCommands.Plan(id, req.getVersion(), req.getPlannedDate()));
    return detail(id);
  }

  /** 移出今日计划 */
  public Boolean unplan(Long id) {
    lifecycle.unplan(id);
    return Boolean.TRUE;
  }

  /** 调整今日计划顺序 */
  public Boolean reorderPlan(ItemPlanReorderReq req) {
    editing.reorder(req.getDate(), req.getItemIds());
    return true;
  }

  /** 删除（仅限未整理） */
  public Boolean delete(Long id) {
    editing.remove(id);
    return true;
  }

  /** 查询系统事件 */
  public List<ItemActivityResp> activities(Long id) {
    return queries.activities(id);
  }
}
