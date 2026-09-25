package com.workos.inbox.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Class InboxAcceptAllReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 批量"先都收下"请求；不带 itemIds 表示处理全部
 */
@Data
public class InboxAcceptAllReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "事项主键集合")
    private List<Long> itemIds;
}
