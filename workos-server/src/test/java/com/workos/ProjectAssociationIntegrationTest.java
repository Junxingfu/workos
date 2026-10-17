package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.daily.interfaces.http.DailyHttpAdapter;
import com.workos.item.application.query.ItemCriteria;
import com.workos.item.domain.ItemStatus;
import com.workos.item.interfaces.http.ItemHttpAdapter;
import com.workos.item.interfaces.request.ItemCreateReq;
import com.workos.item.interfaces.request.ItemPlanReq;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemTransitionReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.interfaces.http.ProjectHttpAdapter;
import com.workos.project.interfaces.request.ProjectContextUpdateReq;
import com.workos.project.interfaces.request.ProjectCreateReq;
import com.workos.today.application.query.TodayQuery;
import com.workos.worklog.interfaces.http.WorkLogHttpAdapter;
import com.workos.worklog.interfaces.request.WorkLogCreateReq;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 验证项目关联不会复制进度、日报或今日计划；每例在专用库中事务回滚。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@EnabledIfEnvironmentVariable(named = "WORKOS_INTEGRATION_TEST", matches = "true")
class ProjectAssociationIntegrationTest {
  @Autowired ItemHttpAdapter items;
  @Autowired ProjectHttpAdapter projects;
  @Autowired TodayQuery today;
  @Autowired DailyHttpAdapter daily;
  @Autowired WorkLogHttpAdapter logs;
  @Autowired TimeZoneHolder clock;

  private Long project(String name) {
    ProjectCreateReq req = new ProjectCreateReq();
    req.setName(name);
    return projects.create(req);
  }

  private Long task(Long main, Long other) {
    ItemCreateReq req = new ItemCreateReq();
    req.setTitle("关联回归任务");
    req.setProjectId(main);
    req.setRelatedProjectIds(List.of(other, other, main));
    req.setStatus(ItemStatus.TODO);
    return items.create(req);
  }

  private boolean inProject(Long id, Long project, ItemStatus status) {
    ItemCriteria req = new ItemCriteria();
    req.setProjectId(project);
    req.setStatus(status);
    return items.page(req).getDataList().stream().anyMatch(it -> it.getId().equals(id));
  }

  @Test
  void linkedProjectsShareIdentityProgressAndCountsWithoutDuplicatingToday() {
    Long main = project("回归主项目"), other = project("回归关联项目");
    Long id = task(main, other);
    assertEquals(1, items.detail(id).getRelatedProjects().size());
    assertTrue(inProject(id, main, ItemStatus.TODO));
    assertTrue(inProject(id, other, ItemStatus.TODO));
    assertEquals(1L, projects.detail(other).getStatistics().getTodo());
    items.plan(id, new ItemPlanReq());
    assertEquals(
        1,
        today.today().getSections().getPlan().stream().filter(it -> it.getId().equals(id)).count());
    ItemTransitionReq change = new ItemTransitionReq();
    change.setToStatus(ItemStatus.DOING);
    change.setVersion(items.detail(id).getVersion());
    items.transition(id, change);
    assertTrue(inProject(id, main, ItemStatus.DOING));
    assertTrue(inProject(id, other, ItemStatus.DOING));
    assertEquals(1L, projects.detail(other).getStatistics().getDoing());
    assertEquals(0L, projects.detail(other).getStatistics().getTodo());
  }

  @Test
  void staleContextDraftCannotOverwriteNewerSavedFacts() {
    Long id = project("现状版本回归");
    ProjectContextUpdateReq req = new ProjectContextUpdateReq();
    req.setVersion(projects.context(id).getVersion());
    req.setCurrentFocus("第一份现状");
    ProjectContextResp saved = projects.updateContext(id, req);
    assertTrue(saved.getVersion() > req.getVersion());
    req.setCurrentFocus("过期的空白页草稿");
    assertThrows(BizException.class, () -> projects.updateContext(id, req));
    req.setVersion(saved.getVersion());
    req.setCurrentFocus("新进展");
    ProjectContextResp updated = projects.updateContext(id, req);
    assertTrue(updated.getVersion() > saved.getVersion());
    req.setCurrentFocus("旧进展");
    assertThrows(BizException.class, () -> projects.updateContext(id, req));
    assertEquals("新进展", projects.context(id).getCurrentFocus());
  }

  @Test
  void workLogAndDailyBelongToMainProjectOnce() {
    Long main = project("日报主归属"), other = project("日报关联归属");
    Long id = task(main, other);
    WorkLogCreateReq req = new WorkLogCreateReq();
    req.setItemId(id);
    req.setContent("跨项目验收唯一记录");
    logs.create(req);
    assertEquals(1, logs.list(id, main, clock.today()).size());
    assertTrue(logs.list(id, other, clock.today()).isEmpty());
    String content = daily.generate(clock.today()).getGeneratedContent();
    assertEquals(1, content.split("跨项目验收唯一记录", -1).length - 1);
    assertTrue(content.contains("日报主归属"));
    assertFalse(content.contains("日报关联归属"));
  }

  @Test
  void invalidOrStaleAssociationUpdatesLeaveAllExistingFactsUntouched() {
    Long main = project("并发主项目"), other = project("并发关联项目");
    Long id = task(main, other);
    int version = items.detail(id).getVersion();
    ItemUpdateReq invalid = new ItemUpdateReq();
    invalid.setVersion(version);
    invalid.setTitle("不应保存");
    invalid.setRelatedProjectIds(List.of(Long.MAX_VALUE));
    assertThrows(BizException.class, () -> items.update(id, invalid));
    assertEquals("关联回归任务", items.detail(id).getTitle());
    assertEquals(version, items.detail(id).getVersion());
    ItemUpdateReq clear = new ItemUpdateReq();
    clear.setVersion(version);
    clear.setRelatedProjectIds(List.of());
    items.update(id, clear);
    ItemUpdateReq stale = new ItemUpdateReq();
    stale.setVersion(version);
    stale.setRelatedProjectIds(List.of(other));
    assertThrows(BizException.class, () -> items.update(id, stale));
    assertTrue(items.detail(id).getRelatedProjects().isEmpty());
    assertFalse(inProject(id, other, ItemStatus.TODO));
  }

  @Test
  void changingMainDeduplicatesAndClearingMainRequiresExplicitRemovalOfLinks() {
    Long main = project("切换主项目"), other = project("切换关联项目"), third = project("第三项目");
    Long id = task(main, other);
    ItemUpdateReq clear = new ItemUpdateReq();
    clear.setClearProject(true);
    assertThrows(BizException.class, () -> items.update(id, clear));
    ItemUpdateReq move = new ItemUpdateReq();
    move.setProjectId(other);
    move.setRelatedProjectIds(List.of(other, third));
    items.update(id, move);
    assertEquals(other, items.detail(id).getProjectId());
    assertEquals(
        List.of(third),
        items.detail(id).getRelatedProjects().stream().map(p -> p.getProjectId()).toList());
    clear.setRelatedProjectIds(List.of());
    items.update(id, clear);
    assertNull(items.detail(id).getProjectId());
    assertTrue(items.detail(id).getRelatedProjects().isEmpty());
  }

  @Test
  void projectCaptureInheritsMainProjectAndDestination() {
    Long main = project("快捷记录项目");
    ItemQuickCaptureReq req = new ItemQuickCaptureReq();
    req.setContent("项目里直接记下");
    req.setProjectId(main);
    req.setDestination(ItemQuickCaptureReq.Destination.TODAY);
    Long id = items.quickCapture(req);
    assertEquals(main, items.detail(id).getProjectId());
    assertEquals(clock.today(), items.detail(id).getPlannedDate());
    assertTrue(inProject(id, main, ItemStatus.TODO));
  }
}
