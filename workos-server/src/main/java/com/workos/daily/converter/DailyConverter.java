package com.workos.daily.converter;

import com.workos.daily.data.entity.DailyPO;
import com.workos.daily.model.dto.DailyDTO;
import com.workos.daily.model.response.DailyResp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @Class DailyConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报对象转换器
 */
@Mapper(componentModel = "spring")
public interface DailyConverter {

    DailyConverter INSTANCE = Mappers.getMapper(DailyConverter.class);

    @Mapping(target = "date", source = "dailyDate")
    DailyDTO toDTO(DailyPO po);

    @Mapping(target = "dailyDate", source = "date")
    DailyPO toPO(DailyDTO dto);

    @Mapping(target = "generated", expression = "java(dto.getGeneratedContent() != null)")
    DailyResp toResp(DailyDTO dto);
}
