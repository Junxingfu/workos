package com.workos.project.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ProjectContextDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目现状领域对象
 */
@Data
public class ProjectContextDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String goal;

    private String keyPeople;

    private String keyLinks;

    /**
     * 当前焦点，对应实体字段 focus
     */
    private String currentFocus;

    private String blocker;

    private String nextAction;

    private String progressNote;

    private String notesMarkdown;

    private Instant updatedAt;

    private Integer version;

    /**
     * 距离上次更新已过去的天数
     */
    private Long staleDays;
}
