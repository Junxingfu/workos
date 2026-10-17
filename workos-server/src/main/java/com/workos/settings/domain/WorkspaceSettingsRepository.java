package com.workos.settings.domain;

/** WorkspaceSettings 聚合仓储；领域声明存取语义，基础设施实现版本条件写入。 */
public interface WorkspaceSettingsRepository {
  /** 按工作区单例 ID，固定为 1读取聚合；不存在时返回 null，由用例决定创建或报错。 */
  WorkspaceSettings find(Long key);

  /** 插入或按旧版本更新整个聚合；成功后回填身份和新版本。 */
  void save(WorkspaceSettings aggregate);
}
