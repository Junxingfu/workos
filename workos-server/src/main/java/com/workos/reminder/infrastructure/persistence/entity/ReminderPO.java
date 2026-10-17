package com.workos.reminder.infrastructure.persistence.entity;

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

/** reminder 表的存储映射；对应手动提醒聚合，与事项进度独立。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("reminder")
public class ReminderPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  @Version private Integer version;

  /** ITEM / DAILY / STANDALONE */
  private String kind;

  /** 关联事项 ID；独立提醒可为空，提醒的处理不改变事项状态。 */
  private Long itemId;

  /** 用户可见标题。 */
  private String title;

  /** 下次展示该提醒的时间点，查询待处理提醒时与当前时间比较。 */
  private Instant remindAt;

  /** PENDING / DISMISSED / CANCELLED */
  private String status;

  /** MVP 不写入，字段保留 */
  private Instant triggeredAt;

  /** 用户处理提醒的时间点；未处理时为空。 */
  private Instant acknowledgedAt;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;
}
