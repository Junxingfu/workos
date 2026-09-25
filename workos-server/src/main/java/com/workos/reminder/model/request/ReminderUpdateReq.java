package com.workos.reminder.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ReminderUpdateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 稍后提醒：只改 remindAt，业务事实不变
 */
@Data
public class ReminderUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "提醒时间不能为空")
    @Schema(title = "新的提醒时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant remindAt;
}
