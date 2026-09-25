package com.workos.today.model.response;

import com.workos.item.model.response.ItemResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Class TodaySectionsResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc Today 六个互斥分区
 */
@Data
public class TodaySectionsResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "① 今日计划")
    private List<ItemResp> plan;

    @Schema(title = "② 逾期")
    private List<ItemResp> overdue;

    @Schema(title = "③ 今日到期")
    private List<ItemResp> today;

    @Schema(title = "④ 待跟进")
    private List<ItemResp> followUp;

    @Schema(title = "⑤ 等待中")
    private List<ItemResp> waiting;

    @Schema(title = "⑥ 待安排（决策队列，上限 5 条）")
    private List<ItemResp> suggested;
}
