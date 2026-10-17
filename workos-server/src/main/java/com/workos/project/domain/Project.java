package com.workos.project.domain;

import com.workos.shared.domain.ConcurrentChange;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** 项目聚合：管理身份、命名和归档，不包含任务集合。 */
public final class Project {
  /** 持久化快照；外部只能读取，修改必须调用业务行为。 */
  public record State(
      Long id,
      String name,
      String code,
      String description,
      String status,
      String color,
      Integer sortOrder,
      Instant archivedAt,
      Instant createdAt,
      Instant updatedAt,
      Integer version) {}

  /** 聚合身份；创建时未分配，首次保存后确定。 */
  private Long id;

  /** 项目名称 */
  private String name;

  /** 项目编码 */
  private String code;

  /** 项目说明，可为空，不等同于 project_context 中的动态现状。 */
  private String description;

  /** ACTIVE / ARCHIVED */
  private String status;

  /** 项目展示颜色；不参与业务规则。 */
  private String color;

  /** 项目列表排序值，较小的排在前面。 */
  private Integer sortOrder;

  /** 归档时间点；活动项目为空，恢复项目时清空。 */
  private Instant archivedAt;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  private Integer version;

  /** 从仓储快照重建已有聚合；新增业务对象应使用命名工厂方法。 */
  public Project(State state) {
    id = state.id();
    name = state.name();
    code = state.code();
    description = state.description();
    status = state.status();
    color = state.color();
    sortOrder = state.sortOrder();
    archivedAt = state.archivedAt();
    createdAt = state.createdAt();
    updatedAt = state.updatedAt();
    version = state.version();
  }

  /** 返回不可变状态快照，供仓储保存或应用读取；不开放字段 setter。 */
  public State snapshot() {
    return new State(
        id,
        name,
        code,
        description,
        status,
        color,
        sortOrder,
        archivedAt,
        createdAt,
        updatedAt,
        version);
  }

  /** 仓储保存成功后回填身份与版本；业务调用方不应自行修改版本。 */
  public void persisted(Long identity, int savedVersion) {
    id = identity;
    version = savedVersion;
  }

  /** 校验调用方看到的版本；不一致时拒绝覆盖较新的修改。 */
  public void expect(Integer expected) {
    if (expected != null && !Objects.equals(expected, version)) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
  }

  public record Edit(
      String name, String description, String status, String color, Integer sortOrder) {}

  /** 创建活动项目，先验证名称；名称跨项目唯一性由应用协调仓储检查。 */
  public static Project create(
      String name, String code, String description, String color, Integer sortOrder, Instant now) {
    validateName(name);
    return new Project(
        new State(
            null,
            name.trim(),
            code,
            description,
            "ACTIVE",
            color,
            sortOrder == null ? 0 : sortOrder,
            null,
            now,
            now,
            0));
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank() || name.trim().length() > 200) {
      throw new IllegalArgumentException("项目名称不能为空且不超过 200 字");
    }
  }

  /** 先验证候选字段再修改；null 表示未提供，归档/恢复复用对应行为。 */
  public void revise(Edit edit, Instant now) {
    if (edit.name() != null) {
      validateName(edit.name());
    }
    if (edit.status() != null && !Set.of("ACTIVE", "ARCHIVED").contains(edit.status())) {
      throw new IllegalArgumentException("项目状态无效");
    }
    if (edit.name() != null) {
      name = edit.name().trim();
    }
    if (edit.description() != null) {
      description = edit.description();
    }
    if (edit.color() != null) {
      color = edit.color();
    }
    if (edit.sortOrder() != null) {
      sortOrder = edit.sortOrder();
    }
    if (edit.status() != null) {
      if (edit.status().equals("ARCHIVED")) {
        archive(now);
      } else {
        restore(now);
      }
    }
    updatedAt = now;
  }

  /** 归档项目并记录时间；事项只引用项目身份，不级联取消事项。 */
  public void archive(Instant now) {
    status = "ARCHIVED";
    archivedAt = now;
    updatedAt = now;
  }

  /** 恢复活动状态并清除归档时间，保留原项目身份。 */
  public void restore(Instant now) {
    status = "ACTIVE";
    archivedAt = null;
    updatedAt = now;
  }
}
