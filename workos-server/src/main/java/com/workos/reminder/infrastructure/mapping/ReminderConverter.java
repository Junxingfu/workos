package com.workos.reminder.infrastructure.mapping;

import com.workos.reminder.application.readmodel.ReminderResp;
import com.workos.reminder.infrastructure.persistence.entity.ReminderPO;
import org.mapstruct.Mapper;

/** 只读存储行到展示视图的映射；业务写入经过聚合仓储。 */
@Mapper(componentModel = "spring")
public interface ReminderConverter {
  /** 保留查询字段语义，不在转换时修改业务状态。 */
  ReminderResp toResp(ReminderPO po);
}
