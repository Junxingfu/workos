package com.workos.settings.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.settings.infrastructure.persistence.entity.SettingsPO;
import org.apache.ibatis.annotations.Mapper;

/** 设置数据访问 */
@Mapper
public interface SettingsMapper extends BaseMapper<SettingsPO> {}
