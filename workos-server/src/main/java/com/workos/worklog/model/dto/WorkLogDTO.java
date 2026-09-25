package com.workos.worklog.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class WorkLogDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录领域对象
 */
@Data
public class WorkLogDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private Long itemId;

    private LocalDate workDate;

    private Instant occurredAt;

    private String content;

    private Instant createdAt;
}
