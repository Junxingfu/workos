package com.workos.item.model.dto;

import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemSource;
import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @Class ItemDTO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项领域对象（承载状态机与展示派生字段）
 */
@Data
@SuperBuilder(toBuilder = true)
public class ItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String projectName;

    private String title;

    private String description;

    private ItemType itemType;

    private ItemStatus status;

    private ItemPriority priority;

    private LocalDate plannedDate;

    private Integer planRank;

    private Instant dueAt;

    private Instant nextFollowUpAt;

    private String waitingFor;

    private Instant waitingSince;

    private Instant waitingExpectedAt;

    private String relatedPerson;

    private Instant completedAt;

    private Instant cancelledAt;

    private ItemSource source;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;

    /**
     * Markdown 里未勾选的 checklist 数量
     */
    private Integer remainingSteps;

    /**
     * 最近一条工作记录
     */
    private String lastLogContent;

    private Instant lastLogAt;

    /**
     * 回退用的最近一条系统事件
     */
    private String lastActivityType;

    /**
     * 当前状态允许到达的状态（前端渲染按钮用）
     */
    private Set<ItemStatus> allowedTransitions;

    /**
     * 是否逾期
     */
    private Boolean overdue;

    /**
     * 是否已到跟进时间
     */
    private Boolean followUpDue;

    /**
     * 已等待天数
     */
    private Long waitingDays;

    /**
     * 关联记录（仅详情接口填充）
     */
    private List<ItemActivityDTO> activities;
}
