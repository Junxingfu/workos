package com.workos.search.model.request;

import com.workos.common.model.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/**
 * @Class SearchReq
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 全局搜索请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SearchReq extends BasePageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关键词
     */
    @Schema(title = "关键词")
    private String q;

    /**
     * 限定类型，逗号分隔；为空表示全部
     */
    @Schema(title = "类型：ITEM,PROJECT,DAILY")
    private String types;
}
