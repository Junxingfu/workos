package com.workos.item.converter;

import com.workos.item.data.entity.ItemActivityPO;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.model.dto.ItemActivityDTO;
import com.workos.item.model.dto.ItemDTO;
import com.workos.item.model.dto.LastActivityDTO;
import com.workos.item.model.dto.LastLogDTO;
import com.workos.item.model.request.ItemCreateReq;
import com.workos.item.model.request.ItemQuickCaptureReq;
import com.workos.item.model.response.ItemActivityResp;
import com.workos.item.model.response.ItemResp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @Class ItemConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项对象转换器
 */
@Mapper(componentModel = "spring")
public interface ItemConverter {

    ItemConverter INSTANCE = Mappers.getMapper(ItemConverter.class);

    /**
     * @Method toDTO
     * @Param [req]
     * @Return ItemDTO
     * @Desc 创建请求转领域对象
     */
    ItemDTO toDTO(ItemCreateReq req);

    /**
     * @Method toDTO
     * @Param [req]
     * @Return ItemDTO
     * @Desc 快速记录转领域对象
     */
    @Mapping(target = "title", source = "content")
    ItemDTO toDTO(ItemQuickCaptureReq req);

    /**
     * @Method toPO
     * @Param [dto]
     * @Return ItemPO
     * @Desc 领域对象转实体
     */
    ItemPO toPO(ItemDTO dto);

    /**
     * @Method toDTO
     * @Param [po]
     * @Return ItemDTO
     * @Desc 实体转领域对象
     */
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "remainingSteps", ignore = true)
    @Mapping(target = "lastLogContent", ignore = true)
    @Mapping(target = "lastLogAt", ignore = true)
    @Mapping(target = "lastActivityType", ignore = true)
    @Mapping(target = "allowedTransitions", ignore = true)
    @Mapping(target = "overdue", ignore = true)
    @Mapping(target = "followUpDue", ignore = true)
    @Mapping(target = "waitingDays", ignore = true)
    @Mapping(target = "activities", ignore = true)
    ItemDTO toDTO(ItemPO po);

    /**
     * @Method toResp
     * @Param [dto]
     * @Return ItemResp
     * @Desc 领域对象转响应
     */
    @Mapping(target = "type", source = "itemType")
    @Mapping(target = "relatedProjects", ignore = true)
    ItemResp toResp(ItemDTO dto);

    /**
     * @Method toResp
     * @Param [po]
     * @Return ItemResp
     * @Desc 实体直接转响应
     */
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

    /**
     * @Method toDTO
     * @Param [po]
     * @Return ItemActivityDTO
     * @Desc 系统事件转领域对象
     */
    ItemActivityDTO toDTO(ItemActivityPO po);

    /**
     * @Method toResp
     * @Param [dto]
     * @Return ItemActivityResp
     * @Desc 系统事件转响应
     */
    ItemActivityResp toResp(ItemActivityDTO dto);

    /**
     * @Method toLastLogDTO
     * @Param [dto]
     * @Return LastLogDTO
     * @Desc 保留方法签名，便于后续扩展
     */
    LastLogDTO toLastLogDTO(LastLogDTO dto);

    /**
     * @Method toLastActivityDTO
     * @Param [dto]
     * @Return LastActivityDTO
     * @Desc 保留方法签名，便于后续扩展
     */
    LastActivityDTO toLastActivityDTO(LastActivityDTO dto);
}
