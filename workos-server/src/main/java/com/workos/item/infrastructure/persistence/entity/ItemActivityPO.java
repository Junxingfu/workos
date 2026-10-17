package com.workos.item.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.workos.item.domain.ActivityType;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** item_activity 表的系统审计投影；由领域事件生成，不作为日报人工工作内容。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("item_activity")
public class ItemActivityPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 关联事项 ID；仅保存身份引用，不嵌入事项对象。 */
  private Long itemId;

  /** 事件类型 */
  private ActivityType activityType;

  /** 变更前状态 */
  private String fromStatus;

  /** 变更后状态 */
  private String toStatus;

  /** 变更字段 */
  private String fieldName;

  /** 旧值 */
  private String oldValue;

  /** 新值 */
  private String newValue;

  /** 备注 */
  private String remark;

  /** 操作者：USER / SYSTEM */
  private String actor;

  /** 发生时间 */
  private Instant occurredAt;
}
