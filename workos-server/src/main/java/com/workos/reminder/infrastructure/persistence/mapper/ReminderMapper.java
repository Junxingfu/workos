package com.workos.reminder.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.reminder.infrastructure.persistence.entity.ReminderPO;
import org.apache.ibatis.annotations.Mapper;

/** 提醒数据访问 */
@Mapper
public interface ReminderMapper extends BaseMapper<ReminderPO> {}
