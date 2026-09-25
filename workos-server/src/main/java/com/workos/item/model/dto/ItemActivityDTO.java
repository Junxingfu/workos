package com.workos.item.model.dto;

import com.workos.item.enums.ActivityType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ItemActivityDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 系统事件领域对象
 */
@Data
public class ItemActivityDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long itemId;

    private ActivityType activityType;

    private String fromStatus;

    private String toStatus;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String remark;

    private String actor;

    private Instant occurredAt;
}
