package com.workos.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.reminder.converter.ReminderConverter;
import com.workos.reminder.data.entity.ReminderPO;
import com.workos.reminder.data.mapper.ReminderMapper;
import com.workos.reminder.model.dto.ReminderDTO;
import com.workos.reminder.model.request.ReminderCreateReq;
import com.workos.reminder.model.request.ReminderUpdateReq;
import com.workos.reminder.model.response.ReminderResp;
import com.workos.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * @Class ReminderServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒业务实现：状态机 PENDING → DISMISSED / CANCELLED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private static final String STATUS_PENDING = "PENDING";

    private static final String STATUS_DISMISSED = "DISMISSED";

    private static final String KIND_ITEM = "ITEM";

    private final ReminderMapper reminderMapper;

    private final ReminderConverter reminderConverter;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建提醒
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReminderCreateReq req) {
        ReminderDTO dto = reminderConverter.toDTO(req);
        dto.setKind(StringUtils.hasText(dto.getKind()) ? dto.getKind() : KIND_ITEM);
        dto.setStatus(STATUS_PENDING);
        ReminderPO po = reminderConverter.toPO(dto);
        reminderMapper.insert(po);
        return po.getId();
    }

    /**
     * @Method reschedule
     * @Param [id, req]
     * @Return ReminderResp
     * @Desc 稍后提醒：只改 remindAt
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderResp reschedule(Long id, ReminderUpdateReq req) {
        ReminderPO po = requireReminder(id);
        po.setRemindAt(req.getRemindAt());
        reminderMapper.updateById(po);
        return reminderConverter.toResp(reminderConverter.toDTO(po));
    }

    /**
     * @Method dismiss
     * @Param [id]
     * @Return ReminderResp
     * @Desc 忽略这次：只改 Reminder，不动业务事实
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderResp dismiss(Long id) {
        ReminderPO po = requireReminder(id);
        if (!STATUS_PENDING.equals(po.getStatus())) {
            throw new BizException(ResponseCode.PARAM_ERROR, "该提醒已处理");
        }
        po.setStatus(STATUS_DISMISSED);
        po.setAcknowledgedAt(timeZoneHolder.now());
        reminderMapper.updateById(po);
        return reminderConverter.toResp(reminderConverter.toDTO(po));
    }

    /**
     * @Method pending
     * @Return List<ReminderResp>
     * @Desc 启动补偿：PENDING 且 remindAt <= now
     */
    @Override
    public List<ReminderResp> pending() {
        Instant now = timeZoneHolder.now();
        List<ReminderPO> poList = reminderMapper.selectList(new LambdaQueryWrapper<ReminderPO>()
                .eq(ReminderPO::getStatus, STATUS_PENDING)
                .le(ReminderPO::getRemindAt, now)
                .orderByAsc(ReminderPO::getRemindAt));
        return poList.stream().map(reminderConverter::toDTO).map(reminderConverter::toResp).toList();
    }

    private ReminderPO requireReminder(Long id) {
        ReminderPO po = Objects.isNull(id) ? null : reminderMapper.selectById(id);
        if (Objects.isNull(po)) {
            throw new BizException(ResponseCode.REMINDER_NOT_FOUND);
        }
        return po;
    }
}
