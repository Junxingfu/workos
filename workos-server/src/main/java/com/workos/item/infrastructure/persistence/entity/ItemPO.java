package com.workos.item.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemSource;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** item 表的存储映射；业务行为由 WorkItem 负责，PO 只承载列值。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("item")
public class ItemPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 数据库主键，由插入操作生成。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 主项目 ID；为空表示未分类。其他关联项目存于 item_project_link，共享本事项进度。 */
  private Long projectId;

  /** 用户可见标题。 */
  private String title;

  /** 补充说明，可为空。 */
  private String description;

  /** 类型：TASK / NOTE */
  private ItemType itemType;

  /** 事项当前状态；合法流转由 WorkItem 和 TransitionPolicy 校验。 */
  private ItemStatus status;

  /** 优先级 P0 / P1 / P2 / P3；数值越小越优先。 */
  private ItemPriority priority;

  /** 我计划哪天做 */
  private LocalDate plannedDate;

  /** 计划日期内的排序值；无计划时为空。 */
  private Integer planRank;

  /** 最晚什么时候完成 */
  private Instant dueAt;

  /** 我什么时候再次主动推动（非空即表示有跟进能力） */
  private Instant nextFollowUpAt;

  /** 在等谁 */
  private String waitingFor;

  /** 什么时候开始等 */
  private Instant waitingSince;

  /** 对方预计什么时候给 */
  private Instant waitingExpectedAt;

  /** 相关人（自由文本，只有一个字段） */
  private String relatedPerson;

  /** 最近完成的时间点；退出完成状态时按流转规则清空。 */
  private Instant completedAt;

  /** 最近取消的时间点；恢复时清空。 */
  private Instant cancelledAt;

  /** 来源 */
  private ItemSource source;

  /** 首次创建的时间点；API 使用 ISO Instant，数据库类型由部署模式适配。 */
  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;

  /** 最近一次保存的时间点。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  @Version private Integer version;
}
