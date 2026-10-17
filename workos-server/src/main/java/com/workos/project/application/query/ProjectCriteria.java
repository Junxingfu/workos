package com.workos.project.application.query;

import com.workos.common.model.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 项目分页查询请求 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectCriteria extends BasePageReq {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(title = "状态：ACTIVE / ARCHIVED")
  private String status;

  @Schema(title = "关键词")
  private String keyword;
}
