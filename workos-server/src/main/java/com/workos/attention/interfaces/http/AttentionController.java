package com.workos.attention.interfaces.http;

import com.workos.attention.application.AttentionApplicationService;
import com.workos.common.enums.ResponseCode;
import com.workos.common.model.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 应用内关注列表与响应入口；读写回执不等于改变任务进度。 */
@RestController
@RequestMapping("/api/attention")
@RequiredArgsConstructor
public class AttentionController {
  private final AttentionApplicationService service;

  @GetMapping
  public BaseResponse<?> list() {
    return BaseResponse.success(service.list());
  }

  public record Response(String key, String action) {}

  @PostMapping
  public BaseResponse<?> respond(@RequestBody Response req) {
    service.respond(req.key(), req.action());
    return BaseResponse.success();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public BaseResponse<?> invalid(IllegalArgumentException e) {
    return BaseResponse.failure(ResponseCode.PARAM_ERROR, e.getMessage());
  }
}
