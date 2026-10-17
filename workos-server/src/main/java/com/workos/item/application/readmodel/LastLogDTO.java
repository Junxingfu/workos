package com.workos.item.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 卡片上的"最近一条记录"：优先取工作记录 */
@Data
public class LastLogDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 事项主键 */
  private Long itemId;

  /** 记录内容 */
  private String content;

  /** 发生时间 */
  private Instant occurredAt;
}
