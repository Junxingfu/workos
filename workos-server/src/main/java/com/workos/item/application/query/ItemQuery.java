package com.workos.item.application.query;

import com.workos.common.model.BasePageResult;
import com.workos.item.application.readmodel.ItemActivityResp;
import com.workos.item.application.readmodel.ItemResp;
import java.util.List;

/** Item 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface ItemQuery {
  /** 读取一件事项及其活动时间线。 */
  ItemResp detail(Long id);

  /** 按条件分页读取事项，包含主项目或关联项目匹配。 */
  BasePageResult<ItemResp> page(ItemCriteria criteria);

  /** 读取已发生的系统事件。 */
  List<ItemActivityResp> activities(Long id);
}
