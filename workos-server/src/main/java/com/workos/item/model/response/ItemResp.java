package com.workos.item.model.response;

import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @Class ItemResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项响应对象
 */
@Data
public class ItemResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    @Schema(title = "类型")
    private ItemType type;

    @Schema(title = "状态")
    private ItemStatus status;

    @Schema(title = "优先级")
    private ItemPriority priority;

    @Schema(title = "所属项目")
    private Long projectId;

    @Schema(title = "所属项目名称")
    private String projectName;

    private String description;

    @Schema(title = "计划日期")
    private LocalDate plannedDate;

    @Schema(title = "截止时间")
    private Instant dueAt;

    @Schema(title = "下次跟进时间")
    private Instant nextFollowUpAt;

    @Schema(title = "等待对象")
    private String waitingFor;

    @Schema(title = "开始等待时间")
    private Instant waitingSince;

    @Schema(title = "对方预计时间")
    private Instant waitingExpectedAt;

    @Schema(title = "已等待天数")
    private Long waitingDays;

    @Schema(title = "相关人")
    private String relatedPerson;

    private Instant completedAt;

    private Instant cancelledAt;

    @Schema(title = "剩余步数（Markdown 未勾选清单）")
    private Integer remainingSteps;

    @Schema(title = "最近一条记录")
    private String lastLogContent;

    private Instant lastLogAt;

    @Schema(title = "最近一条系统事件")
    private String lastActivityType;

    @Schema(title = "是否逾期")
    private Boolean overdue;

    @Schema(title = "是否已到跟进时间")
    private Boolean followUpDue;

    @Schema(title = "允许到达的状态")
    private Set<ItemStatus> allowedTransitions;

    @Schema(title = "系统事件（仅详情返回）")
    private List<ItemActivityResp> activities;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;
}
