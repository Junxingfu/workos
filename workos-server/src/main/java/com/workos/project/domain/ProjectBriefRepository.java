package com.workos.project.domain;

/** ProjectBrief 聚合仓储；领域声明存取语义，基础设施实现版本条件写入。 */
public interface ProjectBriefRepository {
  /** 按所属项目 ID（不是现状记录 ID）读取聚合；不存在时返回 null，由用例决定创建或报错。 */
  ProjectBrief find(Long key);

  /** 插入或按旧版本更新整个聚合；成功后回填身份和新版本。 */
  void save(ProjectBrief aggregate);
}
