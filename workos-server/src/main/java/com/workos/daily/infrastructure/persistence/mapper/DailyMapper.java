package com.workos.daily.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.daily.infrastructure.persistence.entity.DailyPO;
import org.apache.ibatis.annotations.Mapper;

/** 日报数据访问 */
@Mapper
public interface DailyMapper extends BaseMapper<DailyPO> {}
