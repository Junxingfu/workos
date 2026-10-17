package com.workos.followup.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 按人聚合的等待与跟进 */
@Data
public class PersonFollowUpResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "对方")
  private String person;

  @Schema(title = "我在等他的数量")
  private Integer waitingOn;

  @Schema(title = "他在等我的数量；MVP 无此数据模型，恒为 0")
  private Integer waitingMe;
}
