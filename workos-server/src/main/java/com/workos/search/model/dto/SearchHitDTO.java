package com.workos.search.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class SearchHitDTO
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 搜索命中项（仍带原始文本，高亮由服务层生成）
 */
@Data
public class SearchHitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ITEM / PROJECT / DAILY
     */
    private String hitType;

    private Long id;

    private String title;

    private String detail;

    private Long projectId;

    private String projectName;

    private Instant updatedAt;
}
