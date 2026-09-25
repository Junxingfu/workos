package com.workos.settings.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.settings.data.entity.SettingsPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class SettingsMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置数据访问
 */
@Mapper
public interface SettingsMapper extends BaseMapper<SettingsPO> {
}
