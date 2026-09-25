package com.workos.inbox.service;

import com.workos.inbox.model.request.InboxAcceptAllReq;
import com.workos.inbox.model.response.InboxAcceptAllResp;
import com.workos.inbox.model.response.InboxResp;

/**
 * @Class InboxService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 待整理队列业务接口
 */
public interface InboxService {

    /**
     * @Method inbox
     * @Return InboxResp
     * @Desc 待整理列表
     */
    InboxResp inbox();

    /**
     * @Method acceptAll
     * @Param [req]
     * @Return InboxAcceptAllResp
     * @Desc 先都收下：status = TODO / priority = P2
     */
    InboxAcceptAllResp acceptAll(InboxAcceptAllReq req);
}
