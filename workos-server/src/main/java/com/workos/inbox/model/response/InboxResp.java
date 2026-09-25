package com.workos.inbox.model.response;

import com.workos.item.model.response.ItemResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Class InboxResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 待整理队列响应
 */
@Data
public class InboxResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<ItemResp> records;

    private Long total;

    @Schema(title = "最老一条已压了多少天")
    private Long oldestDays;
}
