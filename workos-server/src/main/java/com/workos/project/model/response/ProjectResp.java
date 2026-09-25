package com.workos.project.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ProjectResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目响应对象
 */
@Data
public class ProjectResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String status;

    private String color;

    @Schema(title = "进行中数量")
    private Long doingCount;

    @Schema(title = "等待数量")
    private Long waitingCount;

    @Schema(title = "未完成数量")
    private Long openCount;

    @Schema(title = "今天计划数量")
    private Long todayCount;

    @Schema(title = "当前重点（列表直接可见，不用点进详情）")
    private String currentFocus;

    private Instant lastActivityAt;

    @Schema(title = "事项分布（详情返回）")
    private ProjectStatisticsResp statistics;

    @Schema(title = "项目现状（详情返回）")
    private ProjectContextResp context;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;
}
