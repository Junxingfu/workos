package com.workos.item.application.query;

import com.workos.common.model.BasePageReq;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 全部事项分页查询请求；status 为空时按"未完成"处理 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ItemCriteria extends BasePageReq {

  @Serial private static final long serialVersionUID = 1L;

  /** 所属项目 */
  @Schema(title = "所属项目")
  private Long projectId;

  /** 类型 */
  @Schema(title = "类型")
  private ItemType type;

  /** 状态 */
  @Schema(title = "状态")
  private ItemStatus status;

  /** 优先级 */
  @Schema(title = "优先级")
  private ItemPriority priority;

  /** 截止时间起 */
  @Schema(title = "截止时间起")
  private LocalDate from;

  /** 截止时间止 */
  @Schema(title = "截止时间止")
  private LocalDate to;

  /** 只看无日期的 */
  @Schema(title = "只看无日期")
  private Boolean noDate;

  /** 只看未分类（没有项目） */
  @Schema(title = "只看未分类")
  private Boolean unclassified;

  /** 只看有跟进时间的 */
  @Schema(title = "只看跟进中")
  private Boolean followUpOnly;

  /** 关键词 */
  @Schema(title = "关键词")
  private String keyword;
}
