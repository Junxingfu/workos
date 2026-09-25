package com.workos.search.service;

import com.workos.common.model.BasePageResult;
import com.workos.search.model.request.SearchReq;
import com.workos.search.model.response.SearchResp;

/**
 * @Class SearchService
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 全局搜索业务接口
 */
public interface SearchService {

    /**
     * @Method search
     * @Param [req]
     * @Return BasePageResult<SearchResp>
     * @Desc 检索事项 / 项目 / 日报
     */
    BasePageResult<SearchResp> search(SearchReq req);
}
