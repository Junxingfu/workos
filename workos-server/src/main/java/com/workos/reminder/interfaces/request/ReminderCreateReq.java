package com.workos.reminder.interfaces.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 创建提醒请求 */
@Data
public class ReminderCreateReq implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "类型：ITEM / DAILY / STANDALONE")
  private String kind;

  @Schema(title = "关联事项")
  private Long itemId;

  @NotBlank(message = "提醒标题不能为空")
  @Schema(title = "提醒标题", requiredMode = Schema.RequiredMode.REQUIRED)
  private String title;

  @NotNull(message = "提醒时间不能为空")
  @Schema(title = "提醒时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private Instant remindAt;
}
