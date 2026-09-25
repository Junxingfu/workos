package com.workos.project.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ProjectDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目领域对象
 */
@Data
public class ProjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String code;

    private String description;

    private String status;

    private String color;

    private Integer sortOrder;

    private Instant archivedAt;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;

    /**
     * 进行中数量
     */
    private Long doingCount;

    /**
     * 等待数量
     */
    private Long waitingCount;

    /**
     * 未完成数量
     */
    private Long openCount;

    /**
     * 今天计划数量
     */
    private Long todayCount;

    /**
     * 当前重点
     */
    private String currentFocus;

    /**
     * 最近活动时间
     */
    private Instant lastActivityAt;

    private ProjectStatisticsDTO statistics;

    private ProjectContextDTO context;
}
