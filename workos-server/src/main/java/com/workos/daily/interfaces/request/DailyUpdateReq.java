package com.workos.daily.interfaces.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 更新日报请求 */
@Data
public class DailyUpdateReq implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "版本号")
  private Integer version;

  @Schema(title = "日报内容")
  private String content;
}
