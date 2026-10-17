package com.workos.daily.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Data;

/** 生成结果（预览，不覆盖 content） */
@Data
public class DailyGenerateResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private LocalDate date;

  @Schema(title = "生成内容快照")
  private String generatedContent;

  @Schema(title = "来源记录数")
  private Integer sourceWorkLogCount;

  private Integer version;
}
