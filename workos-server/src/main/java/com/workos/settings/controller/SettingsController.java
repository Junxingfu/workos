package com.workos.settings.controller;

import com.workos.common.model.BaseResponse;
import com.workos.settings.model.request.SettingsUpdateReq;
import com.workos.settings.model.response.SettingsResp;
import com.workos.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Class SettingsController
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
@Tag(name = "设置", description = "时区与偏好设置")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * @Method get
     * @Return BaseResponse<SettingsResp>
     * @Desc 获取设置
     */
    @GetMapping
    @Operation(summary = "获取设置")
    public BaseResponse<SettingsResp> get() {
        return BaseResponse.success(settingsService.get());
    }

    /**
     * @Method update
     * @Param [req]
     * @Return BaseResponse<SettingsResp>
     * @Desc 更新设置
     */
    @PutMapping
    @Operation(summary = "更新设置")
    public BaseResponse<SettingsResp> update(@Valid @RequestBody SettingsUpdateReq req) {
        return BaseResponse.success(settingsService.update(req));
    }
}
