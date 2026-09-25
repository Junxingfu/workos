package com.workos.project.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectStatisticsResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目事项分布
 */
@Data
public class ProjectStatisticsResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long todo;

    private Long doing;

    private Long waiting;

    private Long done;
}
