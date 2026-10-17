package com.workos.search.application.query;

import com.workos.common.model.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 全局搜索请求 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SearchCriteria extends BasePageReq {

  @Serial private static final long serialVersionUID = 1L;

  /** 关键词 */
  @Schema(title = "关键词")
  private String q;

  /** 限定类型，逗号分隔；为空表示全部 */
  @Schema(title = "类型：ITEM,PROJECT,DAILY")
  private String types;
}
