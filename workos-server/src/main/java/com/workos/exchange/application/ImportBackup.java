package com.workos.exchange.application;

/** 导入前保护工作区的端口，Web 环境由部署者负责数据库备份。 */
public interface ImportBackup {
  void beforeImport() throws Exception;
}
