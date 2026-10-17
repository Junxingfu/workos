package com.workos.settings.application;

import com.workos.common.support.TimeZoneHolder;
import com.workos.settings.domain.WorkspaceSettings;
import com.workos.settings.domain.WorkspaceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 设置用例：只有事务提交后才切换进程内业务时区，回滚不能污染当前时间口径。 */
@Service
@RequiredArgsConstructor
public class SettingsApplicationService {
  private final WorkspaceSettingsRepository repository;
  private final TimeZoneHolder clock;

  /** 读取初始化后的工作区单例；缺失配置属于启动或迁移错误。 */
  public WorkspaceSettings.State get() {
    WorkspaceSettings settings = repository.find(1L);
    if (settings == null) {
      throw new IllegalStateException("工作区设置尚未迁移初始化");
    }
    return settings.snapshot();
  }

  /** 启动时用已保存配置初始化进程内业务时钟。 */
  public void initializeClock() {
    WorkspaceSettings.State state = get();
    clock.refresh(state.timezone(), state.dayBoundaryHour().intValue());
  }

  /** 校验并保存偏好；只在事务成功提交后刷新时钟，避免回滚污染内存口径。 */
  @Transactional
  public WorkspaceSettings.State update(WorkspaceSettings.Revision revision) {
    WorkspaceSettings settings = repository.find(1L);
    settings.revise(revision, clock.now());
    repository.save(settings);
    WorkspaceSettings.State saved = settings.snapshot();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clock.refresh(saved.timezone(), saved.dayBoundaryHour().intValue());
          }
        });
    return saved;
  }
}
