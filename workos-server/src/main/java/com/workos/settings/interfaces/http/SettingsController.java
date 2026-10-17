package com.workos.settings.interfaces.http;

import com.workos.common.model.BaseResponse;
import com.workos.settings.application.readmodel.SettingsResp;
import com.workos.settings.interfaces.request.SettingsUpdateReq;
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

/** 设置接口 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
@Tag(name = "设置", description = "时区与偏好设置")
public class SettingsController {

  private final SettingsHttpAdapter settingsService;

  /** 获取设置 */
  @GetMapping
  @Operation(summary = "获取设置")
  public BaseResponse<SettingsResp> get() {
    return BaseResponse.success(settingsService.get());
  }

  /** 更新设置 */
  @PutMapping
  @Operation(summary = "更新设置")
  public BaseResponse<SettingsResp> update(@Valid @RequestBody SettingsUpdateReq req) {
    return BaseResponse.success(settingsService.update(req));
  }
}
