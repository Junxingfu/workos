package com.workos.daily.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class DailyResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报响应对象
 */
@Data
public class DailyResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDate date;

    private String content;

    @Schema(title = "是否已生成过内容")
    private Boolean generated;

    private String generatedContent;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;
}
