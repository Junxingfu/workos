package com.workos.worklog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** work_log 表的人工事实映射；项目归属冻结在记录发生时。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("work_log")
public class WorkLogPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 记录发生时的主项目 ID，可为空；事项改归属后不重写，日报按此归集。 */
  private Long projectId;

  /** 来源事项 ID，可为空；正文始终是人工记录，不混入系统活动。 */
  private Long itemId;

  /** 属于哪一天（受 day_boundary_hour 影响） */
  private LocalDate workDate;

  /** 事实发生的时间点 */
  private Instant occurredAt;

  /** 记录内容 */
  private String content;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;
}
