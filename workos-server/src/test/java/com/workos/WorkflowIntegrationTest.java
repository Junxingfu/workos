package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.daily.application.readmodel.DailyGenerateResp;
import com.workos.daily.application.readmodel.DailyResp;
import com.workos.daily.interfaces.http.DailyHttpAdapter;
import com.workos.daily.interfaces.request.DailyUpdateReq;
import com.workos.inbox.interfaces.http.InboxHttpAdapter;
import com.workos.inbox.interfaces.request.InboxAcceptAllReq;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.domain.FollowUpOutcome;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.interfaces.http.ItemHttpAdapter;
import com.workos.item.interfaces.request.ItemCreateReq;
import com.workos.item.interfaces.request.ItemFollowUpActionReq;
import com.workos.item.interfaces.request.ItemPlanReq;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemTransitionReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import com.workos.project.interfaces.http.ProjectHttpAdapter;
import com.workos.project.interfaces.request.ProjectCreateReq;
import com.workos.today.application.query.TodayQuery;
import com.workos.today.application.readmodel.TodaySectionsResp;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 通过真实业务入口和 PostgreSQL 验证工作流；每个用例回滚，必须显式选择验收数据库。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@EnabledIfEnvironmentVariable(named = "WORKOS_INTEGRATION_TEST", matches = "true")
class WorkflowIntegrationTest {
  @Autowired ItemHttpAdapter items;
  @Autowired TodayQuery today;
  @Autowired InboxHttpAdapter inbox;
  @Autowired DailyHttpAdapter daily;
  @Autowired ProjectHttpAdapter projects;
  @Autowired ItemMapper mapper;
  @Autowired TimeZoneHolder clock;

  private Long capture(ItemQuickCaptureReq.Destination destination) {
    ItemQuickCaptureReq req = new ItemQuickCaptureReq();
    req.setContent("验收：推进接口联调");
    req.setDestination(destination);
    return items.quickCapture(req);
  }

  private ItemResp change(Long id, ItemStatus status) {
    ItemTransitionReq req = new ItemTransitionReq();
    req.setVersion(items.detail(id).getVersion());
    req.setToStatus(status);
    if (status == ItemStatus.WAITING) {
      req.setWaitingFor("验收供应商");
    }
    return items.transition(id, req);
  }

  @Test
  void captureTodayStartsAndCompletesWithoutInboxTriage() {
    Long id = capture(ItemQuickCaptureReq.Destination.TODAY);
    assertEquals(ItemStatus.TODO, items.detail(id).getStatus());
    assertEquals(clock.today(), items.detail(id).getPlannedDate());
    assertTrue(
        today.today().getSections().getPlan().stream().anyMatch(it -> it.getId().equals(id)));
    assertEquals(ItemStatus.DOING, change(id, ItemStatus.DOING).getStatus());
    change(id, ItemStatus.DONE);
    assertFalse(
        today.today().getSections().getPlan().stream().anyMatch(it -> it.getId().equals(id)));
  }

  @Test
  void ordinaryInboxTaskCanBeFoundAndPlanned() {
    Long id = capture(ItemQuickCaptureReq.Destination.INBOX);
    assertTrue(inbox.inbox().getRecords().stream().anyMatch(it -> it.getId().equals(id)));
    change(id, ItemStatus.TODO);
    assertTrue(
        today.today().getSections().getSuggested().stream().anyMatch(it -> it.getId().equals(id)));
    ItemPlanReq plan = new ItemPlanReq();
    plan.setVersion(items.detail(id).getVersion());
    assertEquals(clock.today(), items.plan(id, plan).getPlannedDate());
  }

  @Test
  void pastPlanRemainsVisibleAndCanBeReplanned() {
    Long id = capture(ItemQuickCaptureReq.Destination.TODO);
    ItemPO po = mapper.selectById(id);
    po.setPlannedDate(clock.today().minusDays(2));
    mapper.updateById(po);
    assertTrue(
        today.today().getSections().getCarriedOverPlan().stream()
            .anyMatch(it -> it.getId().equals(id)));
    items.plan(id, new ItemPlanReq());
    assertTrue(
        today.today().getSections().getPlan().stream().anyMatch(it -> it.getId().equals(id)));
  }

  @Test
  void futurePlanDoesNotHideDueOrFollowUpRisk() {
    Long id = capture(ItemQuickCaptureReq.Destination.TODO);
    ItemPO po = mapper.selectById(id);
    po.setPlannedDate(clock.today().plusDays(3));
    po.setDueAt(clock.startOfDay(clock.today()).minusSeconds(60));
    po.setNextFollowUpAt(clock.now().minusSeconds(60));
    mapper.updateById(po);
    TodaySectionsResp sections = today.today().getSections();
    assertTrue(sections.getOverdue().stream().anyMatch(it -> it.getId().equals(id)));
    assertFalse(sections.getFollowUp().stream().anyMatch(it -> it.getId().equals(id)));
    assertTrue(items.detail(id).getFollowUpDue());
  }

  @Test
  void resolvedFollowUpDoesNotRequireChangingAnAlreadyDoingTask() {
    Long id = capture(ItemQuickCaptureReq.Destination.TODO);
    change(id, ItemStatus.DOING);
    ItemUpdateReq update = new ItemUpdateReq();
    update.setNextFollowUpAt(clock.now().minusSeconds(60));
    items.update(id, update);
    ItemFollowUpActionReq resolved = new ItemFollowUpActionReq();
    resolved.setOutcome(FollowUpOutcome.RESOLVED);
    resolved.setToStatus(ItemStatus.DOING);
    resolved.setVersion(items.detail(id).getVersion());
    ItemResp result = items.followUpAction(id, resolved);
    assertEquals(ItemStatus.DOING, result.getStatus());
    assertNull(result.getNextFollowUpAt());
  }

  @Test
  void batchAcceptPreservesFactsAndTouchesOnlyInboxItems() {
    ProjectCreateReq project = new ProjectCreateReq();
    project.setName("验收项目");
    Long projectId = projects.create(project);
    Long id = capture(ItemQuickCaptureReq.Destination.INBOX);
    Long existing = capture(ItemQuickCaptureReq.Destination.TODO);
    int previousHistory = items.activities(existing).size();
    ItemUpdateReq update = new ItemUpdateReq();
    update.setProjectId(projectId);
    update.setPriority(ItemPriority.P0);
    items.update(id, update);
    InboxAcceptAllReq req = new InboxAcceptAllReq();
    req.setItemIds(List.of(id, existing));
    assertEquals(1, inbox.acceptAll(req).getAccepted());
    assertEquals(projectId, items.detail(id).getProjectId());
    assertEquals(ItemPriority.P0, items.detail(id).getPriority());
    assertEquals(previousHistory, items.activities(existing).size());
  }

  @Test
  void clearingAttributesIsExplicitAndVersionProtected() {
    Long id = capture(ItemQuickCaptureReq.Destination.TODO);
    ItemUpdateReq update = new ItemUpdateReq();
    update.setDueAt(clock.now());
    items.update(id, update);
    int version = items.detail(id).getVersion();
    ItemUpdateReq clear = new ItemUpdateReq();
    clear.setVersion(version);
    clear.setClearDueAt(true);
    items.update(id, clear);
    assertNull(items.detail(id).getDueAt());
    assertThrows(BizException.class, () -> items.update(id, clear));
  }

  @Test
  void notesArchiveAndRestoreWithoutEnteringTodayOrDaily() {
    ItemCreateReq req = new ItemCreateReq();
    req.setTitle("验收笔记");
    req.setItemType(ItemType.NOTE);
    Long id = items.create(req);
    assertTrue(items.detail(id).getAllowedTransitions().contains(ItemStatus.DONE));
    change(id, ItemStatus.DONE);
    assertFalse(daily.generate(clock.today()).getGeneratedContent().contains("验收笔记"));
    assertEquals(ItemStatus.INBOX, change(id, ItemStatus.INBOX).getStatus());
    assertNull(items.detail(id).getCompletedAt());
  }

  @Test
  void generateThenApplyUsesNewVersionAndPreservesDraftUntilApplied() {
    LocalDate date = clock.today();
    DailyResp current = daily.get(date);
    DailyUpdateReq draft = new DailyUpdateReq();
    draft.setContent("我的原始草稿");
    draft.setVersion(current.getVersion());
    daily.update(date, draft);
    DailyGenerateResp generated = daily.generate(date);
    assertEquals("我的原始草稿", daily.get(date).getContent());
    DailyUpdateReq apply = new DailyUpdateReq();
    apply.setContent(generated.getGeneratedContent());
    apply.setVersion(generated.getVersion());
    assertEquals(generated.getGeneratedContent(), daily.applyGenerated(date, apply).getContent());
    assertThrows(BizException.class, () -> daily.applyGenerated(date, apply));
  }
}
