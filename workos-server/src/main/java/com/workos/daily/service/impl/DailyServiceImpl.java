package com.workos.daily.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.daily.converter.DailyConverter;
import com.workos.daily.data.entity.DailyPO;
import com.workos.daily.data.mapper.DailyMapper;
import com.workos.daily.model.dto.DailyDTO;
import com.workos.daily.model.request.DailyUpdateReq;
import com.workos.daily.model.response.DailyGenerateResp;
import com.workos.daily.model.response.DailyResp;
import com.workos.daily.service.DailyService;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.item.enums.ItemStatus;
import com.workos.project.data.entity.ProjectPO;
import com.workos.project.data.mapper.ProjectMapper;
import com.workos.worklog.data.entity.WorkLogPO;
import com.workos.worklog.data.mapper.WorkLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Class DailyServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报业务实现：生成是快照，apply 才覆盖；按项目分组，未归属排最后
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyServiceImpl implements DailyService {

    private static final String UNASSIGNED = "未归属";

    private final DailyMapper dailyMapper;

    private final WorkLogMapper workLogMapper;

    private final ItemMapper itemMapper;

    private final ProjectMapper projectMapper;

    private final DailyConverter dailyConverter;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method get
     * @Param [date]
     * @Return DailyResp
     * @Desc 获取日报，首次访问自动创建
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyResp get(java.time.LocalDate date) {
        return dailyConverter.toResp(dailyConverter.toDTO(requireDaily(date, true)));
    }

    /**
     * @Method update
     * @Param [date, req]
     * @Return DailyResp
     * @Desc 更新日报内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyResp update(java.time.LocalDate date, DailyUpdateReq req) {
        DailyPO po = requireDaily(date, true);
        checkVersion(po.getVersion(), req.getVersion());
        po.setContent(req.getContent());
        po.setStatus("DRAFT");
        save(po, req.getVersion());
        return dailyConverter.toResp(dailyConverter.toDTO(po));
    }

    /**
     * @Method generate
     * @Param [date]
     * @Return DailyGenerateResp
     * @Desc 生成快照：只写 generatedContent，不动 content
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyGenerateResp generate(java.time.LocalDate date) {
        List<WorkLogPO> logs = workLogMapper.selectList(new LambdaQueryWrapper<WorkLogPO>()
                .eq(WorkLogPO::getWorkDate, date)
                .orderByAsc(WorkLogPO::getOccurredAt));
        String generated = buildContent(date, logs);
        DailyPO po = requireDaily(date, true);
        po.setGeneratedContent(generated);
        dailyMapper.updateById(po);
        DailyGenerateResp resp = new DailyGenerateResp();
        resp.setDate(date);
        resp.setGeneratedContent(generated);
        resp.setSourceWorkLogCount(logs.size());
        return resp;
    }

    /**
     * @Method applyGenerated
     * @Param [date, req]
     * @Return DailyResp
     * @Desc 用户确认后覆盖 content
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyResp applyGenerated(java.time.LocalDate date, DailyUpdateReq req) {
        DailyPO po = requireDaily(date, true);
        checkVersion(po.getVersion(), req.getVersion());
        po.setContent(req.getContent());
        po.setStatus("CONFIRMED");
        save(po, req.getVersion());
        return dailyConverter.toResp(dailyConverter.toDTO(po));
    }

    /**
     * @Method week
     * @Param [weekStart]
     * @Return List<DailyResp>
     * @Desc 本周只读聚合
     */
    @Override
    public List<DailyResp> week(java.time.LocalDate weekStart) {
        java.time.LocalDate start = Objects.requireNonNullElseGet(weekStart, timeZoneHolder::today);
        List<DailyPO> poList = dailyMapper.selectList(new LambdaQueryWrapper<DailyPO>()
                .ge(DailyPO::getDailyDate, start)
                .lt(DailyPO::getDailyDate, start.plusDays(7))
                .orderByAsc(DailyPO::getDailyDate));
        return poList.stream().map(dailyConverter::toDTO).map(dailyConverter::toResp).toList();
    }

    private String buildContent(java.time.LocalDate date, List<WorkLogPO> logs) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(date).append(" 工作记录\n");
        Map<String, List<WorkLogPO>> grouped = new LinkedHashMap<>();
        Map<Long, String> projectNames = projectNames(logs);
        if (!CollectionUtils.isEmpty(logs)) {
            logs.forEach(log -> grouped
                    .computeIfAbsent(Objects.isNull(log.getProjectId()) ? UNASSIGNED
                            : projectNames.getOrDefault(log.getProjectId(), UNASSIGNED), key -> new ArrayList<>())
                    .add(log));
        }
        grouped.entrySet().stream()
                .sorted((a, b) -> UNASSIGNED.equals(a.getKey()) ? 1
                        : UNASSIGNED.equals(b.getKey()) ? -1 : a.getKey().compareTo(b.getKey()))
                .forEach(entry -> {
                    builder.append("\n## ").append(entry.getKey()).append("\n\n");
                    entry.getValue().forEach(log -> builder.append("- ").append(log.getContent()).append("\n"));
                });
        appendCompletedItems(builder, date);
        return builder.toString();
    }

    private void appendCompletedItems(StringBuilder builder, java.time.LocalDate date) {
        List<ItemPO> doneItems = itemMapper.selectList(new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getStatus, ItemStatus.DONE)
                .ge(ItemPO::getCompletedAt, timeZoneHolder.startOfDay(date))
                .lt(ItemPO::getCompletedAt, timeZoneHolder.endOfDay(date)));
        if (CollectionUtils.isEmpty(doneItems)) {
            return;
        }
        builder.append("\n## 完成事项\n\n");
        doneItems.forEach(item -> builder.append("- ").append(item.getTitle()).append("\n"));
    }

    private Map<Long, String> projectNames(List<WorkLogPO> logs) {
        List<Long> projectIds = logs.stream().map(WorkLogPO::getProjectId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollectionUtils.isEmpty(projectIds)) {
            return Map.of();
        }
        return projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectPO::getId, ProjectPO::getName, (a, b) -> a));
    }

    private DailyPO requireDaily(java.time.LocalDate date, boolean createWhenAbsent) {
        DailyPO po = dailyMapper.selectOne(new LambdaQueryWrapper<DailyPO>()
                .eq(DailyPO::getDailyDate, date));
        if (Objects.nonNull(po) || !createWhenAbsent) {
            return po;
        }
        DailyPO created = DailyPO.builder()
                .dailyDate(date)
                .content("# 今日工作\n\n")
                .status("DRAFT")
                .build();
        dailyMapper.insert(created);
        return created;
    }

    private void checkVersion(Integer current, Integer expected) {
        if (Objects.nonNull(expected) && !Objects.equals(current, expected)) {
            throw new BizException(ResponseCode.DAILY_VERSION_CONFLICT);
        }
    }

    private void save(DailyPO po, Integer expectedVersion) {
        if (Objects.nonNull(expectedVersion)) {
            po.setVersion(expectedVersion);
        }
        if (dailyMapper.updateById(po) == 0) {
            throw new BizException(ResponseCode.DAILY_VERSION_CONFLICT);
        }
    }
}
