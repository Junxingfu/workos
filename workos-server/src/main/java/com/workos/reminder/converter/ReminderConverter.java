package com.workos.reminder.converter;

import com.workos.reminder.data.entity.ReminderPO;
import com.workos.reminder.model.dto.ReminderDTO;
import com.workos.reminder.model.request.ReminderCreateReq;
import com.workos.reminder.model.response.ReminderResp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Class ReminderConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒对象转换器
 */
@Mapper(componentModel = "spring")
public interface ReminderConverter {

    ReminderConverter INSTANCE = Mappers.getMapper(ReminderConverter.class);

    ReminderDTO toDTO(ReminderCreateReq req);

    ReminderDTO toDTO(ReminderPO po);

    ReminderPO toPO(ReminderDTO dto);

    ReminderResp toResp(ReminderDTO dto);
}
