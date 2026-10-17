package com.workos.reminder.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 提醒响应对象 */
@Data
public class ReminderResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private String kind;

  private Long itemId;

  private String title;

  private Instant remindAt;

  private String status;

  private Instant acknowledgedAt;
}
