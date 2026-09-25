package com.workos.reminder.controller;

import com.workos.common.model.BaseResponse;
import com.workos.reminder.model.request.ReminderCreateReq;
import com.workos.reminder.model.request.ReminderUpdateReq;
import com.workos.reminder.model.response.ReminderResp;
import com.workos.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * @Class ReminderController
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reminders")
@Tag(name = "提醒", description = "客户端驱动：启动补偿与前台轮询")
public class ReminderController {

    private final ReminderService reminderService;

    /**
     * @Method create
     * @Param [req]
     * @Return BaseResponse<Long>
     * @Desc 创建提醒
     */
    @PostMapping
    @Operation(summary = "创建提醒")
    public BaseResponse<Long> create(@Valid @RequestBody ReminderCreateReq req) {
        return BaseResponse.success(reminderService.create(req));
    }

    /**
     * @Method reschedule
     * @Param [id, req]
     * @Return BaseResponse<ReminderResp>
     * @Desc 稍后提醒
     */
    @PutMapping("/{id}")
    @Operation(summary = "稍后提醒")
    public BaseResponse<ReminderResp> reschedule(@PathVariable("id") Long id,
                                                 @Valid @RequestBody ReminderUpdateReq req) {
        return BaseResponse.success(reminderService.reschedule(id, req));
    }

    /**
     * @Method dismiss
     * @Param [id]
     * @Return BaseResponse<ReminderResp>
     * @Desc 忽略这次
     */
    @PostMapping("/{id}/dismiss")
    @Operation(summary = "忽略这次")
    public BaseResponse<ReminderResp> dismiss(@PathVariable("id") Long id) {
        return BaseResponse.success(reminderService.dismiss(id));
    }

    /**
     * @Method pending
     * @Return BaseResponse<List<ReminderResp>>
     * @Desc 启动补偿清单
     */
    @GetMapping("/pending")
    @Operation(summary = "启动补偿清单")
    public BaseResponse<List<ReminderResp>> pending() {
        return BaseResponse.success(reminderService.pending());
    }
}
