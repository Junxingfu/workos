package com.workos.worklog.converter;

import com.workos.worklog.data.entity.WorkLogPO;
import com.workos.worklog.model.dto.WorkLogDTO;
import com.workos.worklog.model.response.WorkLogResp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Class WorkLogConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录对象转换器
 */
@Mapper(componentModel = "spring")
public interface WorkLogConverter {

    WorkLogConverter INSTANCE = Mappers.getMapper(WorkLogConverter.class);

    WorkLogDTO toDTO(WorkLogPO po);

    WorkLogPO toPO(WorkLogDTO dto);

    WorkLogResp toResp(WorkLogDTO dto);
}
