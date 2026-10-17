package com.workos.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 桌面内置后端的生命周期适配：发布就绪信息并跟随原生父进程退出。 */
@Component
@Profile("desktop")
@EnableScheduling
public class DesktopLifecycle {
  private final Path directory;
  private final DesktopStorage storage;
  private final AtomicBoolean stopping = new AtomicBoolean();

  public DesktopLifecycle(
      @Value("${workos.desktop.directory}") String directory, DesktopStorage storage) {
    this.directory = Path.of(directory);
    this.storage = storage;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ready(ApplicationReadyEvent event) throws Exception {
    if (event.getApplicationContext() instanceof ServletWebServerApplicationContext context) {
      storage.dailyBackup();
      Path ready = directory.resolve("ready.json");
      Path temp = directory.resolve("ready.tmp");
      Files.writeString(temp, "{\"port\":" + context.getWebServer().getPort() + "}");
      Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
      Files.move(temp, ready, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
  public void backup() throws Exception {
    storage.dailyBackup();
  }

  @Scheduled(fixedDelay = 5000, initialDelay = 5000)
  public void parentWatch() {
    String parent = System.getenv("WORKOS_PARENT_PID");
    if (parent != null
        && ProcessHandle.of(Long.parseLong(parent)).map(p -> !p.isAlive()).orElse(true)
        && stopping.compareAndSet(false, true)) {
      // Spring waits for scheduled tasks during shutdown; exit outside that executor.
      new Thread(() -> System.exit(0), "desktop-parent-exit").start();
    }
  }
}
