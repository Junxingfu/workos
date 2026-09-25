package com.workos.item.state;

import com.workos.item.enums.ItemStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @Class ItemStateMachine
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项状态机（唯一真源）
 */
@Component
public class ItemStateMachine {

    /**
     * 合法转换表：
     * INBOX -> TODO / CANCELLED
     * TODO / DOING / WAITING -> 见下
     * DONE / CANCELLED -> TODO（撤销与恢复）
     */
    private static final Map<ItemStatus, Set<ItemStatus>> TRANSITIONS = Map.of(
            ItemStatus.INBOX, Set.of(ItemStatus.TODO, ItemStatus.CANCELLED),
            ItemStatus.TODO, Set.of(ItemStatus.DOING, ItemStatus.WAITING,
                    ItemStatus.DONE, ItemStatus.CANCELLED),
            ItemStatus.DOING, Set.of(ItemStatus.TODO, ItemStatus.WAITING,
                    ItemStatus.DONE, ItemStatus.CANCELLED),
            ItemStatus.WAITING, Set.of(ItemStatus.TODO, ItemStatus.DOING,
                    ItemStatus.DONE, ItemStatus.CANCELLED),
            ItemStatus.DONE, Set.of(ItemStatus.TODO),
            ItemStatus.CANCELLED, Set.of(ItemStatus.TODO)
    );

    /**
     * @Method canTransition
     * @Param [from, to]
     * @Return boolean
     * @Desc 是否允许状态转换
     */
    public boolean canTransition(ItemStatus from, ItemStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * @Method allowedTargets
     * @Param [from]
     * @Return Set<ItemStatus>
     * @Desc 当前状态允许到达的目标状态
     */
    public Set<ItemStatus> allowedTargets(ItemStatus from) {
        return TRANSITIONS.getOrDefault(from, Set.of());
    }
}
