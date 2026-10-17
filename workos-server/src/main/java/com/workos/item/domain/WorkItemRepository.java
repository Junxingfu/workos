package com.workos.item.domain;

import java.time.LocalDate;
import java.util.List;

/** 领域提出存取需求，基础设施负责乐观锁和事件一起持久化。 */
public interface WorkItemRepository {
  /** 按身份重建完整事项；不存在时报告业务错误。 */
  WorkItem get(long id);

  /** 原子保存事项、项目关联和审计事件；以版本条件拒绝并发覆盖。 */
  void save(WorkItem item);

  /** 删除符合聚合约束的未整理事项；版本冲突时不删除。 */
  void remove(WorkItem item);

  /** 加载指定未整理事项；ids 为空时读取全部未整理事项。 */
  List<WorkItem> inbox(List<Long> ids);

  /** 验证主项目和关联项目确实存在；不把项目对象装入事项聚合。 */
  void requireProjects(ProjectReferences projects);

  /** 读取指定计划日的下一个顺序值，供本次安排使用。 */
  int nextPlanRank(LocalDate date);
}
