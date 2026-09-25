package com.workos.settings.data.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class SettingsPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置（单行）：timezone 是"今天"的唯一真源
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("settings")
public class SettingsPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Short id;

    private String timezone;

    private String dateFormat;

    private String weekStart;

    /**
     * 日界线小时；MVP 固定 0（自然日），不进界面
     */
    private Short dayBoundaryHour;

    private String morningBriefTime;

    private String eodReviewTime;

    private Boolean desktopNotification;

    private String quickCaptureShortcut;

    private Instant updatedAt;
}
