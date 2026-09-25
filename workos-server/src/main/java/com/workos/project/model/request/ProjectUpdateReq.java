package com.workos.project.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectUpdateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 修改项目请求（null 表示不修改）
 */
@Data
public class ProjectUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "版本号")
    private Integer version;

    @Size(max = 200, message = "项目名称长度不能超过200")
    @Schema(title = "项目名称")
    private String name;

    @Schema(title = "项目描述")
    private String description;

    @Schema(title = "状态：ACTIVE / ARCHIVED")
    private String status;

    @Schema(title = "颜色标识")
    private String color;

    @Schema(title = "排序值")
    private Integer sortOrder;
}
