package com.workos.daily.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @Class DailyPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报实体：generatedContent 是快照，不覆盖 content
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("daily")
public class DailyPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate dailyDate;

    /**
     * 用户表达（事实真源是 Item / WorkLog）
     */
    private String content;

    /**
     * 自动生成快照
     */
    private String generatedContent;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
