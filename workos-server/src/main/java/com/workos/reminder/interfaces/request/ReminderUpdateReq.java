package com.workos.reminder.interfaces.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 稍后提醒：只改 remindAt，业务事实不变 */
@Data
public class ReminderUpdateReq implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @NotNull(message = "提醒时间不能为空")
  @Schema(title = "新的提醒时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private Instant remindAt;
}
