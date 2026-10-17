package com.workos.item.interfaces.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 调整今日计划顺序 */
@Data
public class ItemPlanReorderReq implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 计划日期 */
  @NotNull(message = "计划日期不能为空")
  @Schema(title = "计划日期", requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate date;

  /** 事项主键顺序 */
  @Schema(title = "事项顺序")
  private List<Long> itemIds;
}
