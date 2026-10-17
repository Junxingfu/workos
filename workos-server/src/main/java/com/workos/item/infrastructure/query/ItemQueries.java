package com.workos.item.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.model.BasePageResult;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.application.query.ItemCriteria;
import com.workos.item.application.query.ItemQuery;
import com.workos.item.application.readmodel.ItemActivityResp;
import com.workos.item.application.readmodel.ItemProjectDTO;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.application.readmodel.LastActivityDTO;
import com.workos.item.application.readmodel.LastLogDTO;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.TransitionPolicy;
import com.workos.item.infrastructure.mapping.ItemConverter;
import com.workos.item.infrastructure.persistence.entity.ItemActivityPO;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemActivityMapper;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.persistence.mapper.ItemProjectMapper;
import com.workos.project.infrastructure.persistence.entity.ProjectPO;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Item 专用查询投影；只读 SQL 与视图装配独立于聚合写入。 */
@Service
@RequiredArgsConstructor
public class ItemQueries implements ItemQuery {
  private static final Pattern UNCHECKED_STEP =
      Pattern.compile("^\\s*[-*+]\\s+\\[\\s]\\s+\\S", Pattern.MULTILINE);

  private final ItemMapper itemMapper;

  private final ItemProjectMapper itemProjectMapper;

  private final ItemActivityMapper itemActivityMapper;

  private final ProjectMapper projectMapper;

  private final ItemConverter itemConverter;

  private final TransitionPolicy stateMachine = new TransitionPolicy();

  private final TimeZoneHolder timeZoneHolder;

  public ItemResp detail(Long id) {
    ItemPO po = requireItem(id);
    ItemResp resp = assembles(List.of(po)).get(0);
    resp.setActivities(activities(id));
    return resp;
  }

  public BasePageResult<ItemResp> page(ItemCriteria req) {
    req.fillPageDefault();
    LambdaQueryWrapper<ItemPO> wrapper =
        new LambdaQueryWrapper<ItemPO>()
            .eq(Objects.nonNull(req.getType()), ItemPO::getItemType, req.getType())
            .eq(Objects.nonNull(req.getPriority()), ItemPO::getPriority, req.getPriority())
            .isNull(Boolean.TRUE.equals(req.getUnclassified()), ItemPO::getProjectId);
    if (req.getProjectId() != null) {
      wrapper.and(
          w ->
              w.eq(ItemPO::getProjectId, req.getProjectId())
                  .or()
                  .apply(
                      "EXISTS (SELECT 1 FROM item_project_link l WHERE l.item_id = item.id AND l.project_id = {0})",
                      req.getProjectId()));
    }
    if (Objects.nonNull(req.getStatus())) {
      wrapper.eq(ItemPO::getStatus, req.getStatus());
    } else {
      wrapper.notIn(ItemPO::getStatus, ItemStatus.DONE, ItemStatus.CANCELLED);
    }
    if (Objects.nonNull(req.getFrom())) {
      wrapper.ge(ItemPO::getDueAt, timeZoneHolder.startOfDay(req.getFrom()));
    }
    if (Objects.nonNull(req.getTo())) {
      wrapper.lt(ItemPO::getDueAt, timeZoneHolder.endOfDay(req.getTo()));
    }
    if (Boolean.TRUE.equals(req.getNoDate())) {
      wrapper.isNull(ItemPO::getPlannedDate).isNull(ItemPO::getDueAt);
    }
    if (Boolean.TRUE.equals(req.getFollowUpOnly())) {
      wrapper.isNotNull(ItemPO::getNextFollowUpAt);
    }
    if (StringUtils.hasText(req.getKeyword())) {
      String keyword = req.getKeyword().trim();
      wrapper.and(
          w -> w.like(ItemPO::getTitle, keyword).or().like(ItemPO::getDescription, keyword));
    }
    wrapper.orderByDesc(ItemPO::getUpdatedAt).orderByDesc(ItemPO::getId);
    Page<ItemPO> page =
        itemMapper.selectPage(new Page<>(req.getPageNo(), req.getPageSize()), wrapper);
    return new BasePageResult<>(
        assembles(page.getRecords()), page.getCurrent(), page.getSize(), page.getTotal());
  }

  public List<ItemActivityResp> activities(Long id) {
    requireItem(id);
    List<ItemActivityPO> poList =
        itemActivityMapper.selectList(
            new LambdaQueryWrapper<ItemActivityPO>()
                .eq(ItemActivityPO::getItemId, id)
                .orderByDesc(ItemActivityPO::getOccurredAt));
    return poList.stream().map(itemConverter::toResp).toList();
  }

  public List<ItemResp> assembles(List<ItemPO> poList) {
    if (CollectionUtils.isEmpty(poList)) {
      return Collections.emptyList();
    }
    List<Long> itemIds = poList.stream().map(ItemPO::getId).filter(Objects::nonNull).toList();
    Map<Long, String> projectNames = projectNames(poList);
    Map<Long, List<ItemProjectDTO>> related =
        itemProjectMapper.findByItems(itemIds).stream()
            .collect(Collectors.groupingBy(ItemProjectDTO::getItemId));
    Map<Long, LastLogDTO> lastLogs =
        itemMapper.selectLastLogs(itemIds).stream()
            .collect(Collectors.toMap(LastLogDTO::getItemId, Function.identity(), (a, b) -> a));
    Map<Long, LastActivityDTO> lastActivities =
        itemMapper.selectLastActivities(itemIds).stream()
            .collect(
                Collectors.toMap(LastActivityDTO::getItemId, Function.identity(), (a, b) -> a));
    Instant now = timeZoneHolder.now();
    Instant todayStart = timeZoneHolder.startOfDay(timeZoneHolder.today());
    return poList.stream()
        .map(
            po -> {
              ItemResp response = itemConverter.toResp(po);
              response.setProjectName(projectNames.get(po.getProjectId()));
              response.setRemainingSteps(countRemainingSteps(po.getDescription()));
              LastLogDTO lastLog = lastLogs.get(po.getId());
              if (Objects.nonNull(lastLog)) {
                response.setLastLogContent(lastLog.getContent());
                response.setLastLogAt(lastLog.getOccurredAt());
              }
              LastActivityDTO lastActivity = lastActivities.get(po.getId());
              if (Objects.nonNull(lastActivity)) {
                response.setLastActivityType(lastActivity.getActivityType().name());
              }
              response.setAllowedTransitions(allowedTransitions(po));
              response.setOverdue(
                  Objects.nonNull(po.getDueAt())
                      && po.getDueAt().isBefore(todayStart)
                      && (po.getStatus() == ItemStatus.TODO || po.getStatus() == ItemStatus.DOING));
              response.setFollowUpDue(
                  Objects.nonNull(po.getNextFollowUpAt())
                      && !po.getNextFollowUpAt().isAfter(now)
                      && po.getStatus() != ItemStatus.DONE
                      && po.getStatus() != ItemStatus.CANCELLED);
              response.setWaitingDays(
                  po.getStatus() == ItemStatus.WAITING ? waitingDays(po.getWaitingSince()) : null);
              response.setRelatedProjects(related.getOrDefault(po.getId(), List.of()));
              return response;
            })
        .toList();
  }

  public static Integer countRemainingSteps(String markdown) {
    if (!StringUtils.hasText(markdown)) {
      return 0;
    }
    Matcher matcher = UNCHECKED_STEP.matcher(markdown);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  public ItemPO requireItem(Long id) {
    ItemPO po = Objects.isNull(id) ? null : itemMapper.selectById(id);
    if (Objects.isNull(po)) {
      throw new BizException(ResponseCode.ITEM_NOT_FOUND);
    }
    return po;
  }

  private Set<ItemStatus> allowedTransitions(ItemPO po) {
    return stateMachine.allowedTargets(po.getStatus(), po.getItemType()).stream()
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<Long, String> projectNames(List<ItemPO> poList) {
    Set<Long> projectIds =
        poList.stream()
            .map(ItemPO::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (CollectionUtils.isEmpty(projectIds)) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(projectIds).stream()
        .collect(Collectors.toMap(ProjectPO::getId, ProjectPO::getName, (a, b) -> a));
  }

  private Long waitingDays(Instant waitingSince) {
    if (Objects.isNull(waitingSince)) {
      return 0L;
    }
    return Duration.between(waitingSince, timeZoneHolder.now()).toDays();
  }
}
