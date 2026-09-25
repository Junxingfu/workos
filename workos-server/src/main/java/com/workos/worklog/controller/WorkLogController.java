package com.workos.worklog.controller;

import com.workos.common.model.BaseResponse;
import com.workos.worklog.model.request.WorkLogCreateReq;
import com.workos.worklog.model.response.WorkLogResp;
import com.workos.worklog.service.WorkLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * @Class WorkLogController
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-logs")
@Tag(name = "工作记录", description = "我具体做过什么")
public class WorkLogController {

    private final WorkLogService workLogService;

    /**
     * @Method create
     * @Param [req]
     * @Return BaseResponse<Long>
     * @Desc 创建工作记录
     */
    @PostMapping
    @Operation(summary = "创建工作记录")
    public BaseResponse<Long> create(@Valid @RequestBody WorkLogCreateReq req) {
        return BaseResponse.success(workLogService.create(req));
    }

    /**
     * @Method list
     * @Param [itemId, projectId, workDate]
     * @Return BaseResponse<List<WorkLogResp>>
     * @Desc 查询工作记录
     */
    @GetMapping
    @Operation(summary = "查询工作记录")
    public BaseResponse<List<WorkLogResp>> list(
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "workDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        return BaseResponse.success(workLogService.list(itemId, projectId, workDate));
    }
}
