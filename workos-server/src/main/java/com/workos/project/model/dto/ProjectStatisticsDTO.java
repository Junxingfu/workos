package com.workos.project.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectStatisticsDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目事项分布
 */
@Data
public class ProjectStatisticsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long todo;

    private Long doing;

    private Long waiting;

    private Long done;
}
