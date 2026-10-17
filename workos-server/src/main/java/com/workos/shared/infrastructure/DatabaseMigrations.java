package com.workos.shared.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 统一迁移入口：只识别已知旧版本并建立基线，禁止对未知业务库盲目 baseline。 */
@Configuration
public class DatabaseMigrations {
  @Bean(initMethod = "migrate")
  public Flyway flyway(
      DataSource source,
      @Value("${workos.database:postgresql}") String vendor,
      @Value("${workos.desktop.directory:}") String directory)
      throws Exception {
    boolean sqlite = "sqlite".equals(vendor);
    String dialect = sqlite ? "sqlite" : "postgresql";
    FluentConfiguration config =
        Flyway.configure()
            .dataSource(source)
            .locations("classpath:db/migration/" + dialect)
            .cleanDisabled(true)
            .baselineOnMigrate(false)
            .validateMigrationNaming(true);
    Integer legacyVersion = null;
    boolean hasHistory;
    try (Connection connection = source.getConnection()) {
      if (sqlite) {
        try (Statement statement = connection.createStatement();
            ResultSet rows = statement.executeQuery("PRAGMA user_version")) {
          if (rows.next() && rows.getInt(1) > 3) {
            throw new IllegalStateException("请使用更新版本的 WorkOS 打开此数据库");
          }
        }
      }
      hasHistory = exists(connection, "flyway_schema_history");
      if (!hasHistory && exists(connection, "item")) {
        // 缺少任何核心表都不视为受支持旧库，以免把损坏或无关库当作可升级工作区。
        for (String table :
            List.of(
                "project",
                "project_context",
                "item_activity",
                "work_log",
                "daily",
                "reminder",
                "settings",
                "account")) {
          if (!exists(connection, table)) {
            throw new IllegalStateException("旧库缺少必要表：" + table);
          }
        }
        boolean linked = exists(connection, "item_project_link");
        boolean receipts = exists(connection, "attention_receipt");
        boolean imports = exists(connection, "spreadsheet_import");
        if (receipts != imports || (receipts && !linked) || (sqlite && !linked)) {
          throw new IllegalStateException("旧库结构不完整，不能自动建立迁移基线");
        }
        int baseline = sqlite ? (receipts ? 2 : 1) : (receipts ? 3 : linked ? 2 : 1);
        if (sqlite) {
          backup(connection, Path.of(directory), "before-flyway");
        }
        legacyVersion = baseline;
      }
    }
    // 释放连接后再调用 Flyway；桌面连接池只有一个连接，嵌套借用会自锁。
    if (legacyVersion != null) {
      Flyway flyway =
          config
              .baselineVersion(legacyVersion.toString())
              .baselineDescription("Verified legacy WorkOS schema")
              .load();
      flyway.baseline();
      return flyway;
    }
    if (sqlite && hasHistory) {
      Flyway flyway = config.load();
      if (flyway.info().pending().length > 0) {
        try (Connection connection = source.getConnection()) {
          backup(connection, Path.of(directory), "before-migration");
        }
      }
      return flyway;
    }
    return config.load();
  }

  private boolean exists(Connection connection, String table) throws Exception {
    try (ResultSet rows =
        connection.getMetaData().getTables(null, null, table, new String[] {"TABLE"})) {
      return rows.next();
    }
  }

  private void backup(Connection connection, Path directory, String reason) throws Exception {
    Path folder = directory.resolve("backups");
    Files.createDirectories(folder);
    Path backup = folder.resolve(reason + "-" + Instant.now().toEpochMilli() + ".db");
    try (Statement statement = connection.createStatement()) {
      statement.execute("VACUUM INTO '" + backup.toString().replace("'", "''") + "'");
    }
  }
}
