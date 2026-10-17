package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.item.domain.FollowUpOutcome;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.ProjectReferences;
import com.workos.item.domain.WaitingPeriod;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 可独立阅读/运行的领域示例：不启动 Spring，不连数据库。 */
class WorkItemDomainTest {
  private static final Instant NOW = Instant.parse("2026-10-17T01:00:00Z");

  private WorkItem task(ItemStatus status) {
    return new WorkItem(
        new WorkItem.State(
            42,
            3,
            ItemType.TASK,
            status,
            status == ItemStatus.WAITING ? "王工" : null,
            status == ItemStatus.WAITING ? NOW.minusSeconds(172800) : null,
            null,
            NOW.minusSeconds(60),
            null,
            null,
            null,
            null));
  }

  @Test
  void waitingIsAValueAndCannotLoseItsPerson() {
    assertEquals(new WaitingPeriod(" 王工 ", NOW, null), new WaitingPeriod("王工", NOW, null));
    WorkItem task = task(ItemStatus.WAITING);
    WorkItem.State before = task.state();
    assertThrows(IllegalArgumentException.class, () -> task.reviseTracking(" ", null, false, NOW));
    assertEquals(before, task.state());
    assertTrue(task.events().isEmpty());
  }

  @Test
  void receivingResultEndsWaitingAndProducesIdentifiableFacts() {
    WorkItem task = task(ItemStatus.WAITING);
    String note = task.followUp(FollowUpOutcome.RESOLVED, ItemStatus.TODO, null, null, NOW);
    assertEquals("王工 已给出结果", note);
    assertEquals(ItemStatus.TODO, task.state().status());
    assertNull(task.state().waitingFor());
    assertNull(task.state().nextFollowUpAt());
    assertEquals(new WorkItemEvent.WaitingEnded(42, ItemStatus.TODO, 2, NOW), task.events().get(0));
    assertTrue(task.events().stream().allMatch(event -> event.itemId() == 42));
    task.markPersisted();
    assertEquals(4, task.state().version());
    assertTrue(task.events().isEmpty());
  }

  @Test
  void deferringRequiresAFutureTimeAndDoesNotChangeTaskProgress() {
    WorkItem task = task(ItemStatus.WAITING);
    WorkItem.State before = task.state();
    assertThrows(
        IllegalArgumentException.class,
        () -> task.followUp(FollowUpOutcome.STILL_WAITING, null, NOW, null, NOW));
    assertEquals(before, task.state());
    assertTrue(task.events().isEmpty());
    task.followUp(FollowUpOutcome.STILL_WAITING, null, null, "电话未接通", NOW);
    assertEquals(NOW.plusSeconds(172800), task.state().nextFollowUpAt());
    assertEquals(ItemStatus.WAITING, task.state().status());
    assertFalse(task.events().isEmpty());
  }

  @Test
  void abandoningFollowUpIsNotCancellingTheTask() {
    WorkItem task = task(ItemStatus.WAITING);
    assertNull(task.followUp(FollowUpOutcome.ABANDONED, null, null, null, NOW));
    assertEquals(ItemStatus.WAITING, task.state().status());
    assertEquals("王工", task.state().waitingFor());
    assertNull(task.state().nextFollowUpAt());
    assertThrows(
        IllegalArgumentException.class,
        () -> task(ItemStatus.DONE).followUp(FollowUpOutcome.STILL_WAITING, null, null, null, NOW));
  }

  @Test
  void projectReferencesShareOneMainAndCannotBeMutatedThroughACollection() {
    ProjectReferences references = new ProjectReferences(10L, List.of(10L, 20L, 20L, 30L));
    assertEquals(List.of(20L, 30L), references.relatedProjectIds());
    assertThrows(
        UnsupportedOperationException.class, () -> references.relatedProjectIds().add(40L));
    assertThrows(IllegalArgumentException.class, () -> new ProjectReferences(null, List.of(20L)));
  }
}
