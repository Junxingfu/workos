package com.workos.item.model.request;

import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemSource;
import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class ItemCreateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 创建事项请求
 */
@Data
public class ItemCreateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题长度不能超过500")
    @Schema(title = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /**
     * 所属项目
     */
    @Schema(title = "所属项目")
    private Long projectId;

    /**
     * 描述（Markdown）
     */
    @Size(max = 20_000, message = "描述长度不能超过20000")
    @Schema(title = "描述")
    private String description;

    /**
     * 类型：TASK / NOTE
     */
    @Schema(title = "类型")
    private ItemType itemType;

    /**
     * 状态
     */
    @Schema(title = "状态")
    private ItemStatus status;

    /**
     * 优先级
     */
    @Schema(title = "优先级")
    private ItemPriority priority;

    /**
     * 计划日期
     */
    @Schema(title = "计划日期")
    private LocalDate plannedDate;

    /**
     * 截止时间
     */
    @Schema(title = "截止时间")
    private Instant dueAt;

    /**
     * 下次跟进时间
     */
    @Schema(title = "下次跟进时间")
    private Instant nextFollowUpAt;

    /**
     * 等待对象
     */
    @Schema(title = "等待对象")
    private String waitingFor;

    /**
     * 相关人
     */
    @Schema(title = "相关人")
    private String relatedPerson;

    /**
     * 来源
     */
    @Schema(title = "来源")
    private ItemSource source;
}
