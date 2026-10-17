package com.workos.inbox.interfaces.http;

import com.workos.common.model.BaseResponse;
import com.workos.inbox.application.readmodel.InboxAcceptAllResp;
import com.workos.inbox.application.readmodel.InboxResp;
import com.workos.inbox.interfaces.request.InboxAcceptAllReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 待整理接口 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inbox")
@Tag(name = "Inbox", description = "待整理队列")
public class InboxController {

  private final InboxHttpAdapter inboxService;

  /** 待整理列表 */
  @GetMapping
  @Operation(summary = "待整理列表")
  public BaseResponse<InboxResp> inbox() {
    return BaseResponse.success(inboxService.inbox());
  }

  /** 先都收下 */
  @PostMapping("/accept-all")
  @Operation(summary = "先都收下")
  public BaseResponse<InboxAcceptAllResp> acceptAll(
      @RequestBody(required = false) InboxAcceptAllReq req) {
    return BaseResponse.success(inboxService.acceptAll(req));
  }
}
