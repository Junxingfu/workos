package com.workos.project.data.entity;

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

/**
 * @Class ProjectContextPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目现状：稳定信息 + 动态状态
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("project_context")
public class ProjectContextPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /**
     * 目标（稳定）
     */
    private String goal;

    /**
     * 关键人（稳定）
     */
    private String keyPeople;

    /**
     * 关键链接（稳定）
     */
    private String keyLinks;

    /**
     * 当前焦点（动态）
     */
    private String focus;

    /**
     * 当前阻塞
     */
    private String blocker;

    /**
     * 下一步
     */
    private String nextAction;

    /**
     * 进展备注
     */
    private String progressNote;

    private String notesMarkdown;

    @TableField(fill = FieldFill.INSERT)
    private Instant updatedAt;

    @Version
    private Integer version;
}
