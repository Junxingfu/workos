package com.workos.settings.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** settings 单行配置的存储映射；校验由 WorkspaceSettings 负责。 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("settings")
public class SettingsPO implements Serializable {

  /** Java 序列化版本，与聚合的乐观锁版本无关。 */
  @Serial private static final long serialVersionUID = 1L;

  /** 工作区单例标识，固定为 1。 */
  @TableId private Short id;

  /** 乐观锁版本；更新以旧版本为条件，成功后递增，防止覆盖并发修改。 */
  @Version private Integer version;

  /** IANA 时区标识，用于统一 Today、工作日与日报日期的计算。 */
  private String timezone;

  /** 日期展示格式偏好；不改变数据库日期类型。 */
  private String dateFormat;

  /** 每周起始日偏好，MONDAY 或 SUNDAY。 */
  private String weekStart;

  /** 业务日切换小时，允许 0–23；当前界面未开放编辑，默认 0。 */
  private Short dayBoundaryHour;

  /** 晨间摘要时间偏好，HH:mm；目前仅保存配置，尚未接入调度。 */
  private String morningBriefTime;

  /** 日终回顾时间偏好，HH:mm；目前仅保存配置，尚未接入调度。 */
  private String eodReviewTime;

  /** 桌面通知偏好；保存配置不代表已接入系统通知调度。 */
  private Boolean desktopNotification;

  /** 快捷记录键位偏好；系统热键是否注册由桌面适配器决定。 */
  private String quickCaptureShortcut;

  /** 最近一次保存的时间点。 */
  private Instant updatedAt;
}
