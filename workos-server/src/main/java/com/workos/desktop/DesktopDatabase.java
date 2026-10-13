package com.workos.desktop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;
import java.nio.channels.*;
import jakarta.annotation.PreDestroy;

@Configuration
@Profile("desktop")
public class DesktopDatabase {
    private FileChannel channel;
    private FileLock lock;
    @PreDestroy public void unlock() throws Exception { if (lock != null) lock.release(); if (channel != null) channel.close(); }
    @Bean
    public DataSource dataSource(@Value("${workos.desktop.directory}") String directory) throws Exception {
        Path home = Path.of(directory).toAbsolutePath();
        Files.createDirectories(home);
        channel = FileChannel.open(home.resolve("database.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        lock = channel.tryLock();
        if (lock == null) { channel.close(); throw new IllegalStateException("工作区已被另一个 WorkOS 实例打开"); }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + home.resolve("workos.db"));
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("busy_timeout", "10000");
        config.addDataSourceProperty("journal_mode", "WAL");
        HikariDataSource source = new HikariDataSource(config);
        try (Connection c = source.getConnection(); Statement s = c.createStatement()) {
            int version;
            try (ResultSet r = s.executeQuery("PRAGMA user_version")) { version = r.getInt(1); }
            if (version > 1) throw new IllegalStateException("此数据由较新版本创建，请使用新版 WorkOS。");
            if (version == 0) {
                c.setAutoCommit(false);
                try {
                    ScriptUtils.executeSqlScript(c, new ClassPathResource("desktop/schema-v1.sql"));
                    s.execute("PRAGMA user_version = 1");
                    c.commit();
                } catch (Exception e) { c.rollback(); throw e; }
                finally { c.setAutoCommit(true); }
            }
        } catch (Exception e) { source.close(); throw e; }
        return source;
    }
}
