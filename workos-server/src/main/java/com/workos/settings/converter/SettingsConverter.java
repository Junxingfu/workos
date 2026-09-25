package com.workos.settings.converter;

import com.workos.settings.data.entity.SettingsPO;
import com.workos.settings.model.response.SettingsResp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Class SettingsConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置对象转换器
 */
@Mapper(componentModel = "spring")
public interface SettingsConverter {

    SettingsConverter INSTANCE = Mappers.getMapper(SettingsConverter.class);

    SettingsResp toResp(SettingsPO po);
}
