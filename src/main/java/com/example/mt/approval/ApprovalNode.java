package com.example.mt.approval;

/**
 * 审批节点：责任链模式中的一环。
 */
public interface ApprovalNode {
    void setNext(ApprovalNode next);
    ApprovalNode next();
    void handle(ApprovalContext ctx);
}
