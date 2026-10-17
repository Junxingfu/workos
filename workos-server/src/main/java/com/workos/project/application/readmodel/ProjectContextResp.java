package com.workos.project.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 项目现状响应对象 */
@Data
public class ProjectContextResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Integer version;

  private String goal;

  private String currentFocus;

  private String progressNote;

  private String blocker;

  private String nextAction;

  private String keyPeople;

  private String keyLinks;

  private String notesMarkdown;

  private Instant updatedAt;

  @Schema(title = "距上次更新天数")
  private Long staleDays;
}
