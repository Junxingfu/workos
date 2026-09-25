package com.workos.settings.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class SettingsResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置响应对象
 */
@Data
public class SettingsResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "时区")
    private String timezone;

    private String dateFormat;

    private String weekStart;

    private Integer dayBoundaryHour;

    private String morningBriefTime;

    private String eodReviewTime;

    private Boolean desktopNotification;

    private String quickCaptureShortcut;
}
