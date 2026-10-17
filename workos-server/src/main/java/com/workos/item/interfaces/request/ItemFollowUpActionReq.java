package com.workos.item.interfaces.request;

import com.workos.item.domain.FollowUpOutcome;
import com.workos.item.domain.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 跟进动作请求：三选一，不是打开编辑页 */
@Data
public class ItemFollowUpActionReq implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 乐观锁版本 */
  @Schema(title = "版本号")
  private Integer version;

  /** 跟进结果 */
  @NotNull(message = "跟进结果不能为空")
  @Schema(title = "跟进结果", requiredMode = Schema.RequiredMode.REQUIRED)
  private FollowUpOutcome outcome;

  /** 备注；为空时由系统生成默认文案 */
  @Schema(title = "备注")
  private String note;

  /** 下次跟进时间（STILL_WAITING 时使用） */
  @Schema(title = "下次跟进时间")
  private Instant nextFollowUpAt;

  /** 目标状态（RESOLVED 时使用：DOING / DONE） */
  @Schema(title = "目标状态")
  private ItemStatus toStatus;
}
