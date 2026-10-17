package com.workos.inbox.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.inbox.application.query.InboxQuery;
import com.workos.inbox.application.readmodel.InboxResp;
import com.workos.item.domain.ItemStatus;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.query.ItemQueries;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Inbox 专用查询投影；只读 SQL 与视图装配独立于聚合写入。 */
@Service
@RequiredArgsConstructor
public class InboxQueries implements InboxQuery {

  private final ItemMapper itemMapper;

  private final ItemQueries itemService;

  public InboxResp inbox() {
    List<ItemPO> poList =
        itemMapper.selectList(
            new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getStatus, ItemStatus.INBOX)
                .orderByAsc(ItemPO::getCreatedAt));
    InboxResp resp = new InboxResp();
    resp.setRecords(itemService.assembles(poList));
    resp.setTotal((long) poList.size());
    resp.setOldestDays(itemMapper.countOldestInboxDays());
    return resp;
  }
}
