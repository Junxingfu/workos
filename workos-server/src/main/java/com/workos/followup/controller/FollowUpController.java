package com.workos.followup.controller;

import com.workos.common.model.BaseResponse;
import com.workos.followup.model.response.FollowUpsResp;
import com.workos.followup.service.FollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Class FollowUpController
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 等待与跟进工作台接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follow-ups")
@Tag(name = "跟进", description = "等待与跟进工作台")
public class FollowUpController {

    private final FollowUpService followUpService;

    /**
     * @Method followUps
     * @Return BaseResponse<FollowUpsResp>
     * @Desc 聚合等待与跟进
     */
    @GetMapping
    @Operation(summary = "等待与跟进工作台")
    public BaseResponse<FollowUpsResp> followUps() {
        return BaseResponse.success(followUpService.followUps());
    }
}
