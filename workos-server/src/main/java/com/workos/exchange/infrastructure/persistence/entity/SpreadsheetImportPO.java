package com.workos.exchange.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** spreadsheet_import 表的导入成功账本；与事项批量变更在同一事务提交。 */
@Data
@TableName("spreadsheet_import")
public class SpreadsheetImportPO {
  /** 规范化导入内容的 SHA-256 指纹，作为幂等主键。 */
  @TableId(type = IdType.INPUT)
  private String fingerprint;

  /** 导入事务成功提交时保存的 ISO Instant 文本时间。 */
  private String completedAt;

  /** 成功结果的 JSON 快照；重复导入相同指纹时返回这一结果。 */
  private String resultJson;
}
