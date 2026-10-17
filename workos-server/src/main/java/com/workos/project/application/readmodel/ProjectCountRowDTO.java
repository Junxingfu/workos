package com.workos.project.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 项目维度的单值计数行 */
@Data
public class ProjectCountRowDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long projectId;

  private Long cnt;
}
