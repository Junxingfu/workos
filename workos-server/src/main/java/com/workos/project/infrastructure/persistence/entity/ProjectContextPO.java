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

/** project_context 表的存储映射；对应独立版本的 ProjectBrief 聚合。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("project_context")
public class ProjectContextPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 现状所属项目 ID，唯一；现状与项目基本信息使用独立版本。 */
  private Long projectId;

  /** 目标（稳定） */
  private String goal;

  /** 关键人（稳定） */
  private String keyPeople;

  /** 关键链接（稳定） */
  private String keyLinks;

  /** 当前焦点（动态） */
  private String focus;

  /** 当前阻塞 */
  private String blocker;

  /** 下一步 */
  private String nextAction;

  /** 进展备注 */
  private String progressNote;

  /** 现状补充笔记，使用 Markdown，可为空。 */
  private String notesMarkdown;

  /** 最近一次保存的时间点。 */
  @TableField(fill = FieldFill.INSERT)
  private Instant updatedAt;

  /** 现状自身的乐观锁版本，避免与项目基本信息编辑相互冲突。 */
  @Version private Integer version;
}
