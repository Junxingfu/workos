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
 * @Class ProjectPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目实体
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("project")
public class ProjectPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目编码
     */
    private String code;

    private String description;

    /**
     * ACTIVE / ARCHIVED
     */
    private String status;

    private String color;

    private Integer sortOrder;

    private Instant archivedAt;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
