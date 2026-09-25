package com.workos.worklog.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class WorkLogCreateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 创建工作记录请求（"记一句"）
 */
@Data
public class WorkLogCreateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "所属事项")
    private Long itemId;

    @Schema(title = "所属项目")
    private Long projectId;

    @Schema(title = "归属日期")
    private LocalDate workDate;

    @NotBlank(message = "记录内容不能为空")
    @Size(max = 2000, message = "记录内容长度不能超过2000")
    @Schema(title = "记录内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(title = "发生时间")
    private Instant occurredAt;
}
