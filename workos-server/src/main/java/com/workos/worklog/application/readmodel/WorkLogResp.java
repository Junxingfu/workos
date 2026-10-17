package com.workos.worklog.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

/** 工作记录响应对象 */
@Data
public class WorkLogResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private Long itemId;

  private Long projectId;

  private String content;

  private LocalDate workDate;

  private Instant occurredAt;

  private Instant createdAt;
}
