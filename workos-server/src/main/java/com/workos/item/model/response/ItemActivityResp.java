package com.workos.item.model.response;

import com.workos.item.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ItemActivityResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 系统事件响应对象
 */
@Data
public class ItemActivityResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long itemId;

    @Schema(title = "事件类型")
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
