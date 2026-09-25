package com.workos.project.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectCountRowDTO
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 项目维度的单值计数行
 */
@Data
public class ProjectCountRowDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;

    private Long cnt;
}
