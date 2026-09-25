package com.workos.reminder.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.reminder.data.entity.ReminderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class ReminderMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒数据访问
 */
@Mapper
public interface ReminderMapper extends BaseMapper<ReminderPO> {
}
