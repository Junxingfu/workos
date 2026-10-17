package com.workos.desktop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** SQLite 启动配置：定位工作区、获取独占进程锁并配置连接，不承担业务 CRUD。 */
@Configuration
@Profile("desktop")
public class DesktopDatabase {
  private FileChannel channel;
  private FileLock lock;

  @PreDestroy
  public void unlock() throws Exception {
    if (lock != null) {
      lock.release();
    }
    if (channel != null) {
      channel.close();
    }
  }

  @Bean
  public DataSource dataSource(@Value("${workos.desktop.directory}") String directory)
      throws Exception {
    Path home = Path.of(directory).toAbsolutePath();
    Files.createDirectories(home);
    channel =
        FileChannel.open(
            home.resolve("database.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    lock = channel.tryLock();
    if (lock == null) {
      channel.close();
      throw new IllegalStateException("工作区已被另一个 WorkOS 实例打开");
    }
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + home.resolve("workos.db"));
    config.setDriverClassName("org.sqlite.JDBC");
    config.setMaximumPoolSize(1);
    config.addDataSourceProperty("foreign_keys", "true");
    config.addDataSourceProperty("busy_timeout", "10000");
    config.addDataSourceProperty("journal_mode", "WAL");
    HikariDataSource source = new HikariDataSource(config);
    // Flyway 在数据源建立后负责建库和升级；这里只管理文件锁与连接配置。
    return source;
  }
}
