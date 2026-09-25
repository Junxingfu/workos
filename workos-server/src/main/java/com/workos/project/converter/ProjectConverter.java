package com.workos.project.converter;

import com.workos.project.data.entity.ProjectContextPO;
import com.workos.project.data.entity.ProjectPO;
import com.workos.project.model.dto.ProjectContextDTO;
import com.workos.project.model.dto.ProjectDTO;
import com.workos.project.model.dto.ProjectStatisticsDTO;
import com.workos.project.model.request.ProjectContextUpdateReq;
import com.workos.project.model.request.ProjectCreateReq;
import com.workos.project.model.response.ProjectContextResp;
import com.workos.project.model.response.ProjectResp;
import com.workos.project.model.response.ProjectStatisticsResp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @Class ProjectConverter
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目对象转换器
 */
@Mapper(componentModel = "spring")
public interface ProjectConverter {

    ProjectConverter INSTANCE = Mappers.getMapper(ProjectConverter.class);

    ProjectDTO toDTO(ProjectCreateReq req);

    @Mapping(target = "doingCount", ignore = true)
    @Mapping(target = "waitingCount", ignore = true)
    @Mapping(target = "lastActivityAt", ignore = true)
    @Mapping(target = "statistics", ignore = true)
    @Mapping(target = "context", ignore = true)
    ProjectDTO toDTO(ProjectPO po);

    ProjectPO toPO(ProjectDTO dto);

    ProjectResp toResp(ProjectDTO dto);

    @Mapping(target = "currentFocus", source = "focus")
    ProjectContextDTO toDTO(ProjectContextPO po);

    @Mapping(target = "focus", source = "currentFocus")
    ProjectContextPO toPO(ProjectContextDTO dto);

    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentFocus", source = "currentFocus")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "staleDays", ignore = true)
    ProjectContextDTO toDTO(ProjectContextUpdateReq req);

    ProjectContextResp toResp(ProjectContextDTO dto);

    ProjectStatisticsResp toResp(ProjectStatisticsDTO dto);
}
