package com.workos.reminder.service;

import com.workos.reminder.model.request.ReminderCreateReq;
import com.workos.reminder.model.request.ReminderUpdateReq;
import com.workos.reminder.model.response.ReminderResp;

import java.util.List;

/**
 * @Class ReminderService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒业务接口：MVP 由客户端驱动，后端不做触发
 */
public interface ReminderService {

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建提醒
     */
    Long create(ReminderCreateReq req);

    /**
     * @Method reschedule
     * @Param [id, req]
     * @Return ReminderResp
     * @Desc 稍后提醒
     */
    ReminderResp reschedule(Long id, ReminderUpdateReq req);

    /**
     * @Method dismiss
     * @Param [id]
     * @Return ReminderResp
     * @Desc 忽略这次
     */
    ReminderResp dismiss(Long id);

    /**
     * @Method pending
     * @Return List<ReminderResp>
     * @Desc 启动补偿：PENDING 且到点的提醒
     */
    List<ReminderResp> pending();
}
