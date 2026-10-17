package com.workos.today.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** Today 六分区去重计数（数字可直接相加） */
@Data
public class TodaySummaryResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "① 今日计划")
  private Integer plan;

  @Schema(title = "② 逾期")
  private Integer overdue;

  @Schema(title = "③ 今日到期")
  private Integer today;

  @Schema(title = "④ 待跟进")
  private Integer followUp;

  @Schema(title = "⑤ 等待中")
  private Integer waiting;

  @Schema(title = "⑥ 待安排")
  private Integer suggested;

  @Schema(title = "待整理")
  private Integer inbox;

  @Schema(title = "未分类")
  private Integer unclassified;

  @Schema(title = "过去计划但未完成（欠账）")
  private Integer carriedOverPlan;
}
