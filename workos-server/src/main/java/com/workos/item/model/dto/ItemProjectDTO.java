package com.workos.item.model.dto;

import lombok.Data;

/** 关联项目的批量查询结果；主项目仍由 item.project_id 表示。 */
@Data
public class ItemProjectDTO {
    private Long itemId;
    private Long projectId;
    private String projectName;
}
