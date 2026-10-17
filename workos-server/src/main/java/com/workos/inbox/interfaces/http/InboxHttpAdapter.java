package com.workos.inbox.interfaces.http;

import com.workos.inbox.application.query.InboxQuery;
import com.workos.inbox.application.readmodel.InboxAcceptAllResp;
import com.workos.inbox.application.readmodel.InboxResp;
import com.workos.inbox.interfaces.request.InboxAcceptAllReq;
import com.workos.item.application.ItemEditingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Inbox 接口适配器：转换输入并调用用例，读取由查询投影负责。 */
@Component
@RequiredArgsConstructor
public class InboxHttpAdapter {
  private final InboxQuery queries;
  private final ItemEditingService editing;

  /** 待整理列表 */
  public InboxResp inbox() {
    return queries.inbox();
  }

  /** 先都收下：status = TODO / priority = P2 */
  public InboxAcceptAllResp acceptAll(InboxAcceptAllReq req) {
    InboxAcceptAllResp response = new InboxAcceptAllResp();
    response.setAccepted(editing.accept(req == null ? null : req.getItemIds()));
    return response;
  }
}
