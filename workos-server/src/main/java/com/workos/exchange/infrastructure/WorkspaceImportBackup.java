package com.workos.exchange.infrastructure;

import com.workos.desktop.DesktopStorage;
import com.workos.exchange.application.ImportBackup;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 桌面导入前保存独立快照；Web 不装配桌面存储。 */
@Component
@RequiredArgsConstructor
public class WorkspaceImportBackup implements ImportBackup {
  private final ObjectProvider<DesktopStorage> storage;

  public void beforeImport() throws Exception {
    if (storage.getIfAvailable() != null) {
      storage.getObject().backup("before-excel");
    }
  }
}
