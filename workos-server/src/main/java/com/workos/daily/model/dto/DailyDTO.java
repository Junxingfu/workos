package com.workos.daily.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class DailyDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报领域对象
 */
@Data
public class DailyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDate date;

    private String content;

    private String generatedContent;

    private String status;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;
}
