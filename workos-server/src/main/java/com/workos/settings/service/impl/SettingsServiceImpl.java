package com.workos.settings.service.impl;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.settings.converter.SettingsConverter;
import com.workos.settings.data.entity.SettingsPO;
import com.workos.settings.data.mapper.SettingsMapper;
import com.workos.settings.model.request.SettingsUpdateReq;
import com.workos.settings.model.response.SettingsResp;
import com.workos.settings.service.SettingsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * @Class SettingsServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置业务实现：启动即加载时区，保证"今天"口径唯一
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    /**
     * 单行设置固定主键
     */
    private static final Short SINGLE_ROW_ID = 1;

    private final SettingsMapper settingsMapper;

    private final SettingsConverter settingsConverter;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method init
     * @Desc 启动加载时区，避免"今天"口径与库内配置不一致
     */
    @PostConstruct
    public void init() {
        SettingsPO po = settingsMapper.selectById(SINGLE_ROW_ID);
        if (Objects.isNull(po)) {
            settingsMapper.insert(SettingsPO.builder()
                    .id(SINGLE_ROW_ID)
                    .timezone("Asia/Shanghai")
                    .dateFormat("YYYY-MM-DD")
                    .weekStart("MONDAY")
                    .dayBoundaryHour((short) 0)
                    .morningBriefTime("09:00")
                    .eodReviewTime("18:00")
                    .desktopNotification(Boolean.TRUE)
                    .quickCaptureShortcut("Alt+Space")
                    .updatedAt(Instant.now())
                    .build());
            return;
        }
        refreshTimeZone(po);
        log.info("设置加载完成 timezone={} dayBoundaryHour={}", po.getTimezone(), po.getDayBoundaryHour());
    }

    /**
     * @Method get
     * @Return SettingsResp
     * @Desc 获取设置
     */
    @Override
    public SettingsResp get() {
        return settingsConverter.toResp(requireSettings());
    }

    /**
     * @Method update
     * @Param [req]
     * @Return SettingsResp
     * @Desc 更新设置：null 不修改，时区变更立即生效
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettingsResp update(SettingsUpdateReq req) {
        SettingsPO po = requireSettings();
        if (StringUtils.hasText(req.getTimezone())) {
            po.setTimezone(req.getTimezone());
        }
        if (StringUtils.hasText(req.getDateFormat())) {
            po.setDateFormat(req.getDateFormat());
        }
        if (StringUtils.hasText(req.getWeekStart())) {
            po.setWeekStart(req.getWeekStart());
        }
        if (Objects.nonNull(req.getDayBoundaryHour())) {
            po.setDayBoundaryHour(req.getDayBoundaryHour().shortValue());
        }
        if (StringUtils.hasText(req.getMorningBriefTime())) {
            po.setMorningBriefTime(req.getMorningBriefTime());
        }
        if (StringUtils.hasText(req.getEodReviewTime())) {
            po.setEodReviewTime(req.getEodReviewTime());
        }
        if (Objects.nonNull(req.getDesktopNotification())) {
            po.setDesktopNotification(req.getDesktopNotification());
        }
        if (StringUtils.hasText(req.getQuickCaptureShortcut())) {
            po.setQuickCaptureShortcut(req.getQuickCaptureShortcut());
        }
        po.setUpdatedAt(Instant.now());
        settingsMapper.updateById(po);
        refreshTimeZone(po);
        return settingsConverter.toResp(po);
    }

    private void refreshTimeZone(SettingsPO po) {
        timeZoneHolder.refresh(po.getTimezone(),
                Objects.isNull(po.getDayBoundaryHour()) ? null : po.getDayBoundaryHour().intValue());
    }

    private SettingsPO requireSettings() {
        SettingsPO po = settingsMapper.selectById(SINGLE_ROW_ID);
        if (Objects.isNull(po)) {
            throw new BizException(ResponseCode.DATA_NOT_FOUND, "设置未初始化");
        }
        return po;
    }
}
