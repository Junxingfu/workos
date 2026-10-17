package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.attention.application.AttentionApplicationService;
import com.workos.attention.infrastructure.persistence.mapper.AttentionReceiptMapper;
import com.workos.common.support.TimeZoneHolder;
import com.workos.exchange.application.TaskExchangeService;
import com.workos.exchange.infrastructure.TaskWorkbook;
import com.workos.exchange.infrastructure.persistence.mapper.SpreadsheetImportMapper;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.WorkItem;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.interfaces.http.ItemHttpAdapter;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemTransitionReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("desktop")
class TaskManagementIntegrationTest {
  static final Path DIRECTORY;

  static {
    try {
      DIRECTORY = Files.createTempDirectory("workos-task-management-test-");
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("workos.desktop.directory", DIRECTORY::toString);
  }

  @Autowired TaskExchangeService exchange;
  @Autowired ItemHttpAdapter items;
  @Autowired ItemMapper mapper;
  @Autowired AttentionApplicationService attention;
  @Autowired AttentionReceiptMapper receipts;
  @Autowired SpreadsheetImportMapper imports;
  @Autowired TimeZoneHolder clock;

  @BeforeEach
  void clear() {
    mapper.delete(null);
    receipts.delete(null);
    imports.delete(null);
  }

  private long task(String title) {
    ItemQuickCaptureReq req = new ItemQuickCaptureReq();
    req.setContent(title);
    req.setDestination(ItemQuickCaptureReq.Destination.TODAY);
    return items.quickCapture(req);
  }

  private List<String> newRow(String title) {
    ArrayList<String> row = new ArrayList<>(Collections.nCopies(14, ""));
    row.set(3, title);
    return row;
  }

  private byte[] book(List<List<String>> rows) throws Exception {
    return TaskWorkbook.write(rows, List.of(), clock.zone().toString());
  }

  @Test
  void roundTripPreviewIsReadOnlyAndApplyIsIdempotent() throws Exception {
    long id = task("中文任务 = 不是公式");
    List<TaskWorkbook.Row> exported = TaskWorkbook.read(exchange.export(false));
    ArrayList<String> row = new ArrayList<>(exported.get(0).cells());
    row.set(3, "Excel 修改后的任务");
    row.set(4, "已完成");
    List<String> added = newRow("批量新增，安排今天");
    added.set(8, clock.today().toString());
    byte[] bytes = book(List.of(row, added));
    TaskExchangeService.Preview preview = exchange.preview(bytes);
    assertTrue(preview.canApply());
    assertEquals("中文任务 = 不是公式", items.detail(id).getTitle());
    assertEquals(1L, mapper.selectCount(null));
    TaskExchangeService.Result result = exchange.apply(preview.token());
    assertEquals(1, result.created());
    assertEquals(1, result.updated());
    assertEquals(ItemStatus.DONE, items.detail(id).getStatus());
    assertNotNull(items.detail(id).getCompletedAt());
    assertEquals(result, exchange.apply(preview.token()));
    assertFalse(exchange.preview(bytes).canApply());
    assertEquals(2L, mapper.selectCount(null));
  }

  @Test
  void excelCanEndWaitingWithoutAnInvalidIntermediateAggregate() throws Exception {
    long id = task("等待验收结果");
    ItemTransitionReq change = new ItemTransitionReq();
    change.setToStatus(ItemStatus.WAITING);
    change.setWaitingFor("验收同事");
    change.setNextFollowUpAt(clock.now().plusSeconds(3600));
    items.transition(id, change);
    ArrayList<String> row =
        new ArrayList<>(TaskWorkbook.read(exchange.export(false)).get(0).cells());
    row.set(4, "已完成");
    TaskExchangeService.Preview preview = exchange.preview(book(List.of(row)));
    assertTrue(preview.canApply());
    exchange.apply(preview.token());
    ItemResp result = items.detail(id);
    assertEquals(ItemStatus.DONE, result.getStatus());
    assertEquals(null, result.getWaitingFor());
    assertEquals(null, result.getNextFollowUpAt());
    assertNotNull(result.getCompletedAt());
  }

  @Test
  void staleVersionPreventsEntireBatchIncludingNewRows() throws Exception {
    long id = task("原任务");
    ArrayList<String> old =
        new ArrayList<>(TaskWorkbook.read(exchange.export(false)).get(0).cells());
    old.set(3, "旧表修改");
    TaskExchangeService.Preview preview = exchange.preview(book(List.of(newRow("不能部分新增"), old)));
    assertTrue(preview.canApply());
    ItemUpdateReq update = new ItemUpdateReq();
    update.setTitle("应用已更新");
    items.update(id, update);
    assertThrows(IllegalArgumentException.class, () -> exchange.apply(preview.token()));
    assertEquals(1L, mapper.selectCount(null));
    assertEquals("应用已更新", items.detail(id).getTitle());
  }

  @Test
  void blanksKeepValuesButClearMarkerRemovesDates() throws Exception {
    long id = task("保留标题");
    ItemUpdateReq update = new ItemUpdateReq();
    update.setDescription("保留描述");
    update.setDueAt(clock.now());
    items.update(id, update);
    ArrayList<String> row =
        new ArrayList<>(TaskWorkbook.read(exchange.export(false)).get(0).cells());
    row.set(12, "");
    row.set(9, "[清空]");
    row.set(8, "[清空]");
    TaskExchangeService.Preview preview = exchange.preview(book(List.of(row)));
    assertTrue(preview.canApply());
    exchange.apply(preview.token());
    assertEquals("保留描述", items.detail(id).getDescription());
    assertEquals(null, items.detail(id).getDueAt());
    assertEquals(null, items.detail(id).getPlannedDate());
  }

  @Test
  void rejectsInvalidTransitionDuplicateIdsAndFormula() throws Exception {
    task("原任务");
    ArrayList<String> row =
        new ArrayList<>(TaskWorkbook.read(exchange.export(false)).get(0).cells());
    row.set(4, "待整理");
    assertFalse(exchange.preview(book(List.of(row))).canApply());
    row.set(4, "待办");
    assertFalse(exchange.preview(book(List.of(row, row))).canApply());
    try (XSSFWorkbook workbook =
            new XSSFWorkbook(new ByteArrayInputStream(book(List.of(newRow("公式")))));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      workbook.getSheet("任务").getRow(1).getCell(3).setCellFormula("1+1");
      workbook.write(out);
      assertThrows(IllegalArgumentException.class, () -> exchange.preview(out.toByteArray()));
    }
  }

  @Test
  void notificationReceiptDoesNotChangeTaskAndNewDateCreatesNewNotice() {
    long id = task("该跟进的任务");
    ItemUpdateReq update = new ItemUpdateReq();
    update.setDueAt(clock.now().minusSeconds(120));
    items.update(id, update);
    ItemResp before = items.detail(id);
    AttentionApplicationService.Notice notice =
        attention.list().stream().filter(n -> n.kind().equals("DUE")).findFirst().orElseThrow();
    attention.respond(notice.key(), "READ");
    assertEquals(before.getVersion(), items.detail(id).getVersion());
    assertEquals(ItemStatus.TODO, items.detail(id).getStatus());
    assertTrue(
        attention.list().stream()
            .filter(n -> n.key().equals(notice.key()))
            .findFirst()
            .orElseThrow()
            .read());
    attention.respond(notice.key(), "SNOOZE");
    assertTrue(
        attention.list().stream()
            .filter(n -> n.key().equals(notice.key()))
            .findFirst()
            .orElseThrow()
            .snoozedUntil()
            .isAfter(clock.now()));
    update = new ItemUpdateReq();
    update.setDueAt(clock.now().minusSeconds(60));
    items.update(id, update);
    AttentionApplicationService.Notice fresh =
        attention.list().stream().filter(n -> n.kind().equals("DUE")).findFirst().orElseThrow();
    assertFalse(fresh.read());
    assertFalse(fresh.key().equals(notice.key()));
    ItemTransitionReq transition = new ItemTransitionReq();
    transition.setToStatus(ItemStatus.DONE);
    items.transition(id, transition);
    assertTrue(attention.list().isEmpty());
  }

  @Test
  void pureAggregateEndsWaitingAndReopeningClearsCompletion() {
    Instant now = Instant.parse("2026-10-15T10:00:00Z");
    WorkItem task =
        new WorkItem(
            new WorkItem.State(
                1,
                0,
                ItemType.TASK,
                ItemStatus.WAITING,
                "同事",
                now.minusSeconds(86400),
                now,
                now,
                null,
                null,
                null,
                null));
    task.transition(ItemStatus.DONE, null, null, null, "完成", now);
    assertEquals(null, task.state().waitingFor());
    assertEquals(null, task.state().nextFollowUpAt());
    assertEquals(2, task.events().size());
    assertEquals(now, task.state().completedAt());
    task.transition(ItemStatus.TODO, null, null, null, "恢复", now);
    assertEquals(null, task.state().completedAt());
    assertThrows(
        IllegalArgumentException.class,
        () -> task.plan(clock.today().minusDays(1), clock.today(), 1, now));
  }
}
