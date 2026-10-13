package com.workos.project.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ProjectContextResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目现状响应对象
 */
@Data
public class ProjectContextResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer version;

    private String goal;

    private String currentFocus;

    private String progressNote;

    private String blocker;

    private String nextAction;

    private String keyPeople;

    private String keyLinks;

    private String notesMarkdown;

    private Instant updatedAt;

    @Schema(title = "距上次更新天数")
    private Long staleDays;
}
