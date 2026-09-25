package com.workos.worklog.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class WorkLogPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录：只有人工记录，没有 source
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("work_log")
public class WorkLogPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long itemId;

    /**
     * 属于哪一天（受 day_boundary_hour 影响）
     */
    private LocalDate workDate;

    /**
     * 事实发生的时间点
     */
    private Instant occurredAt;

    /**
     * 记录内容
     */
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
