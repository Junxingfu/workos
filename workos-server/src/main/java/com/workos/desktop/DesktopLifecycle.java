package com.workos.desktop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

@Component @Profile("desktop") @EnableScheduling
public class DesktopLifecycle {
    private final Path directory;
    private final DesktopStorage storage;
    public DesktopLifecycle(@Value("${workos.desktop.directory}") String directory, DesktopStorage storage) {
        this.directory = Path.of(directory); this.storage = storage;
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
    public void backup() throws Exception { storage.dailyBackup(); }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void parentWatch() {
        String parent = System.getenv("WORKOS_PARENT_PID");
        if (parent != null && ProcessHandle.of(Long.parseLong(parent)).map(p -> !p.isAlive()).orElse(true)) System.exit(0);
    }
}
