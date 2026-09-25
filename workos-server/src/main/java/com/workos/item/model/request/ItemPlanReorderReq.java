package com.workos.item.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @Class ItemPlanReorderReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 调整今日计划顺序
 */
@Data
public class ItemPlanReorderReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 计划日期
     */
    @NotNull(message = "计划日期不能为空")
    @Schema(title = "计划日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    /**
     * 事项主键顺序
     */
    @Schema(title = "事项顺序")
    private List<Long> itemIds;
}
