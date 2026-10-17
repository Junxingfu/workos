package com.workos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.daily.domain.DailyComposition;
import com.workos.daily.domain.DailyReport;
import com.workos.item.domain.ItemDetails;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemRevision;
import com.workos.item.domain.ItemSource;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.ProjectReferences;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemEvent;
import com.workos.project.domain.Project;
import com.workos.reminder.domain.Reminder;
import com.workos.settings.domain.WorkspaceSettings;
import com.workos.shared.domain.ConcurrentChange;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** IDDD 行为示例：不启动 Spring，也不加载 PO 或数据库。 */
class AggregateBehaviorTest {
  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

  @Test
  void archiveAndRestoreOnlyChangeProjectLifecycle() {
    Project project = Project.create("发布准备", null, null, null, null, NOW);
    project.archive(NOW);
    assertEquals("ARCHIVED", project.snapshot().status());
    assertEquals(NOW, project.snapshot().archivedAt());
    project.restore(NOW.plusSeconds(1));
    assertEquals("ACTIVE", project.snapshot().status());
    assertNull(project.snapshot().archivedAt());
  }

  @Test
  void versionsAreComparedByValueBeyondIntegerCache() {
    Project project = Project.create("版本检查", null, null, null, null, NOW);
    project.persisted(1L, 200);
    assertDoesNotThrow(() -> project.expect(Integer.valueOf("200")));
    assertThrows(ConcurrentChange.class, () -> project.expect(199));
  }

  @Test
  void generatingDoesNotOverwriteReviewedText() {
    DailyReport report = DailyReport.draft(TODAY, NOW);
    report.write("我手工整理的正文", NOW);
    report.generate("新快照", NOW);
    assertEquals("我手工整理的正文", report.snapshot().content());
    assertEquals("新快照", report.snapshot().generatedContent());
    report.apply("已核对的快照", NOW);
    assertEquals("CONFIRMED", report.snapshot().status());
    assertEquals("已核对的快照", report.snapshot().content());
  }

  @Test
  void compositionGroupsPrimaryProjectOnceAndKeepsUnassignedLast() {
    DailyComposition.Source source =
        new DailyComposition.Source(
            List.of(
                new DailyComposition.Entry(null, "零散沟通"), new DailyComposition.Entry("平台", "统一升级")),
            List.of("联调完成"));
    String text = DailyComposition.compose(TODAY, source);
    assertTrue(text.indexOf("## 平台") < text.indexOf("## 未归属"));
    assertEquals(1, text.split("统一升级", -1).length - 1);
    assertTrue(text.contains("## 完成事项"));
  }

  @Test
  void dismissedReminderCannotBeRescheduled() {
    Reminder reminder = Reminder.schedule("STANDALONE", null, "喝水", NOW.plusSeconds(60), NOW);
    reminder.dismiss(NOW);
    assertThrows(IllegalArgumentException.class, () -> reminder.reschedule(NOW.plusSeconds(120)));
  }

  @Test
  void invalidSettingsDoNotPartiallyChangeTimezone() {
    WorkspaceSettings settings = WorkspaceSettings.defaults(NOW);
    WorkspaceSettings.State before = settings.snapshot();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            settings.revise(
                new WorkspaceSettings.Revision("UTC", null, null, 25, null, null, null, null),
                NOW));
    assertEquals(before, settings.snapshot());
  }

  @Test
  void rejectedCompoundRevisionLeavesItemAndEventsUnchanged() {
    ItemDetails details =
        new ItemDetails(
            "部署",
            null,
            new ProjectReferences(1L, List.of(2L)),
            ItemPriority.P2,
            null,
            null,
            ItemSource.MANUAL);
    WorkItem item =
        WorkItem.create(details, ItemType.TASK, ItemStatus.TODO, null, null, null, TODAY, 0, NOW);
    WorkItem.State before = item.state();
    List<WorkItemEvent> events = item.events();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            item.revise(
                new ItemRevision(
                    "另一个标题",
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    TODAY.minusDays(1),
                    null,
                    false,
                    null,
                    null,
                    false,
                    null),
                TODAY,
                1,
                NOW));
    assertEquals(before, item.state());
    assertEquals(details, item.details());
    assertEquals(events, item.events());
  }
}
