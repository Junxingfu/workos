package com.workos.item.model.dto;

import com.workos.item.enums.ActivityType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class LastActivityDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 卡片回退展示时使用的最近一条系统事件
 */
@Data
public class LastActivityDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事项主键
     */
    private Long itemId;

    /**
     * 事件类型
     */
    private ActivityType activityType;

    /**
     * 发生时间
     */
    private Instant occurredAt;
}
