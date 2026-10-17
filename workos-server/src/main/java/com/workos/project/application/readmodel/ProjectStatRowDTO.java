package com.workos.project.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 项目统计行：按状态聚合的计数与最近活动时间 */
@Data
public class ProjectStatRowDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long projectId;

  private String status;

  private Long cnt;

  private Instant lastAt;
}
