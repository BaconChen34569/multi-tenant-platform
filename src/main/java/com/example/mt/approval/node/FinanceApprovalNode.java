package com.example.mt.approval.node;

import com.example.mt.approval.AbstractApprovalNode;
import com.example.mt.approval.ApprovalContext;
import org.springframework.stereotype.Component;

@Component
public class FinanceApprovalNode extends AbstractApprovalNode {

    private static final double THRESHOLD = 5000;

    @Override
    public void handle(ApprovalContext ctx) {
        if (ctx.getAmount() > THRESHOLD) {
            ctx.append("财务审批通过(金额 " + ctx.getAmount() + ")");
        } else {
            ctx.append("财务免审(金额 " + ctx.getAmount() + " < 阈值)");
        }
        passNext(ctx);
    }
}
