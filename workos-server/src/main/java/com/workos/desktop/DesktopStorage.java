package com.workos.desktop;

import com.workos.common.support.TimeZoneHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** 备份使用 SQLite 一致性快照；导入/恢复在一个事务内完成，原 PostgreSQL 只读。 */
@Service @Profile("desktop")
public class DesktopStorage {
    public static final List<String> TABLES = List.of("project", "project_context", "item", "item_project_link", "work_log", "item_activity", "daily", "reminder", "settings", "account");
    private final DataSource dataSource;
    private final Path directory;
    private final TimeZoneHolder clock;
    public DesktopStorage(DataSource source, @Value("${workos.desktop.directory}") String directory, TimeZoneHolder clock) throws Exception {
        this.dataSource = source; this.directory = Path.of(directory).toAbsolutePath(); this.clock = clock;
        Files.createDirectories(this.directory.resolve("backups"));
    }
    public Map<String, Long> counts(Connection c) throws SQLException {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String table : TABLES) try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next(); result.put(table, r.getLong(1));
        }
        return result;
    }
    public synchronized Map<String, Object> info() throws Exception {
        try (Connection c = dataSource.getConnection(); var files = Files.list(directory.resolve("backups"))) {
            return Map.of("directory", directory.toString(), "example", Boolean.parseBoolean(System.getenv("WORKOS_DESKTOP_EXAMPLE")), "canImport", canImport(c), "counts", counts(c), "backups", files.filter(p -> p.toString().endsWith(".db")).sorted(Comparator.comparingLong(DesktopStorage::modified).reversed()).map(p -> p.getFileName().toString()).toList());
        }
    }
    private static long modified(Path file) {
        try { return Files.getLastModifiedTime(file).toMillis(); } catch (java.io.IOException e) { return 0; }
    }
    private boolean canImport(Connection c) throws SQLException {
        if (counts(c).entrySet().stream().anyMatch(e -> !Set.of("settings", "daily").contains(e.getKey()) && e.getValue() > 0)) return false;
        // 首次查看日报会创建默认模板；它不是用户已录入的工作事实。
        try (Statement s = c.createStatement(); ResultSet rows = s.executeQuery("SELECT content, generated_content, version FROM daily")) {
            while (rows.next()) if (!"# 今日工作\n\n".equals(rows.getString(1)) || rows.getString(2) != null || rows.getInt(3) != 0) return false;
        }
        return true;
    }
    public synchronized String backup(String prefix) throws Exception {
        String name = prefix + "-" + Instant.now().toString().replace(':', '-') + ".db";
        Path target = directory.resolve("backups").resolve(name);
        Path partial = directory.resolve("backups").resolve(name + ".partial");
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("VACUUM INTO '" + partial.toString().replace("'", "''") + "'");
            Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(partial); }
        return name;
    }
    public synchronized void dailyBackup() throws Exception {
        String prefix = "auto-" + LocalDate.now(clock.zone());
        try (var files = Files.list(directory.resolve("backups"))) {
            if (files.anyMatch(p -> p.getFileName().toString().startsWith(prefix) && p.toString().endsWith(".db"))) return;
        }
        backup(prefix);
        try (var files = Files.list(directory.resolve("backups"))) {
            for (Path old : files.filter(p -> p.getFileName().toString().startsWith("auto-") && p.toString().endsWith(".db")).sorted(Comparator.reverseOrder()).skip(14).toList()) Files.delete(old);
        }
    }
    public synchronized Map<String, Long> importPostgres(String database) throws Exception {
        if (database == null || !Set.of("workos", "workos_usability_20261011").contains(database)) throw new IllegalArgumentException("只能导入本机已知的工作库或示例库");
        // 此入口专用于当前项目的本地 compose 默认数据库，不接收任意 JDBC URL。
        try (Connection source = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/" + database + "?connectTimeout=5&socketTimeout=30", "workos", "workos")) {
            source.setReadOnly(true); source.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); source.setAutoCommit(false);
            backup("before-import");
            return copy(source, false);
        }
    }
    public synchronized Map<String, Long> restore(String name) throws Exception {
        if (name == null || !name.matches("[A-Za-z0-9T:.Z_-]+\\.db")) throw new IllegalArgumentException("备份名称无效");
        Path file = directory.resolve("backups").resolve(name);
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) throw new IllegalArgumentException("备份不存在");
        try (Connection source = DriverManager.getConnection("jdbc:sqlite:file:" + file + "?mode=ro")) {
            try (Statement s = source.createStatement(); ResultSet r = s.executeQuery("PRAGMA quick_check")) {
                if (!r.next() || !"ok".equals(r.getString(1))) throw new IllegalArgumentException("备份完整性检查失败");
            }
            try (Statement s = source.createStatement(); ResultSet r = s.executeQuery("PRAGMA user_version")) {
                if (!r.next() || r.getInt(1) != 1) throw new IllegalArgumentException("备份版本不兼容");
            }
            backup("before-restore");
            return copy(source, true);
        }
    }
    private Map<String, Long> copy(Connection source, boolean replace) throws Exception {
        Map<String, Long> expected = counts(source);
        try (Connection target = dataSource.getConnection()) {
            target.setAutoCommit(false);
            try {
                if (!replace && !canImport(target)) throw new IllegalStateException("当前工作区已有数据。为避免覆盖，仅允许导入到空工作区。");
                try (Statement s = target.createStatement()) {
                    s.execute("PRAGMA defer_foreign_keys = ON");
                    for (int i = TABLES.size() - 1; i >= 0; i--) s.executeUpdate("DELETE FROM " + TABLES.get(i));
                }
                for (String table : TABLES) {
                    List<String> columns = new ArrayList<>();
                    try (Statement s = target.createStatement(); ResultSet r = s.executeQuery("PRAGMA table_info(" + table + ")")) { while (r.next()) columns.add(r.getString("name")); }
                    String fields = String.join(",", columns);
                    try (Statement read = source.createStatement(); ResultSet rows = read.executeQuery("SELECT " + fields + " FROM " + table);
                         PreparedStatement insert = target.prepareStatement("INSERT INTO " + table + " (" + fields + ") VALUES (" + String.join(",", Collections.nCopies(columns.size(), "?")) + ")")) {
                        while (rows.next()) {
                            for (int i = 1; i <= columns.size(); i++) {
                                Object value = rows.getObject(i);
                                if (value instanceof Timestamp t) value = t.toInstant().toEpochMilli();
                                if (value instanceof java.sql.Date d) value = d.toLocalDate().toString();
                                if (value instanceof Boolean b) value = b ? 1 : 0;
                                insert.setObject(i, value);
                            }
                            insert.executeUpdate();
                        }
                    }
                }
                if (!expected.equals(counts(target))) throw new IllegalStateException("导入数量核对失败");
                try (Statement s = target.createStatement(); ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
                    if (r.next()) throw new IllegalStateException("关联完整性检查失败，已回滚");
                }
                target.commit();
                try (Statement s = target.createStatement(); ResultSet r = s.executeQuery("SELECT timezone, day_boundary_hour FROM settings WHERE id=1")) {
                    if (r.next()) clock.refresh(r.getString(1), r.getInt(2));
                }
                return expected;
            } catch (Exception e) { target.rollback(); throw e; }
            finally { target.setAutoCommit(true); }
        }
    }
}
