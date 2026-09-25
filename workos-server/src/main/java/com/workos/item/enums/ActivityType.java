package com.workos.item.enums;

/**
 * @Class ActivityType
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 系统事件类型（不进日报"工作内容"）
 */
public enum ActivityType {

    /**
     * 创建事项
     */
    CREATED,

    /**
     * 状态变化
     */
    STATUS_CHANGED,

    /**
     * 字段变化
     */
    FIELD_CHANGED,

    /**
     * 计划调整
     */
    PLAN_CHANGED,

    /**
     * 退出等待；remark 记录"已等待 N 天"
     */
    WAITING_ENDED
}
