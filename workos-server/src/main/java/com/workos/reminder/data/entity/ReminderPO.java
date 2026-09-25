package com.workos.reminder.data.entity;

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

/**
 * @Class ReminderPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 提醒实体：PENDING / DISMISSED / CANCELLED，没有 FIRED
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("reminder")
public class ReminderPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * ITEM / DAILY / STANDALONE
     */
    private String kind;

    private Long itemId;

    private String title;

    private Instant remindAt;

    /**
     * PENDING / DISMISSED / CANCELLED
     */
    private String status;

    /**
     * MVP 不写入，字段保留
     */
    private Instant triggeredAt;

    private Instant acknowledgedAt;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
