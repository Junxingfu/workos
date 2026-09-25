package com.workos.item.state;

import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import org.springframework.stereotype.Component;

/**
 * @Class ItemTypeCapability
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 类型能力矩阵：状态定义生命周期，类型定义可用能力
 */
@Component
public class ItemTypeCapability {

    /**
     * @Method allows
     * @Param [target, type]
     * @Return boolean
     * @Desc NOTE 只允许 INBOX / DONE / CANCELLED
     */
    public boolean allows(ItemStatus target, ItemType type) {
        if (type == ItemType.NOTE) {
            return target == ItemStatus.INBOX
                    || target == ItemStatus.DONE
                    || target == ItemStatus.CANCELLED;
        }
        return true;
    }

    /**
     * @Method joinToday
     * @Param [type]
     * @Return boolean
     * @Desc NOTE 不进 Today
     */
    public boolean joinToday(ItemType type) {
        return type != ItemType.NOTE;
    }

    /**
     * @Method allowReminder
     * @Param [type]
     * @Return boolean
     * @Desc NOTE 不挂 Reminder
     */
    public boolean allowReminder(ItemType type) {
        return type != ItemType.NOTE;
    }

    /**
     * @Method enterDaily
     * @Param [type]
     * @Return boolean
     * @Desc NOTE 不进日报
     */
    public boolean enterDaily(ItemType type) {
        return type != ItemType.NOTE;
    }
}
