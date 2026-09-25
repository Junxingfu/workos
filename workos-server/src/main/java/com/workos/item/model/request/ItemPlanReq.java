package com.workos.item.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Class ItemPlanReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 加入今日计划 / 排到某天
 */
@Data
public class ItemPlanReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 乐观锁版本
     */
    @Schema(title = "版本号")
    private Integer version;

    /**
     * 计划日期
     */
    @NotNull(message = "计划日期不能为空")
    @Schema(title = "计划日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate plannedDate;
}
