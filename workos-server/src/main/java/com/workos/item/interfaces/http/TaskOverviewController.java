package com.workos.item.interfaces.http;

import com.workos.common.model.BaseResponse;
import com.workos.item.application.query.TaskOverviewQuery;
import com.workos.item.domain.ItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 桌面浮窗的只读 HTTP 入口；摘要计数和条目由应用查询端口提供。 */
@RestController
@RequiredArgsConstructor
public class TaskOverviewController {
  private final TaskOverviewQuery query;

  @GetMapping("/api/items/overview")
  public BaseResponse<TaskOverviewQuery.Overview> overview(
      @RequestParam(defaultValue = "TODO") ItemStatus status) {
    return BaseResponse.success(query.overview(status));
  }
}
