package com.workos.project.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ProjectStatRowDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目统计行：按状态聚合的计数与最近活动时间
 */
@Data
public class ProjectStatRowDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;

    private String status;

    private Long cnt;

    private Instant lastAt;
}
