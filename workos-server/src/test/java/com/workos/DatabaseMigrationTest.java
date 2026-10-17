package com.workos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workos.shared.infrastructure.DatabaseMigrations;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.sqlite.SQLiteDataSource;

/** 迁移测试只创建临时库；验证真实 Flyway 接管旧库而不是模拟 SQL 执行。 */
class DatabaseMigrationTest {
  @TempDir Path directory;

  private SQLiteDataSource source() {
    SQLiteDataSource source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("workos.db"));
    return source;
  }

  private void migrate(SQLiteDataSource source) throws Exception {
    new DatabaseMigrations().flyway(source, "sqlite", directory.toString()).migrate();
  }

  @Test
  void freshWorkspaceAndRepeatedStartup() throws Exception {
    SQLiteDataSource source = source();
    migrate(source);
    migrate(source);
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement()) {
      try (ResultSet rows =
          sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success = 1")) {
        assertTrue(rows.next());
        assertEquals(3, rows.getInt(1));
      }
      try (ResultSet rows = sql.executeQuery("PRAGMA user_version")) {
        assertTrue(rows.next());
        assertEquals(3, rows.getInt(1));
      }
      try (ResultSet rows =
          sql.executeQuery(
              "SELECT description FROM schema_comment WHERE table_name='item' AND column_name='version'")) {
        assertTrue(rows.next());
        assertTrue(rows.getString(1).contains("乐观锁"));
      }
    }
  }

  private void legacyUpgrade(int legacyVersion) throws Exception {
    SQLiteDataSource source = source();
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement()) {
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/migration/sqlite/V1__initial_workspace.sql"));
      if (legacyVersion == 2) {
        ScriptUtils.executeSqlScript(
            connection,
            new ClassPathResource("db/migration/sqlite/V2__attention_and_spreadsheet_import.sql"));
      }
      sql.execute(
          "INSERT INTO project(name, status, created_at, updated_at, version) VALUES ('升级前的项目', 'ACTIVE', 1000, 1000, 131)");
    }
    migrate(source);
    migrate(source);
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement();
        ResultSet rows = sql.executeQuery("SELECT name, version FROM project")) {
      assertTrue(rows.next());
      assertEquals("升级前的项目", rows.getString(1));
      assertEquals(131, rows.getInt(2));
      assertFalse(rows.next());
    }
    try (Stream<Path> backups = Files.list(directory.resolve("backups"))) {
      assertEquals(
          1, backups.filter(p -> p.getFileName().toString().startsWith("before-flyway-")).count());
    }
  }

  @Test
  void adoptsLegacyV1WithoutLosingData() throws Exception {
    legacyUpgrade(1);
  }

  @Test
  void adoptsLegacyV2WithoutLosingData() throws Exception {
    legacyUpgrade(2);
  }

  @Test
  void rejectsUnknownPartialSchema() throws Exception {
    SQLiteDataSource source = source();
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement()) {
      sql.execute("CREATE TABLE item(id INTEGER PRIMARY KEY)");
    }
    assertThrows(IllegalStateException.class, () -> migrate(source));
  }

  @Test
  void rejectsFutureSchema() throws Exception {
    SQLiteDataSource source = source();
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement()) {
      sql.execute("PRAGMA user_version=99");
    }
    assertThrows(IllegalStateException.class, () -> migrate(source));
  }

  @Test
  void detectsChangedMigrationChecksum() throws Exception {
    SQLiteDataSource source = source();
    migrate(source);
    try (Connection connection = source.getConnection();
        Statement sql = connection.createStatement()) {
      sql.execute("UPDATE flyway_schema_history SET checksum=42 WHERE version='1'");
    }
    assertThrows(FlywayValidateException.class, () -> migrate(source));
  }
}
