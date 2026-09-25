package com.workos.inbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.common.support.TimeZoneHolder;
import com.workos.inbox.model.request.InboxAcceptAllReq;
import com.workos.inbox.model.response.InboxAcceptAllResp;
import com.workos.inbox.model.response.InboxResp;
import com.workos.inbox.service.InboxService;
import com.workos.item.data.entity.ItemActivityPO;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.data.mapper.ItemActivityMapper;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.item.enums.ActivityType;
import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemStatus;
import com.workos.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @Class InboxServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 待整理业务实现：整理完成条件 = status != INBOX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxServiceImpl implements InboxService {

    private final ItemMapper itemMapper;

    private final ItemActivityMapper itemActivityMapper;

    private final ItemService itemService;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method inbox
     * @Return InboxResp
     * @Desc 待整理列表：最早进 Inbox 的排在前面
     */
    @Override
    public InboxResp inbox() {
        List<ItemPO> poList = itemMapper.selectList(new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getStatus, ItemStatus.INBOX)
                .orderByAsc(ItemPO::getCreatedAt));
        InboxResp resp = new InboxResp();
        resp.setRecords(itemService.assembles(poList));
        resp.setTotal((long) poList.size());
        resp.setOldestDays(itemMapper.countOldestInboxDays());
        return resp;
    }

    /**
     * @Method acceptAll
     * @Param [req]
     * @Return InboxAcceptAllResp
     * @Desc 先都收下：一次设为 TODO / P2 / 无项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboxAcceptAllResp acceptAll(InboxAcceptAllReq req) {
        List<Long> itemIds = Objects.isNull(req) ? null : req.getItemIds();
        int accepted;
        if (CollectionUtils.isEmpty(itemIds)) {
            accepted = itemMapper.acceptAllToTodo();
        } else {
            accepted = itemMapper.update(null, new LambdaUpdateWrapper<ItemPO>()
                    .set(ItemPO::getStatus, ItemStatus.TODO)
                    .set(ItemPO::getPriority, ItemPriority.P2)
                    .set(ItemPO::getProjectId, null)
                    .in(ItemPO::getId, itemIds)
                    .eq(ItemPO::getStatus, ItemStatus.INBOX));
        }
        if (accepted > 0) {
            recordAcceptActivity(CollectionUtils.isEmpty(itemIds) ? null : itemIds);
        }
        InboxAcceptAllResp resp = new InboxAcceptAllResp();
        resp.setAccepted(accepted);
        return resp;
    }

    private void recordAcceptActivity(List<Long> itemIds) {
        List<ItemPO> items = CollectionUtils.isEmpty(itemIds)
                ? itemMapper.selectList(new LambdaQueryWrapper<ItemPO>().eq(ItemPO::getStatus, ItemStatus.TODO))
                : itemMapper.selectBatchIds(itemIds);
        items.forEach(item -> itemActivityMapper.insert(ItemActivityPO.builder()
                .itemId(item.getId())
                .activityType(ActivityType.STATUS_CHANGED)
                .fromStatus(ItemStatus.INBOX.name())
                .toStatus(ItemStatus.TODO.name())
                .remark("先都收下")
                .actor("USER")
                .occurredAt(timeZoneHolder.now())
                .build()));
    }
}
