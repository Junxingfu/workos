package com.workos.project.domain;

/** Project 聚合仓储；领域声明存取语义，基础设施实现版本条件写入。 */
public interface ProjectRepository {
  /** 按项目 ID读取聚合；不存在时返回 null，由用例决定创建或报错。 */
  Project find(Long key);

  /** 插入或按旧版本更新整个聚合；成功后回填身份和新版本。 */
  void save(Project aggregate);

  /** 检查名称是否被其他项目占用；更新时排除自身 ID。 */
  boolean nameUsed(String name, Long exceptId);
}
