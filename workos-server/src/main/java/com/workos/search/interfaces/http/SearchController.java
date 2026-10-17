package com.workos.search.interfaces.http;

import com.workos.common.model.BasePageResult;
import com.workos.common.model.BaseResponse;
import com.workos.search.application.query.SearchCriteria;
import com.workos.search.application.query.SearchQuery;
import com.workos.search.application.readmodel.SearchResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 全局搜索接口（前端 ⌘K 使用） */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "搜索", description = "全局搜索事项 / 项目 / 日报")
public class SearchController {

  private final SearchQuery searchService;

  /** 全局搜索 */
  @GetMapping
  @Operation(summary = "全局搜索")
  public BaseResponse<BasePageResult<SearchResp>> search(SearchCriteria req) {
    return BaseResponse.success(searchService.search(req));
  }
}
