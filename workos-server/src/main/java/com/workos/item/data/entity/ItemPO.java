package com.workos.item.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.workos.item.enums.ItemPriority;
import com.workos.item.enums.ItemSource;
import com.workos.item.enums.ItemStatus;
import com.workos.item.enums.ItemType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class ItemPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项实体（Follow-up 是能力，Waiting 是状态，都不是类型）
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("item")
public class ItemPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目；可以为空（未分类）
     */
    private Long projectId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述（Markdown，可包含 checklist）
     */
    private String description;

    /**
     * 类型：TASK / NOTE
     */
    private ItemType itemType;

    /**
     * 状态
     */
    private ItemStatus status;

    /**
     * 优先级
     */
    private ItemPriority priority;

    /**
     * 我计划哪天做
     */
    private LocalDate plannedDate;

    /**
     * 今日计划顺序
     */
    private Integer planRank;

    /**
     * 最晚什么时候完成
     */
    private Instant dueAt;

    /**
     * 我什么时候再次主动推动（非空即表示有跟进能力）
     */
    private Instant nextFollowUpAt;

    /**
     * 在等谁
     */
    private String waitingFor;

    /**
     * 什么时候开始等
     */
    private Instant waitingSince;

    /**
     * 对方预计什么时候给
     */
    private Instant waitingExpectedAt;

    /**
     * 相关人（自由文本，只有一个字段）
     */
    private String relatedPerson;

    /**
     * 完成时间
     */
    private Instant completedAt;

    /**
     * 取消时间
     */
    private Instant cancelledAt;

    /**
     * 来源
     */
    private ItemSource source;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;
}
