package com.example.mt.approval.node;

import com.example.mt.approval.AbstractApprovalNode;
import com.example.mt.approval.ApprovalContext;
import org.springframework.stereotype.Component;

@Component
public class HrApprovalNode extends AbstractApprovalNode {
    @Override
    public void handle(ApprovalContext ctx) {
        ctx.append("人事环节已处理: " + ctx.getTitle());
        passNext(ctx);
    }
}
