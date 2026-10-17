package com.workos.today.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Data;

/** Today 聚合响应：后端只负责分区归属 */
@Data
public class TodayResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "业务日期")
  private LocalDate date;

  private TodaySummaryResp summary;

  private TodaySectionsResp sections;
}
