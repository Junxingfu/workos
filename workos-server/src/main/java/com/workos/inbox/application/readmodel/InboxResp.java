package com.workos.inbox.application.readmodel;

import com.workos.item.application.readmodel.ItemResp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/** 待整理队列响应 */
@Data
public class InboxResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private List<ItemResp> records;

  private Long total;

  @Schema(title = "最老一条已压了多少天")
  private Long oldestDays;
}
