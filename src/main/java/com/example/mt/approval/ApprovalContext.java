package com.example.mt.approval;

import lombok.Data;

/**
 * 审批上下文：在责任链各节点间传递数据。
 */
@Data
public class ApprovalContext {
    private String applicant;
    private String title;
    private double amount;                       // 用于财务节点判断
    private StringBuilder log = new StringBuilder();

    public void append(String s) {
        log.append(s).append("; ");
    }
}
