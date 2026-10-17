package com.workos.followup.application.readmodel;

import com.workos.item.application.readmodel.ItemResp;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/** 等待与跟进工作台 */
@Data
public class FollowUpsResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 今天到点 */
  private List<ItemResp> today;

  /** 已过期且未处理 */
  private List<ItemResp> overdue;

  /** 未来 7 天内 */
  private List<ItemResp> thisWeek;

  /** 正在等待且未到跟进时间 */
  private List<ItemResp> waiting;

  /** 最近完成 */
  private List<ItemResp> recentlyDone;

  /** 按人聚合 */
  private List<PersonFollowUpResp> byPerson;
}
