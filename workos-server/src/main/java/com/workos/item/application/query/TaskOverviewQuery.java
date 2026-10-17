package com.workos.item.application.query;

import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.domain.ItemStatus;
import java.util.List;

/** 浮窗任务摘要查询端口；完整计数与有限条目在一个只读事务中获取。 */
public interface TaskOverviewQuery {
  /** 三类状态的完整数量，以及当前选中状态最多三十条展示数据。 */
  public record Overview(
      long todo, long doing, long waiting, ItemStatus selected, List<ItemResp> tasks) {}

  /** 仅接受 TODO、DOING、WAITING；详情复用事项查询的业务标记。 */
  Overview overview(ItemStatus selected);
}
