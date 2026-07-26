package com.example.mt.approval;

import org.springframework.stereotype.Component;

/**
 * 责任链执行器：从 head 节点依次推进。
 */
@Component
public class ChainExecutor {

    public void execute(ApprovalNode head, ApprovalContext ctx) {
        ApprovalNode n = head;
        while (n != null) {
            n.handle(ctx);
            n = n.next();
        }
    }
}
