package com.workos;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 源码边界守卫：防止后来把存储、HTTP 或框架重新塞回领域。 */
class DomainBoundaryTest {
  @Test
  void domainHasNoFrameworkOrOuterLayerDependencies() throws Exception {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/workos"))) {
      for (Path file :
          files
              .filter(p -> p.toString().contains("/domain/") && p.toString().endsWith(".java"))
              .toList()) {
        String source = Files.readString(file);
        assertFalse(
            source.matches(
                "(?s).*(org\\.springframework|com\\.baomidou|java\\.sql|jakarta\\.|lombok\\.|com\\.fasterxml|com\\.workos\\.\\w+\\.(infrastructure|interfaces|application))\\..*"),
            file.toString());
      }
    }
  }

  @Test
  void applicationDoesNotDependOnInfrastructureOrHttp() throws Exception {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/workos"))) {
      for (Path file :
          files
              .filter(p -> p.toString().contains("/application/") && p.toString().endsWith(".java"))
              .toList()) {
        String source = Files.readString(file);
        assertFalse(
            source.contains(".infrastructure.")
                || source.contains(".interfaces.")
                || source.contains("com.baomidou")
                || source.contains("java.sql"),
            file.toString());
      }
    }
  }

  @Test
  void httpDependsOnApplicationPortsInsteadOfStorageImplementations() throws Exception {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/workos"))) {
      for (Path file :
          files
              .filter(
                  path ->
                      path.toString().contains("/interfaces/") && path.toString().endsWith(".java"))
              .toList()) {
        String source = Files.readString(file);
        assertFalse(
            source.contains(".infrastructure.")
                || source.contains("com.baomidou")
                || source.contains("java.sql"),
            file.toString());
      }
    }
  }
}
