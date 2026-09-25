package com.workos.search.controller;

import com.workos.common.model.BasePageResult;
import com.workos.common.model.BaseResponse;
import com.workos.search.model.request.SearchReq;
import com.workos.search.model.response.SearchResp;
import com.workos.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Class SearchController
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 全局搜索接口（前端 ⌘K 使用）
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "搜索", description = "全局搜索事项 / 项目 / 日报")
public class SearchController {

    private final SearchService searchService;

    /**
     * @Method search
     * @Param [req]
     * @Return BaseResponse<BasePageResult<SearchResp>>
     * @Desc 全局搜索
     */
    @GetMapping
    @Operation(summary = "全局搜索")
    public BaseResponse<BasePageResult<SearchResp>> search(SearchReq req) {
        return BaseResponse.success(searchService.search(req));
    }
}
