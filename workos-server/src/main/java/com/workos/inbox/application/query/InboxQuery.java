package com.workos.inbox.application.query;

import com.workos.inbox.application.readmodel.InboxResp;

/** Inbox 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface InboxQuery {
  /** 读取待整理事项及提示信息。 */
  InboxResp inbox();
}
