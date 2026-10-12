package com.workos.item.model.request;

import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class ItemUpdateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 属性修改请求（不允许改 status，状态必须走 transitions）
 */
@Data
public class ItemUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 乐观锁版本
     */
    @Schema(title = "版本号")
    private Integer version;

    /**
     * 所属项目
     */
    @Schema(title = "所属项目")
    private Long projectId;

    /** null 不修改，空数组清空关联项目。 */
    @Size(max = 20)
    private java.util.List<Long> relatedProjectIds;

    /**
     * 标题
     */
    @Size(max = 500, message = "标题长度不能超过500")
    @Schema(title = "标题")
    private String title;

    /**
     * 描述
     */
    @Size(max = 20_000, message = "描述长度不能超过20000")
    @Schema(title = "描述")
    private String description;

    /**
     * 类型
     */
    @Schema(title = "类型")
    private ItemType itemType;

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
     * 显式清空跟进时间（避免部分更新把未传字段误判为清空）
     */
    @Schema(title = "是否清空跟进时间")
    private Boolean clearNextFollowUp;

    private Boolean clearProject;

    private Boolean clearDueAt;
}
