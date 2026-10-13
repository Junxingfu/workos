package com.workos.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.model.BasePageResult;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.item.enums.ItemStatus;
import com.workos.project.converter.ProjectConverter;
import com.workos.project.data.entity.ProjectContextPO;
import com.workos.project.data.entity.ProjectPO;
import com.workos.project.data.mapper.ProjectContextMapper;
import com.workos.project.data.mapper.ProjectMapper;
import com.workos.project.enums.ProjectStatus;
import com.workos.project.model.dto.ProjectContextDTO;
import com.workos.project.model.dto.ProjectCountRowDTO;
import com.workos.project.model.dto.ProjectDTO;
import com.workos.project.model.dto.ProjectStatRowDTO;
import com.workos.project.model.dto.ProjectStatisticsDTO;
import com.workos.project.model.request.ProjectContextUpdateReq;
import com.workos.project.model.request.ProjectCreateReq;
import com.workos.project.model.request.ProjectPageReq;
import com.workos.project.model.request.ProjectUpdateReq;
import com.workos.project.model.response.ProjectContextResp;
import com.workos.project.model.response.ProjectResp;
import com.workos.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/**
 * @Class ProjectServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目业务实现：归档是业务状态，详情聚合现状与统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    private final ProjectContextMapper projectContextMapper;

    private final ItemMapper itemMapper;

    private final ProjectConverter projectConverter;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateReq req) {
        Long duplicate = projectMapper.selectCount(new LambdaQueryWrapper<ProjectPO>()
                .eq(ProjectPO::getName, req.getName()));
        if (duplicate > 0) {
            throw new BizException(ResponseCode.PROJECT_NAME_DUPLICATE);
        }
        ProjectDTO dto = projectConverter.toDTO(req);
        dto.setStatus(ProjectStatus.ACTIVE.name());
        if (Objects.isNull(dto.getSortOrder())) {
            dto.setSortOrder(0);
        }
        ProjectPO po = projectConverter.toPO(dto);
        projectMapper.insert(po);
        return po.getId();
    }

    /**
     * @Method detail
     * @Param [id]
     * @Return ProjectResp
     * @Desc 项目详情（含现状与统计）
     */
    @Override
    public ProjectResp detail(Long id) {
        ProjectPO po = requireProject(id);
        ProjectDTO dto = projectConverter.toDTO(po);
        ProjectStatisticsDTO statistics = statistics(List.of(id)).get(id);
        dto.setStatistics(Objects.requireNonNullElseGet(statistics, this::emptyStatistics));
        dto.setDoingCount(dto.getStatistics().getDoing());
        dto.setWaitingCount(dto.getStatistics().getWaiting());
        dto.setOpenCount(openCount(dto.getStatistics()));
        dto.setTodayCount(todayCounts(List.of(id)).get(id));
        dto.setCurrentFocus(contextFocus(List.of(id)).get(id));
        dto.setLastActivityAt(lastActivities(List.of(id)).get(id));
        dto.setContext(loadContext(id));
        return projectConverter.toResp(dto);
    }

    /**
     * @Method page
     * @Param [req]
     * @Return BasePageResult<ProjectResp>
     * @Desc 项目分页
     */
    @Override
    public BasePageResult<ProjectResp> page(ProjectPageReq req) {
        req.fillPageDefault();
        LambdaQueryWrapper<ProjectPO> wrapper = new LambdaQueryWrapper<ProjectPO>()
                .eq(StringUtils.hasText(req.getStatus()), ProjectPO::getStatus,
                        StringUtils.hasText(req.getStatus()) ? req.getStatus() : null)
                .like(StringUtils.hasText(req.getKeyword()), ProjectPO::getName, req.getKeyword())
                .orderByAsc(ProjectPO::getSortOrder)
                .orderByDesc(ProjectPO::getUpdatedAt);
        Page<ProjectPO> page = projectMapper.selectPage(new Page<>(req.getPageNo(), req.getPageSize()), wrapper);
        List<ProjectResp> records = toResps(page.getRecords());
        return new BasePageResult<>(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * @Method update
     * @Param [id, req]
     * @Return Boolean
     * @Desc 修改项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, ProjectUpdateReq req) {
        ProjectPO po = requireProject(id);
        checkVersion(po.getVersion(), req.getVersion());
        if (StringUtils.hasText(req.getName()) && !Objects.equals(req.getName(), po.getName())) {
            Long duplicate = projectMapper.selectCount(new LambdaQueryWrapper<ProjectPO>()
                    .eq(ProjectPO::getName, req.getName())
                    .ne(ProjectPO::getId, id));
            if (duplicate > 0) {
                throw new BizException(ResponseCode.PROJECT_NAME_DUPLICATE);
            }
            po.setName(req.getName());
        }
        if (Objects.nonNull(req.getDescription())) {
            po.setDescription(req.getDescription());
        }
        if (Objects.nonNull(req.getColor())) {
            po.setColor(req.getColor());
        }
        if (Objects.nonNull(req.getSortOrder())) {
            po.setSortOrder(req.getSortOrder());
        }
        if (StringUtils.hasText(req.getStatus())) {
            po.setStatus(req.getStatus());
        }
        boolean restoring = StringUtils.hasText(req.getStatus())
                && ProjectStatus.ACTIVE.name().equals(req.getStatus());
        if (StringUtils.hasText(req.getStatus()) && !restoring) {
            po.setArchivedAt(timeZoneHolder.now());
        }
        updateProject(po, req.getVersion());
        if (restoring) {
            clearProjectFields(po.getId(), po.getVersion(),
                    wrapper -> wrapper.set(ProjectPO::getArchivedAt, null));
        }
        return Boolean.TRUE;
    }

    /**
     * @Method archive
     * @Param [id]
     * @Return Boolean
     * @Desc 归档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean archive(Long id) {
        ProjectPO po = requireProject(id);
        po.setStatus(ProjectStatus.ARCHIVED.name());
        po.setArchivedAt(timeZoneHolder.now());
        updateProject(po, null);
        return Boolean.TRUE;
    }

    /**
     * @Method restore
     * @Param [id]
     * @Return Boolean
     * @Desc 恢复归档项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean restore(Long id) {
        ProjectPO po = requireProject(id);
        po.setStatus(ProjectStatus.ACTIVE.name());
        updateProject(po, null);
        clearProjectFields(po.getId(), po.getVersion(),
                wrapper -> wrapper.set(ProjectPO::getArchivedAt, null));
        return Boolean.TRUE;
    }

    /**
     * @Method context
     * @Param [id]
     * @Return ProjectContextResp
     * @Desc 获取项目现状
     */
    @Override
    public ProjectContextResp context(Long id) {
        requireProject(id);
        return projectConverter.toResp(loadContext(id));
    }

    /**
     * @Method updateContext
     * @Param [id, req]
     * @Return ProjectContextResp
     * @Desc 更新项目现状：首次自动创建
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectContextResp updateContext(Long id, ProjectContextUpdateReq req) {
        requireProject(id);
        ProjectContextPO po = projectContextMapper.selectOne(new LambdaQueryWrapper<ProjectContextPO>()
                .eq(ProjectContextPO::getProjectId, id));
        boolean create = Objects.isNull(po);
        checkVersion(create ? 0 : po.getVersion(), req.getVersion());
        if (create) {
            po = ProjectContextPO.builder().projectId(id).version(1).build();
        }
        mergeContext(po, req);
        po.setUpdatedAt(timeZoneHolder.now());
        if (create) {
            projectContextMapper.insert(po);
        } else {
            if (projectContextMapper.updateById(po) == 0) {
                throw new BizException(ResponseCode.VERSION_CONFLICT);
            }
        }
        return projectConverter.toResp(withStaleDays(projectConverter.toDTO(po)));
    }

    private List<ProjectResp> toResps(List<ProjectPO> poList) {
        if (CollectionUtils.isEmpty(poList)) {
            return Collections.emptyList();
        }
        List<Long> projectIds = poList.stream().map(ProjectPO::getId).toList();
        Map<Long, ProjectStatisticsDTO> statisticsMap = statistics(projectIds);
        Map<Long, Instant> activityMap = lastActivities(projectIds);
        Map<Long, Long> todayMap = todayCounts(projectIds);
        Map<Long, String> focusMap = contextFocus(projectIds);
        return poList.stream().map(po -> {
            ProjectDTO dto = projectConverter.toDTO(po);
            ProjectStatisticsDTO statistics = statisticsMap.get(po.getId());
            dto.setDoingCount(Objects.isNull(statistics) ? 0L : Objects.requireNonNullElse(statistics.getDoing(), 0L));
            dto.setWaitingCount(Objects.isNull(statistics) ? 0L : Objects.requireNonNullElse(statistics.getWaiting(), 0L));
            dto.setOpenCount(Objects.isNull(statistics) ? 0L : openCount(statistics));
            dto.setTodayCount(todayMap.get(po.getId()));
            dto.setCurrentFocus(focusMap.get(po.getId()));
            dto.setLastActivityAt(activityMap.get(po.getId()));
            return projectConverter.toResp(dto);
        }).toList();
    }

    /**
     * @Method openCount
     * @Param [statistics]
     * @Return Long
     * @Desc 未完成 = 待整理 + 待办 + 进行中 + 等待
     */
    private Long openCount(ProjectStatisticsDTO statistics) {
        return Objects.requireNonNullElse(statistics.getTodo(), 0L)
                + Objects.requireNonNullElse(statistics.getDoing(), 0L)
                + Objects.requireNonNullElse(statistics.getWaiting(), 0L);
    }

    /**
     * @Method todayCounts
     * @Param [projectIds]
     * @Return Map<Long, Long>
     * @Desc 各项目今天计划的数量
     */
    private Map<Long, Long> todayCounts(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        return itemMapper.selectProjectTodayCount(projectIds, timeZoneHolder.today()).stream()
                .collect(Collectors.toMap(ProjectCountRowDTO::getProjectId,
                        row -> Objects.requireNonNullElse(row.getCnt(), 0L), (a, b) -> a));
    }

    /**
     * @Method contextFocus
     * @Param [projectIds]
     * @Return Map<Long, String>
     * @Desc 各项目当前重点
     */
    private Map<Long, String> contextFocus(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        return projectContextMapper.selectList(new LambdaQueryWrapper<ProjectContextPO>()
                        .in(ProjectContextPO::getProjectId, projectIds)).stream()
                .filter(context -> StringUtils.hasText(context.getFocus()))
                .collect(Collectors.toMap(ProjectContextPO::getProjectId, ProjectContextPO::getFocus, (a, b) -> a));
    }

    private Map<Long, ProjectStatisticsDTO> statistics(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        Map<Long, ProjectStatisticsDTO> result = new java.util.HashMap<>();
        List<ProjectStatRowDTO> rows = itemMapper.selectProjectStatRows(projectIds);
        rows.forEach(row -> {
            ProjectStatisticsDTO statistics = result.computeIfAbsent(row.getProjectId(), key -> {
                ProjectStatisticsDTO dto = new ProjectStatisticsDTO();
                dto.setTodo(0L);
                dto.setDoing(0L);
                dto.setWaiting(0L);
                dto.setDone(0L);
                return dto;
            });
            long cnt = Objects.requireNonNullElse(row.getCnt(), 0L);
            ItemStatus status = ItemStatus.valueOf(row.getStatus());
            switch (status) {
                case TODO, INBOX -> statistics.setTodo(statistics.getTodo() + cnt);
                case DOING -> statistics.setDoing(cnt);
                case WAITING -> statistics.setWaiting(cnt);
                case DONE, CANCELLED -> statistics.setDone(statistics.getDone() + cnt);
            }
        });
        return result;
    }

    private ProjectStatisticsDTO emptyStatistics() {
        ProjectStatisticsDTO dto = new ProjectStatisticsDTO();
        dto.setTodo(0L);
        dto.setDoing(0L);
        dto.setWaiting(0L);
        dto.setDone(0L);
        return dto;
    }

    /**
     * @Method clearProjectFields
     * @Param [id, expectedVersion, setter]
     * @Desc 显式清空字段：updateById 默认忽略 null，置空必须走 UpdateWrapper
     */
    private void clearProjectFields(Long id, Integer expectedVersion, Consumer<LambdaUpdateWrapper<ProjectPO>> setter) {
        LambdaUpdateWrapper<ProjectPO> wrapper = new LambdaUpdateWrapper<ProjectPO>().eq(ProjectPO::getId, id);
        if (Objects.nonNull(expectedVersion)) {
            wrapper.eq(ProjectPO::getVersion, expectedVersion)
                    .set(ProjectPO::getVersion, expectedVersion + 1);
        }
        setter.accept(wrapper);
        wrapper.set(ProjectPO::getUpdatedAt, timeZoneHolder.now());
        projectMapper.update(null, wrapper);
    }

    private Map<Long, Instant> lastActivities(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        return itemMapper.selectProjectStatRows(projectIds).stream()
                .filter(row -> Objects.nonNull(row.getLastAt()))
                .collect(Collectors.toMap(ProjectStatRowDTO::getProjectId, ProjectStatRowDTO::getLastAt,
                        (a, b) -> a.isAfter(b) ? a : b));
    }

    private ProjectContextDTO loadContext(Long projectId) {
        ProjectContextPO po = projectContextMapper.selectOne(new LambdaQueryWrapper<ProjectContextPO>()
                .eq(ProjectContextPO::getProjectId, projectId));
        if (Objects.isNull(po)) {
            ProjectContextDTO empty = new ProjectContextDTO();
            empty.setProjectId(projectId);
            empty.setVersion(0);
            empty.setStaleDays(0L);
            return empty;
        }
        return withStaleDays(projectConverter.toDTO(po));
    }

    private ProjectContextDTO withStaleDays(ProjectContextDTO dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getUpdatedAt())) {
            return dto;
        }
        dto.setStaleDays(Duration.between(dto.getUpdatedAt(), timeZoneHolder.now()).toDays());
        return dto;
    }

    private void mergeContext(ProjectContextPO po, ProjectContextUpdateReq req) {
        ProjectContextDTO dto = projectConverter.toDTO(req);
        if (Objects.nonNull(dto.getGoal())) {
            po.setGoal(dto.getGoal());
        }
        if (Objects.nonNull(dto.getCurrentFocus())) {
            po.setFocus(dto.getCurrentFocus());
        }
        if (Objects.nonNull(dto.getProgressNote())) {
            po.setProgressNote(dto.getProgressNote());
        }
        if (Objects.nonNull(dto.getBlocker())) {
            po.setBlocker(dto.getBlocker());
        }
        if (Objects.nonNull(dto.getNextAction())) {
            po.setNextAction(dto.getNextAction());
        }
        if (Objects.nonNull(dto.getKeyPeople())) {
            po.setKeyPeople(dto.getKeyPeople());
        }
        if (Objects.nonNull(dto.getKeyLinks())) {
            po.setKeyLinks(dto.getKeyLinks());
        }
        if (Objects.nonNull(dto.getNotesMarkdown())) {
            po.setNotesMarkdown(dto.getNotesMarkdown());
        }
    }

    private ProjectPO requireProject(Long id) {
        ProjectPO po = Objects.isNull(id) ? null : projectMapper.selectById(id);
        if (Objects.isNull(po)) {
            throw new BizException(ResponseCode.PROJECT_NOT_FOUND);
        }
        return po;
    }

    private void checkVersion(Integer current, Integer expected) {
        if (Objects.nonNull(expected) && !Objects.equals(current, expected)) {
            throw new BizException(ResponseCode.VERSION_CONFLICT);
        }
    }

    private void updateProject(ProjectPO po, Integer expectedVersion) {
        if (Objects.nonNull(expectedVersion)) {
            po.setVersion(expectedVersion);
        }
        if (projectMapper.updateById(po) == 0) {
            throw new BizException(ResponseCode.VERSION_CONFLICT);
        }
    }
}
