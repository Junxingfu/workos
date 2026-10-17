package com.workos.reminder.interfaces.http;

import com.workos.common.model.BaseResponse;
import com.workos.reminder.application.readmodel.ReminderResp;
import com.workos.reminder.interfaces.request.ReminderCreateReq;
import com.workos.reminder.interfaces.request.ReminderUpdateReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提醒接口 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reminders")
@Tag(name = "提醒", description = "客户端驱动：启动补偿与前台轮询")
public class ReminderController {

  private final ReminderHttpAdapter reminderService;

  /** 创建提醒 */
  @PostMapping
  @Operation(summary = "创建提醒")
  public BaseResponse<Long> create(@Valid @RequestBody ReminderCreateReq req) {
    return BaseResponse.success(reminderService.create(req));
  }

  /** 稍后提醒 */
  @PutMapping("/{id}")
  @Operation(summary = "稍后提醒")
  public BaseResponse<ReminderResp> reschedule(
      @PathVariable("id") Long id, @Valid @RequestBody ReminderUpdateReq req) {
    return BaseResponse.success(reminderService.reschedule(id, req));
  }

  /** 忽略这次 */
  @PostMapping("/{id}/dismiss")
  @Operation(summary = "忽略这次")
  public BaseResponse<ReminderResp> dismiss(@PathVariable("id") Long id) {
    return BaseResponse.success(reminderService.dismiss(id));
  }

  /** 启动补偿清单 */
  @GetMapping("/pending")
  @Operation(summary = "启动补偿清单")
  public BaseResponse<List<ReminderResp>> pending() {
    return BaseResponse.success(reminderService.pending());
  }
}
