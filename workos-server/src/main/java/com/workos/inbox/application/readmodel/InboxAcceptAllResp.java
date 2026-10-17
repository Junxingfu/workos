package com.workos.inbox.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 批量收下结果 */
@Data
public class InboxAcceptAllResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "已收下数量")
  private Integer accepted;
}
