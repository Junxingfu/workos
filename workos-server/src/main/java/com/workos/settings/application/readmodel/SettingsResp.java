package com.workos.settings.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 设置响应对象 */
@Data
public class SettingsResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "时区")
  private String timezone;

  private String dateFormat;

  private String weekStart;

  private Integer dayBoundaryHour;

  private String morningBriefTime;

  private String eodReviewTime;

  private Boolean desktopNotification;

  private String quickCaptureShortcut;
}
