package com.example.mt.approval;

/**
 * 审批节点基类：统一维护 next 指针，并提供向下传递的模板方法。
 */
public abstract class AbstractApprovalNode implements ApprovalNode {

    private ApprovalNode next;

    @Override
    public void setNext(ApprovalNode next) {
        this.next = next;
    }

    @Override
    public ApprovalNode next() {
        return next;
    }

    /** 处理完后交给下一个节点（责任链推进） */
    protected void passNext(ApprovalContext ctx) {
        if (next != null) {
            next.handle(ctx);
        }
    }
}
