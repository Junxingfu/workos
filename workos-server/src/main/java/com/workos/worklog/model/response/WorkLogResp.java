package com.workos.worklog.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class WorkLogResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录响应对象
 */
@Data
public class WorkLogResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long itemId;

    private Long projectId;

    private String content;

    private LocalDate workDate;

    private Instant occurredAt;

    private Instant createdAt;
}
