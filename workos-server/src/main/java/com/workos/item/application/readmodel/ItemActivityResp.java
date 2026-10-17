package com.workos.item.application.readmodel;

import com.workos.item.domain.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 系统事件响应对象 */
@Data
public class ItemActivityResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private Long itemId;

  @Schema(title = "事件类型")
  private ActivityType activityType;

  private String fromStatus;

  private String toStatus;

  private String fieldName;

  private String oldValue;

  private String newValue;

  private String remark;

  private String actor;

  private Instant occurredAt;
}
