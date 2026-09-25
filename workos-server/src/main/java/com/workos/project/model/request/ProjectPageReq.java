package com.workos.project.model.request;

import com.workos.common.model.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/**
 * @Class ProjectPageReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectPageReq extends BasePageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "状态：ACTIVE / ARCHIVED")
    private String status;

    @Schema(title = "关键词")
    private String keyword;
}
