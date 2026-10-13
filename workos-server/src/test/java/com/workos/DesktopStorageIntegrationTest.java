package com.workos;

import com.workos.desktop.DesktopStorage;
import com.workos.item.model.request.*;
import com.workos.item.service.ItemService;
import com.workos.search.model.request.SearchReq;
import com.workos.search.service.SearchService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("desktop")
class DesktopStorageIntegrationTest {
    static final Path HOME;
    static { try { HOME = Files.createTempDirectory("workos-sqlite-regression-"); } catch (Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("workos.desktop.directory", HOME::toString); }
    @Autowired DesktopStorage storage;
    @Autowired ItemService items;
    @Autowired SearchService search;
    @Autowired DataSource dataSource;
    @Autowired com.workos.daily.service.DailyService dailies;
    @BeforeEach void clear() throws Exception {
        try (var c = dataSource.getConnection(); var s = c.createStatement()) {
            for (int i = DesktopStorage.TABLES.size()-1; i >= 0; i--) if (!DesktopStorage.TABLES.get(i).equals("settings")) s.executeUpdate("DELETE FROM " + DesktopStorage.TABLES.get(i));
        }
    }
    private Long task(String title) {
        var req = new ItemQuickCaptureReq(); req.setContent(title); req.setDestination(ItemQuickCaptureReq.Destination.TODAY);
        return items.quickCapture(req);
    }
    @Test void viewingBlankDailyDoesNotBlockImportButEditingItDoes() throws Exception {
        var date = java.time.LocalDate.of(2026, 10, 13);
        dailies.get(date);
        assertEquals(true, storage.info().get("canImport"));
        var request = new com.workos.daily.model.request.DailyUpdateReq();
        request.setContent("# 今日工作\n已完成需求评审");
        dailies.update(date, request);
        assertEquals(false, storage.info().get("canImport"));
    }
    @Test void backupRestoresExactFactsAndKeepsSafetyCopy() throws Exception {
        Long id = task("恢复验证");
        String snapshot = storage.backup("manual");
        var update = new ItemUpdateReq(); update.setTitle("修改后"); items.update(id, update);
        storage.restore(snapshot);
        assertEquals("恢复验证", items.detail(id).getTitle());
        assertTrue(storage.info().get("backups").toString().contains("before-restore"));
    }
    @Test void invalidForeignKeysRollbackRestoreWithoutChangingCurrentData() throws Exception {
        Long id = task("保留当前事实");
        String snapshot = storage.backup("manual");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + HOME.resolve("backups").resolve(snapshot)); var s = c.createStatement()) {
            s.executeUpdate("INSERT INTO item_project_link(item_id, project_id) VALUES (" + id + ",999999)");
        }
        assertThrows(IllegalStateException.class, () -> storage.restore(snapshot));
        assertEquals("保留当前事实", items.detail(id).getTitle());
        assertTrue(items.detail(id).getRelatedProjects().isEmpty());
    }
    @Test void sqliteDatesAndLiteralSearchKeepBusinessMeaning() {
        Long id = task("code2name 100%_验收");
        Instant due = Instant.parse("2026-10-13T09:12:34.123Z");
        var update = new ItemUpdateReq(); update.setDueAt(due); items.update(id, update);
        assertEquals(due, items.detail(id).getDueAt());
        var req = new SearchReq(); req.setQ("100%_");
        assertEquals(1, search.search(req).getDataList().size());
        req.setQ("CODE2NAME"); assertEquals(1, search.search(req).getDataList().size());
    }
    @Test void dailyBackupIsIdempotentAndRejectsTraversal() throws Exception {
        storage.dailyBackup();
        long before = ((java.util.List<?>) storage.info().get("backups")).stream().filter(n -> n.toString().startsWith("auto-")).count();
        storage.dailyBackup();
        long after = ((java.util.List<?>) storage.info().get("backups")).stream().filter(n -> n.toString().startsWith("auto-")).count();
        assertEquals(before, after);
        assertThrows(IllegalArgumentException.class, () -> storage.restore("../workos.db"));
    }
}
