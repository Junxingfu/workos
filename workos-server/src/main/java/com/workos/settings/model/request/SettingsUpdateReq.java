package com.workos.settings.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class SettingsUpdateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 更新设置请求
 */
@Data
public class SettingsUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "时区")
    private String timezone;

    @Schema(title = "日期格式")
    private String dateFormat;

    @Schema(title = "每周起始日")
    private String weekStart;

    @Schema(title = "日界线小时")
    private Integer dayBoundaryHour;

    @Schema(title = "早间简报时间")
    private String morningBriefTime;

    @Schema(title = "收工复盘时间")
    private String eodReviewTime;

    @Schema(title = "桌面通知开关")
    private Boolean desktopNotification;

    @Schema(title = "快速记录快捷键")
    private String quickCaptureShortcut;
}
