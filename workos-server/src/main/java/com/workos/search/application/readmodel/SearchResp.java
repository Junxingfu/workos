package com.workos.search.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 搜索命中项响应 */
@Data
public class SearchResp implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ITEM / PROJECT / DAILY */
  @Schema(title = "类型")
  private String type;

  private Long id;

  private String title;

  /** 命中片段，关键词用 <em> 包裹 */
  @Schema(title = "高亮片段")
  private String highlight;

  private Long projectId;

  private String projectName;

  private Instant updatedAt;
}
