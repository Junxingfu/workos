package com.workos.project.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.model.BasePageResult;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.ItemStatus;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.project.application.query.ProjectCriteria;
import com.workos.project.application.query.ProjectQuery;
import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.application.readmodel.ProjectCountRowDTO;
import com.workos.project.application.readmodel.ProjectResp;
import com.workos.project.application.readmodel.ProjectStatRowDTO;
import com.workos.project.application.readmodel.ProjectStatisticsResp;
import com.workos.project.infrastructure.mapping.ProjectConverter;
import com.workos.project.infrastructure.persistence.entity.ProjectContextPO;
import com.workos.project.infrastructure.persistence.entity.ProjectPO;
import com.workos.project.infrastructure.persistence.mapper.ProjectContextMapper;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Project 专用查询投影；只读 SQL 与视图装配独立于聚合写入。 */
@Service
@RequiredArgsConstructor
public class ProjectQueries implements ProjectQuery {

  private final ProjectMapper projectMapper;

  private final ProjectContextMapper projectContextMapper;

  private final ItemMapper itemMapper;

  private final ProjectConverter projectConverter;

  private final TimeZoneHolder timeZoneHolder;

  public ProjectResp detail(Long id) {
    ProjectPO po = requireProject(id);
    ProjectResp response = projectConverter.toResp(po);
    ProjectStatisticsResp statistics = statistics(List.of(id)).get(id);
    response.setStatistics(Objects.requireNonNullElseGet(statistics, this::emptyStatistics));
    response.setDoingCount(response.getStatistics().getDoing());
    response.setWaitingCount(response.getStatistics().getWaiting());
    response.setOpenCount(openCount(response.getStatistics()));
    response.setTodayCount(todayCounts(List.of(id)).get(id));
    response.setCurrentFocus(contextFocus(List.of(id)).get(id));
    response.setLastActivityAt(lastActivities(List.of(id)).get(id));
    response.setContext(loadContext(id));
    return response;
  }

  public BasePageResult<ProjectResp> page(ProjectCriteria req) {
    req.fillPageDefault();
    LambdaQueryWrapper<ProjectPO> wrapper =
        new LambdaQueryWrapper<ProjectPO>()
            .eq(
                StringUtils.hasText(req.getStatus()),
                ProjectPO::getStatus,
                StringUtils.hasText(req.getStatus()) ? req.getStatus() : null)
            .like(StringUtils.hasText(req.getKeyword()), ProjectPO::getName, req.getKeyword())
            .orderByAsc(ProjectPO::getSortOrder)
            .orderByDesc(ProjectPO::getUpdatedAt);
    Page<ProjectPO> page =
        projectMapper.selectPage(new Page<>(req.getPageNo(), req.getPageSize()), wrapper);
    List<ProjectResp> records = toResps(page.getRecords());
    return new BasePageResult<>(records, page.getCurrent(), page.getSize(), page.getTotal());
  }

  public ProjectContextResp context(Long id) {
    requireProject(id);
    return loadContext(id);
  }

  private List<ProjectResp> toResps(List<ProjectPO> poList) {
    if (CollectionUtils.isEmpty(poList)) {
      return Collections.emptyList();
    }
    List<Long> projectIds = poList.stream().map(ProjectPO::getId).toList();
    Map<Long, ProjectStatisticsResp> statisticsMap = statistics(projectIds);
    Map<Long, Instant> activityMap = lastActivities(projectIds);
    Map<Long, Long> todayMap = todayCounts(projectIds);
    Map<Long, String> focusMap = contextFocus(projectIds);
    return poList.stream()
        .map(
            po -> {
              ProjectResp response = projectConverter.toResp(po);
              ProjectStatisticsResp statistics = statisticsMap.get(po.getId());
              response.setDoingCount(
                  Objects.isNull(statistics)
                      ? 0L
                      : Objects.requireNonNullElse(statistics.getDoing(), 0L));
              response.setWaitingCount(
                  Objects.isNull(statistics)
                      ? 0L
                      : Objects.requireNonNullElse(statistics.getWaiting(), 0L));
              response.setOpenCount(Objects.isNull(statistics) ? 0L : openCount(statistics));
              response.setTodayCount(todayMap.get(po.getId()));
              response.setCurrentFocus(focusMap.get(po.getId()));
              response.setLastActivityAt(activityMap.get(po.getId()));
              return response;
            })
        .toList();
  }

  private Long openCount(ProjectStatisticsResp statistics) {
    return Objects.requireNonNullElse(statistics.getTodo(), 0L)
        + Objects.requireNonNullElse(statistics.getDoing(), 0L)
        + Objects.requireNonNullElse(statistics.getWaiting(), 0L);
  }

  private Map<Long, Long> todayCounts(List<Long> projectIds) {
    if (CollectionUtils.isEmpty(projectIds)) {
      return Collections.emptyMap();
    }
    return itemMapper.selectProjectTodayCount(projectIds, timeZoneHolder.today()).stream()
        .collect(
            Collectors.toMap(
                ProjectCountRowDTO::getProjectId,
                row -> Objects.requireNonNullElse(row.getCnt(), 0L),
                (a, b) -> a));
  }

  private Map<Long, String> contextFocus(List<Long> projectIds) {
    if (CollectionUtils.isEmpty(projectIds)) {
      return Collections.emptyMap();
    }
    return projectContextMapper
        .selectList(
            new LambdaQueryWrapper<ProjectContextPO>()
                .in(ProjectContextPO::getProjectId, projectIds))
        .stream()
        .filter(context -> StringUtils.hasText(context.getFocus()))
        .collect(
            Collectors.toMap(
                ProjectContextPO::getProjectId, ProjectContextPO::getFocus, (a, b) -> a));
  }

  private Map<Long, ProjectStatisticsResp> statistics(List<Long> projectIds) {
    if (CollectionUtils.isEmpty(projectIds)) {
      return Collections.emptyMap();
    }
    Map<Long, ProjectStatisticsResp> result = new HashMap<>();
    List<ProjectStatRowDTO> rows = itemMapper.selectProjectStatRows(projectIds);
    rows.forEach(
        row -> {
          ProjectStatisticsResp statistics =
              result.computeIfAbsent(
                  row.getProjectId(),
                  key -> {
                    ProjectStatisticsResp response = new ProjectStatisticsResp();
                    response.setTodo(0L);
                    response.setDoing(0L);
                    response.setWaiting(0L);
                    response.setDone(0L);
                    return response;
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

  private ProjectStatisticsResp emptyStatistics() {
    ProjectStatisticsResp response = new ProjectStatisticsResp();
    response.setTodo(0L);
    response.setDoing(0L);
    response.setWaiting(0L);
    response.setDone(0L);
    return response;
  }

  private Map<Long, Instant> lastActivities(List<Long> projectIds) {
    if (CollectionUtils.isEmpty(projectIds)) {
      return Collections.emptyMap();
    }
    return itemMapper.selectProjectStatRows(projectIds).stream()
        .filter(row -> Objects.nonNull(row.getLastAt()))
        .collect(
            Collectors.toMap(
                ProjectStatRowDTO::getProjectId,
                ProjectStatRowDTO::getLastAt,
                (a, b) -> a.isAfter(b) ? a : b));
  }

  private ProjectContextResp loadContext(Long projectId) {
    ProjectContextPO po =
        projectContextMapper.selectOne(
            new LambdaQueryWrapper<ProjectContextPO>()
                .eq(ProjectContextPO::getProjectId, projectId));
    if (Objects.isNull(po)) {
      ProjectContextResp empty = new ProjectContextResp();
      empty.setVersion(0);
      empty.setStaleDays(0L);
      return empty;
    }
    return withStaleDays(projectConverter.toResp(po));
  }

  private ProjectContextResp withStaleDays(ProjectContextResp response) {
    if (Objects.isNull(response) || Objects.isNull(response.getUpdatedAt())) {
      return response;
    }
    response.setStaleDays(Duration.between(response.getUpdatedAt(), timeZoneHolder.now()).toDays());
    return response;
  }

  private ProjectPO requireProject(Long id) {
    ProjectPO po = Objects.isNull(id) ? null : projectMapper.selectById(id);
    if (Objects.isNull(po)) {
      throw new BizException(ResponseCode.PROJECT_NOT_FOUND);
    }
    return po;
  }
}
