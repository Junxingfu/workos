package com.workos.project.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 项目事项分布 */
@Data
public class ProjectStatisticsResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long todo;

  private Long doing;

  private Long waiting;

  private Long done;
}
