package com.workos.project.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** project 表的存储映射；对应 Project 聚合，不持有事项集合。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("project")
public class ProjectPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
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
  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  @Version private Integer version;
}
