package com.workos.daily.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.daily.data.entity.DailyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class DailyMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报数据访问
 */
@Mapper
public interface DailyMapper extends BaseMapper<DailyPO> {
}
