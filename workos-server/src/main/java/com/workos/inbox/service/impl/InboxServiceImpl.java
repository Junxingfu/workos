package com.workos.inbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.support.TimeZoneHolder;
import com.workos.inbox.model.request.InboxAcceptAllReq;
import com.workos.inbox.model.response.InboxAcceptAllResp;
import com.workos.inbox.model.response.InboxResp;
import com.workos.inbox.service.InboxService;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.item.enums.ItemType;
import com.workos.item.model.request.ItemTransitionReq;
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
     * @Desc 先都收下：保留归属和优先级，通过同一状态入口收下实际待整理事项
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboxAcceptAllResp acceptAll(InboxAcceptAllReq req) {
        List<Long> itemIds = Objects.isNull(req) ? null : req.getItemIds();
        List<ItemPO> items = itemMapper.selectList(new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getStatus, ItemStatus.INBOX)
                .in(!CollectionUtils.isEmpty(itemIds), ItemPO::getId, itemIds));
        for (ItemPO item : items) {
            ItemTransitionReq transition = new ItemTransitionReq();
            transition.setVersion(item.getVersion());
            transition.setToStatus(item.getItemType() == ItemType.NOTE ? ItemStatus.DONE : ItemStatus.TODO);
            transition.setRemark("先都收下");
            itemService.transition(item.getId(), transition);
        }
        int accepted = items.size();
        InboxAcceptAllResp resp = new InboxAcceptAllResp();
        resp.setAccepted(accepted);
        return resp;
    }

}
