package com.workos.attention.domain;

import java.util.List;
import java.util.Map;

/** 关注上下文的输入事实及回执端口，不泄露任务 PO。 */
public interface AttentionRepository {
  record TaskFact(AttentionPolicy.Facts facts, String title) {}

  /** 提供仍活动任务的只读事实，供关注策略计算，不返回事项聚合。 */
  List<TaskFact> activeTasks();

  /** 按稳定关注键读取回执，缺失键代表尚未响应。 */
  Map<String, AttentionReceipt> receipts();

  /** 按稳定键原子写入回执；重复响应不会产生第二条记录。 */
  void save(AttentionReceipt receipt);
}
