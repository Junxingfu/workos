package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.daily.interfaces.http.DailyHttpAdapter;
import com.workos.daily.interfaces.request.DailyUpdateReq;
import com.workos.desktop.DesktopStorage;
import com.workos.item.interfaces.http.ItemHttpAdapter;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import com.workos.search.application.query.SearchCriteria;
import com.workos.search.application.query.SearchQuery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("desktop")
class DesktopStorageIntegrationTest {
  static final Path HOME;

  static {
    try {
      HOME = Files.createTempDirectory("workos-sqlite-regression-");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("workos.desktop.directory", HOME::toString);
  }

  @Autowired DesktopStorage storage;
  @Autowired ItemHttpAdapter items;
  @Autowired SearchQuery search;
  @Autowired DataSource dataSource;
  @Autowired DailyHttpAdapter dailies;

  @BeforeEach
  void clear() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      for (int i = DesktopStorage.TABLES.size() - 1; i >= 0; i--) {
        if (!DesktopStorage.TABLES.get(i).equals("settings")) {
          s.executeUpdate("DELETE FROM " + DesktopStorage.TABLES.get(i));
        }
      }
    }
  }

  private Long task(String title) {
    ItemQuickCaptureReq req = new ItemQuickCaptureReq();
    req.setContent(title);
    req.setDestination(ItemQuickCaptureReq.Destination.TODAY);
    return items.quickCapture(req);
  }

  @Test
  void viewingBlankDailyDoesNotBlockImportButEditingItDoes() throws Exception {
    LocalDate date = LocalDate.of(2026, 10, 13);
    dailies.get(date);
    assertEquals(true, storage.info().get("canImport"));
    DailyUpdateReq request = new DailyUpdateReq();
    request.setContent("# 今日工作\n已完成需求评审");
    dailies.update(date, request);
    assertEquals(false, storage.info().get("canImport"));
  }

  @Test
  void backupRestoresExactFactsAndKeepsSafetyCopy() throws Exception {
    Long id = task("恢复验证");
    String snapshot = storage.backup("manual");
    ItemUpdateReq update = new ItemUpdateReq();
    update.setTitle("修改后");
    items.update(id, update);
    storage.restore(snapshot);
    assertEquals("恢复验证", items.detail(id).getTitle());
    assertTrue(storage.info().get("backups").toString().contains("before-restore"));
  }

  @Test
  void invalidForeignKeysRollbackRestoreWithoutChangingCurrentData() throws Exception {
    Long id = task("保留当前事实");
    String snapshot = storage.backup("manual");
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:sqlite:" + HOME.resolve("backups").resolve(snapshot));
        Statement s = c.createStatement()) {
      s.executeUpdate(
          "INSERT INTO item_project_link(item_id, project_id) VALUES (" + id + ",999999)");
    }
    assertThrows(IllegalStateException.class, () -> storage.restore(snapshot));
    assertEquals("保留当前事实", items.detail(id).getTitle());
    assertTrue(items.detail(id).getRelatedProjects().isEmpty());
  }

  @Test
  void sqliteDatesAndLiteralSearchKeepBusinessMeaning() {
    Long id = task("code2name 100%_验收");
    Instant due = Instant.parse("2026-10-13T09:12:34.123Z");
    ItemUpdateReq update = new ItemUpdateReq();
    update.setDueAt(due);
    items.update(id, update);
    assertEquals(due, items.detail(id).getDueAt());
    SearchCriteria req = new SearchCriteria();
    req.setQ("100%_");
    assertEquals(1, search.search(req).getDataList().size());
    req.setQ("CODE2NAME");
    assertEquals(1, search.search(req).getDataList().size());
  }

  @Test
  void dailyBackupIsIdempotentAndRejectsTraversal() throws Exception {
    storage.dailyBackup();
    long before =
        ((List<?>) storage.info().get("backups"))
            .stream().filter(n -> n.toString().startsWith("auto-")).count();
    storage.dailyBackup();
    long after =
        ((List<?>) storage.info().get("backups"))
            .stream().filter(n -> n.toString().startsWith("auto-")).count();
    assertEquals(before, after);
    assertThrows(IllegalArgumentException.class, () -> storage.restore("../workos.db"));
  }

  @Test
  void restoresV1BackupAndInitializesNewTablesWithoutLosingTasks() throws Exception {
    Long id = task("v1 恢复验收");
    String snapshot = storage.backup("v1-fixture");
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:sqlite:" + HOME.resolve("backups").resolve(snapshot));
        Statement statement = c.createStatement()) {
      statement.execute("DROP TABLE attention_receipt");
      statement.execute("DROP TABLE spreadsheet_import");
      statement.execute("PRAGMA user_version=1");
    }
    ItemUpdateReq update = new ItemUpdateReq();
    update.setTitle("后来修改");
    items.update(id, update);
    Map<String, Long> counts = storage.restore(snapshot);
    assertEquals("v1 恢复验收", items.detail(id).getTitle());
    assertEquals(0L, counts.get("attention_receipt"));
    assertEquals(0L, counts.get("spreadsheet_import"));
  }
}
