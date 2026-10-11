package com.workos.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.model.BasePageResult;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.converter.ItemConverter;
import com.workos.item.data.entity.ItemActivityPO;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.data.mapper.ItemActivityMapper;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.item.enums.ActivityType;
import com.workos.item.enums.FollowUpOutcome;
import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemSource;
import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import com.workos.item.model.dto.ItemActivityDTO;
import com.workos.item.model.dto.ItemDTO;
import com.workos.item.model.dto.LastActivityDTO;
import com.workos.item.model.dto.LastLogDTO;
import com.workos.item.model.request.ItemCreateReq;
import com.workos.item.model.request.ItemFollowUpActionReq;
import com.workos.item.model.request.ItemPageReq;
import com.workos.item.model.request.ItemPlanReorderReq;
import com.workos.item.model.request.ItemPlanReq;
import com.workos.item.model.request.ItemQuickCaptureReq;
import com.workos.item.model.request.ItemTransitionReq;
import com.workos.item.model.request.ItemUpdateReq;
import com.workos.item.model.response.ItemActivityResp;
import com.workos.item.model.response.ItemResp;
import com.workos.item.service.ItemService;
import com.workos.item.state.ItemStateMachine;
import com.workos.item.state.ItemTypeCapability;
import com.workos.project.data.entity.ProjectPO;
import com.workos.project.data.mapper.ProjectMapper;
import com.workos.worklog.data.entity.WorkLogPO;
import com.workos.worklog.data.mapper.WorkLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Class ItemServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项业务实现：状态机、等待规则、跟进规则、活动记录的唯一权威
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    /**
     * Markdown 未勾选清单
     */
    private static final Pattern UNCHECKED_STEP = Pattern.compile("^\\s*[-*+]\\s+\\[\\s]\\s+\\S", Pattern.MULTILINE);

    private final ItemMapper itemMapper;

    private final ItemActivityMapper itemActivityMapper;

    private final WorkLogMapper workLogMapper;

    private final ProjectMapper projectMapper;

    private final ItemConverter itemConverter;

    private final ItemStateMachine stateMachine;

    private final ItemTypeCapability typeCapability;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建事项
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ItemCreateReq req) {
        ItemDTO dto = itemConverter.toDTO(req);
        dto.setItemType(Objects.requireNonNullElse(req.getItemType(), ItemType.TASK));
        dto.setStatus(Objects.requireNonNullElse(req.getStatus(), ItemStatus.INBOX));
        dto.setPriority(Objects.requireNonNullElse(req.getPriority(), ItemPriority.P2));
        dto.setSource(Objects.requireNonNullElse(req.getSource(), ItemSource.MANUAL));
        return save(dto, ActivityType.CREATED, "创建事项");
    }

    /**
     * @Method quickCapture
     * @Param [req]
     * @Return Long
     * @Desc 快速记录：一句话进 Inbox
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long quickCapture(ItemQuickCaptureReq req) {
        ItemDTO dto = itemConverter.toDTO(req);
        dto.setItemType(ItemType.TASK);
        ItemQuickCaptureReq.Destination destination = Objects.requireNonNullElse(
                req.getDestination(), ItemQuickCaptureReq.Destination.INBOX);
        dto.setStatus(destination == ItemQuickCaptureReq.Destination.INBOX ? ItemStatus.INBOX : ItemStatus.TODO);
        if (destination == ItemQuickCaptureReq.Destination.TODAY) {
            dto.setPlannedDate(timeZoneHolder.today());
            dto.setPlanRank(nextPlanRank(timeZoneHolder.today()));
        }
        dto.setPriority(ItemPriority.P2);
        dto.setSource(ItemSource.QUICK_CAPTURE);
        return save(dto, ActivityType.CREATED, "快速记录");
    }

    /**
     * @Method detail
     * @Param [id]
     * @Return ItemResp
     * @Desc 事项详情
     */
    @Override
    public ItemResp detail(Long id) {
        ItemPO po = requireItem(id);
        ItemResp resp = assembles(List.of(po)).get(0);
        resp.setActivities(activities(id));
        return resp;
    }

    /**
     * @Method page
     * @Param [req]
     * @Return BasePageResult<ItemResp>
     * @Desc 全部事项分页
     */
    @Override
    public BasePageResult<ItemResp> page(ItemPageReq req) {
        req.fillPageDefault();
        LambdaQueryWrapper<ItemPO> wrapper = new LambdaQueryWrapper<ItemPO>()
                .eq(Objects.nonNull(req.getProjectId()), ItemPO::getProjectId, req.getProjectId())
                .eq(Objects.nonNull(req.getType()), ItemPO::getItemType, req.getType())
                .eq(Objects.nonNull(req.getPriority()), ItemPO::getPriority, req.getPriority())
                .isNull(Boolean.TRUE.equals(req.getUnclassified()), ItemPO::getProjectId);
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
            wrapper.and(w -> w.like(ItemPO::getTitle, keyword).or().like(ItemPO::getDescription, keyword));
        }
        wrapper.orderByDesc(ItemPO::getUpdatedAt).orderByDesc(ItemPO::getId);
        Page<ItemPO> page = itemMapper.selectPage(new Page<>(req.getPageNo(), req.getPageSize()), wrapper);
        return new BasePageResult<>(assembles(page.getRecords()),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * @Method update
     * @Param [id, req]
     * @Return Boolean
     * @Desc 属性修改：null 代表不修改，清空跟进需显式声明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, ItemUpdateReq req) {
        ItemPO po = requireItem(id);
        checkVersion(po.getVersion(), req.getVersion());
        List<String> changes = new ArrayList<>();
        if (Boolean.TRUE.equals(req.getClearProject())) changes.add("清空项目");
        if (Boolean.TRUE.equals(req.getClearDueAt())) changes.add("清空截止时间");
        if (Objects.nonNull(req.getTitle()) && !Objects.equals(req.getTitle(), po.getTitle())) {
            changes.add("标题：" + req.getTitle());
            po.setTitle(req.getTitle());
        }
        if (Objects.nonNull(req.getDescription())) {
            po.setDescription(req.getDescription());
            changes.add("描述已更新");
        }
        if (Objects.nonNull(req.getProjectId()) && !Objects.equals(req.getProjectId(), po.getProjectId())) {
            changes.add("项目：" + req.getProjectId());
            po.setProjectId(req.getProjectId());
        }
        if (Objects.nonNull(req.getItemType()) && !Objects.equals(req.getItemType(), po.getItemType())) {
            checkTypeCapability(po.getStatus(), req.getItemType());
            changes.add("类型：" + req.getItemType());
            po.setItemType(req.getItemType());
        }
        if (Objects.nonNull(req.getPriority()) && !Objects.equals(req.getPriority(), po.getPriority())) {
            changes.add("优先级：" + req.getPriority());
            po.setPriority(req.getPriority());
        }
        if (Objects.nonNull(req.getPlannedDate()) && !Objects.equals(req.getPlannedDate(), po.getPlannedDate())) {
            changes.add("计划日期：" + req.getPlannedDate());
            po.setPlannedDate(req.getPlannedDate());
        }
        if (Objects.nonNull(req.getDueAt()) && !Objects.equals(req.getDueAt(), po.getDueAt())) {
            changes.add("截止时间：" + req.getDueAt());
            po.setDueAt(req.getDueAt());
        }
        if (Objects.nonNull(req.getWaitingFor()) && !Objects.equals(req.getWaitingFor(), po.getWaitingFor())) {
            changes.add("等待对象：" + req.getWaitingFor());
            po.setWaitingFor(req.getWaitingFor());
        }
        if (Objects.nonNull(req.getRelatedPerson()) && !Objects.equals(req.getRelatedPerson(), po.getRelatedPerson())) {
            po.setRelatedPerson(req.getRelatedPerson());
        }
        if (Boolean.TRUE.equals(req.getClearNextFollowUp())) {
            if (Objects.nonNull(po.getNextFollowUpAt())) {
                changes.add("清空跟进时间");
            }
        } else if (Objects.nonNull(req.getNextFollowUpAt())
                && !Objects.equals(req.getNextFollowUpAt(), po.getNextFollowUpAt())) {
            changes.add("下次跟进：" + req.getNextFollowUpAt());
            po.setNextFollowUpAt(req.getNextFollowUpAt());
        }
        if (changes.isEmpty()) {
            return Boolean.TRUE;
        }
        updateItem(po, req.getVersion());
        if (Boolean.TRUE.equals(req.getClearProject()) || Boolean.TRUE.equals(req.getClearDueAt())) {
            clearItemFields(po.getId(), po.getVersion(), wrapper -> {
                if (Boolean.TRUE.equals(req.getClearProject())) wrapper.set(ItemPO::getProjectId, null);
                if (Boolean.TRUE.equals(req.getClearDueAt())) wrapper.set(ItemPO::getDueAt, null);
            });
            po = requireItem(id);
        }
        if (Boolean.TRUE.equals(req.getClearNextFollowUp())) {
            clearItemFields(po.getId(), po.getVersion(),
                    wrapper -> wrapper.set(ItemPO::getNextFollowUpAt, null));
        }
        recordActivity(po.getId(), ActivityType.FIELD_CHANGED, null, null,
                String.join("；", changes), "USER");
        return Boolean.TRUE;
    }

    /**
     * @Method transition
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 状态转换：状态机校验 + 类型能力校验 + 等待字段维护
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemResp transition(Long id, ItemTransitionReq req) {
        ItemPO po = requireItem(id);
        checkVersion(po.getVersion(), req.getVersion());
        ItemStatus from = po.getStatus();
        ItemStatus to = req.getToStatus();
        if (!stateMachine.allowedTargets(from, po.getItemType()).contains(to)) {
            throw new BizException(ResponseCode.ITEM_INVALID_TRANSITION,
                    String.format("不允许从 %s 变更为 %s", from, to));
        }
        checkTypeCapability(to, po.getItemType());

        boolean leavingWaiting = from == ItemStatus.WAITING && to != ItemStatus.WAITING;
        boolean enteringWaiting = to == ItemStatus.WAITING && from != ItemStatus.WAITING;
        if (enteringWaiting) {
            String waitingFor = StringUtils.hasText(req.getWaitingFor()) ? req.getWaitingFor() : po.getWaitingFor();
            if (!StringUtils.hasText(waitingFor)) {
                throw new BizException(ResponseCode.WAITING_FOR_REQUIRED);
            }
            po.setWaitingFor(waitingFor);
            if (Objects.isNull(po.getWaitingSince())) {
                po.setWaitingSince(timeZoneHolder.now());
            }
            po.setWaitingExpectedAt(req.getWaitingExpectedAt());
        } else if (to == ItemStatus.WAITING && StringUtils.hasText(req.getWaitingFor())) {
            po.setWaitingFor(req.getWaitingFor());
            po.setWaitingExpectedAt(req.getWaitingExpectedAt());
        }
        if (Objects.nonNull(req.getNextFollowUpAt())) {
            po.setNextFollowUpAt(req.getNextFollowUpAt());
        }
        Long waitingDays = null;
        if (leavingWaiting) {
            waitingDays = waitingDays(po.getWaitingSince());
        }
        po.setStatus(to);
        switch (to) {
            case DONE -> po.setCompletedAt(timeZoneHolder.now());
            case CANCELLED -> po.setCancelledAt(timeZoneHolder.now());
            default -> {
            }
        }
        updateItem(po, req.getVersion());
        if (leavingWaiting || to == ItemStatus.TODO || to == ItemStatus.INBOX) {
            clearItemFields(po.getId(), po.getVersion(), wrapper -> {
                if (leavingWaiting) {
                    // 退出等待 = 这段等待结束：等待信息与跟进时间一起收口，
                    // 否则会留下过期的 next_follow_up_at，让"待跟进"里出现已经不用催的事
                    wrapper.set(ItemPO::getWaitingFor, null)
                            .set(ItemPO::getWaitingSince, null)
                            .set(ItemPO::getWaitingExpectedAt, null)
                            .set(ItemPO::getNextFollowUpAt, null);
                }
                if (to == ItemStatus.TODO || to == ItemStatus.INBOX) {
                    wrapper.set(ItemPO::getCompletedAt, null)
                            .set(ItemPO::getCancelledAt, null);
                }
            });
        }
        if (leavingWaiting) {
            recordActivity(po.getId(), ActivityType.WAITING_ENDED, from, to,
                    "已等待 " + waitingDays + " 天", "USER");
        }
        recordActivity(po.getId(), ActivityType.STATUS_CHANGED, from, to, req.getRemark(), "USER");
        return detail(po.getId());
    }

    /**
     * @Method followUpAction
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 跟进动作：还没回 / 对方给了 / 暂时不跟
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemResp followUpAction(Long id, ItemFollowUpActionReq req) {
        ItemPO po = requireItem(id);
        checkVersion(po.getVersion(), req.getVersion());
        FollowUpOutcome outcome = req.getOutcome();
        if (outcome == FollowUpOutcome.STILL_WAITING) {
            writeWorkLog(po, StringUtils.hasText(req.getNote()) ? req.getNote()
                    : defaultStillWaitingText(po));
            po.setNextFollowUpAt(Objects.requireNonNullElse(req.getNextFollowUpAt(),
                    timeZoneHolder.now().plus(Duration.ofDays(2))));
            updateItem(po, req.getVersion());
            recordActivity(po.getId(), ActivityType.FIELD_CHANGED, null, null, "已跟进，更新下次跟进时间", "USER");
            return detail(id);
        }
        if (outcome == FollowUpOutcome.ABANDONED) {
            clearItemFields(po.getId(), po.getVersion(),
                    wrapper -> wrapper.set(ItemPO::getNextFollowUpAt, null));
            recordActivity(po.getId(), ActivityType.FIELD_CHANGED, null, null, "暂时不跟", "USER");
            return detail(id);
        }
        writeWorkLog(po, StringUtils.hasText(req.getNote()) ? req.getNote() : defaultResolvedText(po));
        ItemStatus to = Objects.requireNonNullElse(req.getToStatus(),
                po.getStatus() == ItemStatus.WAITING ? ItemStatus.DOING : po.getStatus());
        clearItemFields(po.getId(), po.getVersion(),
                wrapper -> wrapper.set(ItemPO::getNextFollowUpAt, null));
        if (to == po.getStatus()) {
            recordActivity(po.getId(), ActivityType.FIELD_CHANGED, null, null, "跟进完成", "USER");
            return detail(id);
        }
        ItemTransitionReq transitionReq = new ItemTransitionReq();
        transitionReq.setToStatus(to);
        transitionReq.setRemark("跟进完成：" + req.getNote());
        return transition(id, transitionReq);
    }

    /**
     * @Method plan
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 加入今日计划 / 排到某天
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemResp plan(Long id, ItemPlanReq req) {
        ItemPO po = requireItem(id);
        checkVersion(po.getVersion(), req.getVersion());
        if (req.getPlannedDate() == null) req.setPlannedDate(timeZoneHolder.today());
        if (req.getPlannedDate().isBefore(timeZoneHolder.today())) {
            throw new BizException(ResponseCode.PARAM_ERROR, "计划日期不能早于今天");
        }
        if (po.getItemType() != ItemType.TASK ||
                (po.getStatus() != ItemStatus.TODO && po.getStatus() != ItemStatus.DOING && po.getStatus() != ItemStatus.WAITING)) {
            throw new BizException(ResponseCode.ITEM_INVALID_TRANSITION, "当前状态不能加入计划");
        }
        po.setPlannedDate(req.getPlannedDate());
        po.setPlanRank(nextPlanRank(req.getPlannedDate()));
        updateItem(po, req.getVersion());
        recordActivity(po.getId(), ActivityType.PLAN_CHANGED, null, null,
                "排到 " + req.getPlannedDate(), "USER");
        return detail(id);
    }

    /**
     * @Method unplan
     * @Param [id]
     * @Return Boolean
     * @Desc 移出今日计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unplan(Long id) {
        ItemPO po = requireItem(id);
        clearItemFields(po.getId(), po.getVersion(), wrapper -> wrapper
                .set(ItemPO::getPlannedDate, null)
                .set(ItemPO::getPlanRank, null));
        recordActivity(po.getId(), ActivityType.PLAN_CHANGED, null, null, "移出计划", "USER");
        return Boolean.TRUE;
    }

    /**
     * @Method reorderPlan
     * @Param [req]
     * @Return Boolean
     * @Desc 调整今日计划顺序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reorderPlan(ItemPlanReorderReq req) {
        if (CollectionUtils.isEmpty(req.getItemIds())) {
            return Boolean.TRUE;
        }
        int rank = 1;
        for (Long itemId : req.getItemIds()) {
            ItemPO po = itemMapper.selectById(itemId);
            if (Objects.isNull(po) || !Objects.equals(po.getPlannedDate(), req.getDate())) {
                continue;
            }
            po.setPlanRank(rank++);
            updateItem(po, null);
        }
        return Boolean.TRUE;
    }

    /**
     * @Method delete
     * @Param [id]
     * @Return Boolean
     * @Desc 删除：整理完成条件放宽后，仅未整理事项可物理删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        ItemPO po = requireItem(id);
        if (po.getStatus() != ItemStatus.INBOX) {
            throw new BizException(ResponseCode.ITEM_DELETE_NOT_ALLOWED);
        }
        itemActivityMapper.delete(new LambdaQueryWrapper<ItemActivityPO>()
                .eq(ItemActivityPO::getItemId, id));
        itemMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * @Method activities
     * @Param [id]
     * @Return List<ItemActivityResp>
     * @Desc 查询系统事件
     */
    @Override
    public List<ItemActivityResp> activities(Long id) {
        requireItem(id);
        List<ItemActivityPO> poList = itemActivityMapper.selectList(new LambdaQueryWrapper<ItemActivityPO>()
                .eq(ItemActivityPO::getItemId, id)
                .orderByDesc(ItemActivityPO::getOccurredAt));
        return poList.stream().map(itemConverter::toDTO).map(itemConverter::toResp).toList();
    }

    /**
     * @Method assembles
     * @Param [poList]
     * @Return List<ItemResp>
     * @Desc 批量装配展示字段
     */
    @Override
    public List<ItemResp> assembles(List<ItemPO> poList) {
        if (CollectionUtils.isEmpty(poList)) {
            return Collections.emptyList();
        }
        List<Long> itemIds = poList.stream().map(ItemPO::getId).filter(Objects::nonNull).toList();
        Map<Long, String> projectNames = projectNames(poList);
        Map<Long, LastLogDTO> lastLogs = itemMapper.selectLastLogs(itemIds).stream()
                .collect(Collectors.toMap(LastLogDTO::getItemId, Function.identity(), (a, b) -> a));
        Map<Long, LastActivityDTO> lastActivities = itemMapper.selectLastActivities(itemIds).stream()
                .collect(Collectors.toMap(LastActivityDTO::getItemId, Function.identity(), (a, b) -> a));
        Instant now = timeZoneHolder.now();
        Instant todayStart = timeZoneHolder.startOfDay(timeZoneHolder.today());
        return poList.stream().map(po -> {
            ItemDTO dto = itemConverter.toDTO(po);
            dto.setProjectName(projectNames.get(po.getProjectId()));
            dto.setRemainingSteps(countRemainingSteps(po.getDescription()));
            LastLogDTO lastLog = lastLogs.get(po.getId());
            if (Objects.nonNull(lastLog)) {
                dto.setLastLogContent(lastLog.getContent());
                dto.setLastLogAt(lastLog.getOccurredAt());
            }
            LastActivityDTO lastActivity = lastActivities.get(po.getId());
            if (Objects.nonNull(lastActivity)) {
                dto.setLastActivityType(lastActivity.getActivityType().name());
            }
            dto.setAllowedTransitions(allowedTransitions(po));
            dto.setOverdue(Objects.nonNull(po.getDueAt())
                    && po.getDueAt().isBefore(todayStart)
                    && (po.getStatus() == ItemStatus.TODO || po.getStatus() == ItemStatus.DOING));
            dto.setFollowUpDue(Objects.nonNull(po.getNextFollowUpAt())
                    && !po.getNextFollowUpAt().isAfter(now)
                    && po.getStatus() != ItemStatus.DONE
                    && po.getStatus() != ItemStatus.CANCELLED);
            dto.setWaitingDays(po.getStatus() == ItemStatus.WAITING ? waitingDays(po.getWaitingSince()) : null);
            return itemConverter.toResp(dto);
        }).toList();
    }

    /**
     * @Method countRemainingSteps
     * @Param [markdown]
     * @Return Integer
     * @Desc 统计 Markdown 中未勾选的清单项
     */
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

    private Long save(ItemDTO dto, ActivityType activityType, String remark) {
        if (dto.getStatus() == ItemStatus.WAITING && !StringUtils.hasText(dto.getWaitingFor())) {
            throw new BizException(ResponseCode.WAITING_FOR_REQUIRED);
        }
        checkTypeCapability(dto.getStatus(), dto.getItemType());
        if (dto.getProjectId() != null && Objects.isNull(projectMapper.selectById(dto.getProjectId()))) {
            throw new BizException(ResponseCode.PROJECT_NOT_FOUND);
        }
        if (dto.getStatus() == ItemStatus.WAITING) {
            dto.setWaitingSince(timeZoneHolder.now());
        }
        ItemPO po = itemConverter.toPO(dto);
        itemMapper.insert(po);
        recordActivity(po.getId(), activityType, null, po.getStatus(), remark, "USER");
        return po.getId();
    }

    private ItemPO requireItem(Long id) {
        ItemPO po = Objects.isNull(id) ? null : itemMapper.selectById(id);
        if (Objects.isNull(po)) {
            throw new BizException(ResponseCode.ITEM_NOT_FOUND);
        }
        return po;
    }

    private void checkVersion(Integer current, Integer expected) {
        if (Objects.nonNull(expected) && !Objects.equals(current, expected)) {
            throw new BizException(ResponseCode.VERSION_CONFLICT);
        }
    }

    private void checkTypeCapability(ItemStatus target, ItemType type) {
        if (!typeCapability.allows(target, type)) {
            throw new BizException(ResponseCode.ITEM_TYPE_CAPABILITY_DENIED,
                    String.format("NOTE 不支持 %s", target));
        }
    }

    private void updateItem(ItemPO po, Integer expectedVersion) {
        if (Objects.nonNull(expectedVersion)) {
            po.setVersion(expectedVersion);
        }
        if (itemMapper.updateById(po) == 0) {
            throw new BizException(ResponseCode.VERSION_CONFLICT);
        }
    }

    /**
     * @Method clearItemFields
     * @Param [id, expectedVersion, setter]
     * @Desc 显式清空字段：updateById 默认忽略 null，置空必须走 UpdateWrapper
     */
    private void clearItemFields(Long id, Integer expectedVersion, Consumer<LambdaUpdateWrapper<ItemPO>> setter) {
        LambdaUpdateWrapper<ItemPO> wrapper = new LambdaUpdateWrapper<ItemPO>().eq(ItemPO::getId, id);
        if (Objects.nonNull(expectedVersion)) {
            wrapper.eq(ItemPO::getVersion, expectedVersion)
                    .set(ItemPO::getVersion, expectedVersion + 1);
        }
        setter.accept(wrapper);
        wrapper.set(ItemPO::getUpdatedAt, timeZoneHolder.now());
        if (itemMapper.update(null, wrapper) == 0) {
            throw new BizException(ResponseCode.VERSION_CONFLICT);
        }
    }

    private Set<ItemStatus> allowedTransitions(ItemPO po) {
        return stateMachine.allowedTargets(po.getStatus(), po.getItemType()).stream()
                .filter(target -> typeCapability.allows(target, po.getItemType()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, String> projectNames(List<ItemPO> poList) {
        Set<Long> projectIds = poList.stream().map(ItemPO::getProjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        return projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectPO::getId, ProjectPO::getName, (a, b) -> a));
    }

    private Integer nextPlanRank(LocalDate date) {
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getPlannedDate, date));
        return count.intValue() + 1;
    }

    private Long waitingDays(Instant waitingSince) {
        if (Objects.isNull(waitingSince)) {
            return 0L;
        }
        return Duration.between(waitingSince, timeZoneHolder.now()).toDays();
    }

    private String defaultStillWaitingText(ItemPO po) {
        return StringUtils.hasText(po.getWaitingFor())
                ? "已催 " + po.getWaitingFor() + "，对方还没回"
                : "已跟进，等待对方回复";
    }

    private String defaultResolvedText(ItemPO po) {
        return StringUtils.hasText(po.getWaitingFor())
                ? po.getWaitingFor() + " 已给出结果"
                : "跟进已闭环";
    }

    private void writeWorkLog(ItemPO po, String content) {
        Instant now = timeZoneHolder.now();
        workLogMapper.insert(WorkLogPO.builder()
                .itemId(po.getId())
                .projectId(po.getProjectId())
                .workDate(timeZoneHolder.toDate(now))
                .occurredAt(now)
                .content(content)
                .build());
    }

    private void recordActivity(Long itemId, ActivityType activityType, ItemStatus from,
                                ItemStatus to, String remark, String actor) {
        itemActivityMapper.insert(ItemActivityPO.builder()
                .itemId(itemId)
                .activityType(activityType)
                .fromStatus(Objects.isNull(from) ? null : from.name())
                .toStatus(Objects.isNull(to) ? null : to.name())
                .remark(remark)
                .actor(actor)
                .occurredAt(timeZoneHolder.now())
                .build());
    }

    /**
     * @Method findActivities
     * @Param [itemId]
     * @Return List<ItemActivityDTO>
     * @Desc 供其它模块复用的系统事件查询
     */
    public List<ItemActivityDTO> findActivities(Long itemId) {
        return itemActivityMapper.selectList(new LambdaQueryWrapper<ItemActivityPO>()
                        .eq(ItemActivityPO::getItemId, itemId)
                        .orderByDesc(ItemActivityPO::getOccurredAt)).stream()
                .map(itemConverter::toDTO)
                .toList();
    }
}
