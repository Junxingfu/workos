package com.workos.project.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectCreateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 创建项目请求
 */
@Data
public class ProjectCreateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过200")
    @Schema(title = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 100, message = "项目编码长度不能超过100")
    @Schema(title = "项目编码")
    private String code;

    @Schema(title = "项目描述")
    private String description;

    @Schema(title = "颜色标识")
    private String color;

    @Schema(title = "排序值")
    private Integer sortOrder;
}
