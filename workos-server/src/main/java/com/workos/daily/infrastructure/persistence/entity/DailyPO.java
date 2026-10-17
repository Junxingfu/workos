package com.workos.daily.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** daily 表的存储映射；正文与生成快照分离，覆盖规则由 DailyReport 负责。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("daily")
public class DailyPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 工作区业务日期；每个日期只有一份日报。 */
  private LocalDate dailyDate;

  /** 用户表达（事实真源是 Item / WorkLog） */
  private String content;

  /** 依据人工工作记录和完成事项生成的快照；生成本身不覆盖用户正文。 */
  private String generatedContent;

  /** 日报保存状态：编辑为 DRAFT，确认应用为 CONFIRMED；不代表事项执行状态。 */
  private String status;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  @Version private Integer version;
}
