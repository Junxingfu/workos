package com.workos.item.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.item.application.query.TaskOverviewQuery;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 浮窗专用读模型；数量来自完整查询，不是 Today 候选条数，也不加载聚合执行写行为。 */
@Service
@RequiredArgsConstructor
public class MyBatisTaskOverviewQuery implements TaskOverviewQuery {
  private final ItemMapper items;

  private final ItemQueries queries;

  @Transactional(readOnly = true)
  public Overview overview(ItemStatus selected) {
    if (!Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.WAITING).contains(selected)) {
      throw new BizException(ResponseCode.PARAM_ERROR, "请选择待办、进行中或等待中");
    }
    Page<ItemPO> page =
        items.selectPage(
            new Page<ItemPO>(1, 30, false),
            filter(selected)
                .orderByAsc(ItemPO::getPriority)
                .orderByDesc(ItemPO::getUpdatedAt)
                .orderByDesc(ItemPO::getId));
    return new Overview(
        count(ItemStatus.TODO),
        count(ItemStatus.DOING),
        count(ItemStatus.WAITING),
        selected,
        queries.assembles(page.getRecords()));
  }

  private long count(ItemStatus status) {
    return items.selectCount(filter(status));
  }

  private LambdaQueryWrapper<ItemPO> filter(ItemStatus status) {
    return new LambdaQueryWrapper<ItemPO>()
        .eq(ItemPO::getItemType, ItemType.TASK)
        .eq(ItemPO::getStatus, status);
  }
}
