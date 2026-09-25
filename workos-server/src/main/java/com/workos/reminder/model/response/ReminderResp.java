package com.workos.reminder.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ReminderResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒响应对象
 */
@Data
public class ReminderResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String kind;

    private Long itemId;

    private String title;

    private Instant remindAt;

    private String status;

    private Instant acknowledgedAt;
}
