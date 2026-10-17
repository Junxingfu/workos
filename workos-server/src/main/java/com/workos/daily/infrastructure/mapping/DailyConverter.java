package com.workos.daily.infrastructure.mapping;

import com.workos.daily.application.readmodel.DailyResp;
import com.workos.daily.infrastructure.persistence.entity.DailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 只读存储行到展示视图的映射；业务写入经过聚合仓储。 */
@Mapper(componentModel = "spring")
public interface DailyConverter {
  /** 保留查询字段语义，不在转换时修改业务状态。 */
  @Mapping(target = "date", source = "dailyDate")
  @Mapping(target = "generated", expression = "java(po.getGeneratedContent() != null)")
  DailyResp toResp(DailyPO po);
}
