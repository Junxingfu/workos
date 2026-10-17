package com.workos.item.infrastructure.mapping;

import com.workos.item.application.readmodel.ItemActivityResp;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.infrastructure.persistence.entity.ItemActivityPO;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 事项对象转换器 */
@Mapper(componentModel = "spring")
public interface ItemConverter {

  /** 将事项存储行映射为查询视图；派生字段由 ItemQueries 批量装配。 */
  @Mapping(target = "type", source = "itemType")
  @Mapping(target = "relatedProjects", ignore = true)
  @Mapping(target = "projectName", ignore = true)
  @Mapping(target = "remainingSteps", ignore = true)
  @Mapping(target = "lastLogContent", ignore = true)
  @Mapping(target = "lastLogAt", ignore = true)
  @Mapping(target = "lastActivityType", ignore = true)
  @Mapping(target = "overdue", ignore = true)
  @Mapping(target = "followUpDue", ignore = true)
  @Mapping(target = "waitingDays", ignore = true)
  @Mapping(target = "allowedTransitions", ignore = true)
  @Mapping(target = "activities", ignore = true)
  ItemResp toResp(ItemPO po);

  /** 将已落库的审计事件映射为时间线条目，不重建领域事件。 */
  ItemActivityResp toResp(ItemActivityPO po);
}
