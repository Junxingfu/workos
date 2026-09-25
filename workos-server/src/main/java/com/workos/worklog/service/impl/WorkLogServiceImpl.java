package com.workos.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.data.mapper.ItemMapper;
import com.workos.worklog.converter.WorkLogConverter;
import com.workos.worklog.data.entity.WorkLogPO;
import com.workos.worklog.data.mapper.WorkLogMapper;
import com.workos.worklog.model.dto.WorkLogDTO;
import com.workos.worklog.model.request.WorkLogCreateReq;
import com.workos.worklog.model.response.WorkLogResp;
import com.workos.worklog.service.WorkLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * @Class WorkLogServiceImpl
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录业务实现：workDate 与 occurredAt 都必须落库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl implements WorkLogService {

    private final WorkLogMapper workLogMapper;

    private final ItemMapper itemMapper;

    private final WorkLogConverter workLogConverter;

    private final TimeZoneHolder timeZoneHolder;

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建工作记录：项目缺省继承事项
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkLogCreateReq req) {
        ItemPO item = null;
        if (Objects.nonNull(req.getItemId())) {
            item = itemMapper.selectById(req.getItemId());
            if (Objects.isNull(item)) {
                throw new BizException(ResponseCode.ITEM_NOT_FOUND);
            }
        }
        Instant occurredAt = Objects.requireNonNullElseGet(req.getOccurredAt(), timeZoneHolder::now);
        WorkLogDTO dto = new WorkLogDTO();
        dto.setItemId(req.getItemId());
        dto.setProjectId(Objects.nonNull(req.getProjectId()) ? req.getProjectId()
                : Objects.isNull(item) ? null : item.getProjectId());
        dto.setOccurredAt(occurredAt);
        dto.setWorkDate(Objects.requireNonNullElseGet(req.getWorkDate(),
                () -> timeZoneHolder.toDate(occurredAt)));
        dto.setContent(req.getContent());
        WorkLogPO po = workLogConverter.toPO(dto);
        workLogMapper.insert(po);
        return po.getId();
    }

    /**
     * @Method list
     * @Param [itemId, projectId, workDate]
     * @Return List<WorkLogResp>
     * @Desc 查询工作记录
     */
    @Override
    public List<WorkLogResp> list(Long itemId, Long projectId, LocalDate workDate) {
        List<WorkLogPO> poList = workLogMapper.selectList(new LambdaQueryWrapper<WorkLogPO>()
                .eq(Objects.nonNull(itemId), WorkLogPO::getItemId, itemId)
                .eq(Objects.nonNull(projectId), WorkLogPO::getProjectId, projectId)
                .eq(Objects.nonNull(workDate), WorkLogPO::getWorkDate, workDate)
                .orderByDesc(WorkLogPO::getOccurredAt));
        return poList.stream().map(workLogConverter::toDTO).map(workLogConverter::toResp).toList();
    }
}
