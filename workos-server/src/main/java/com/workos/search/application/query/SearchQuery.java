package com.workos.search.application.query;

import com.workos.common.model.BasePageResult;
import com.workos.search.application.readmodel.SearchResp;

/** 全局搜索业务接口 */
public interface SearchQuery {

  /** 检索事项 / 项目 / 日报 */
  BasePageResult<SearchResp> search(SearchCriteria req);
}
