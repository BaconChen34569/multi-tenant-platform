package com.example.mt.approval;

import com.example.mt.approval.node.FinanceApprovalNode;
import com.example.mt.approval.node.HrApprovalNode;
import com.example.mt.approval.node.LeaderApprovalNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批节点工厂：按节点类型列表动态装配责任链。
 * 新业务只需调整传入的 nodeTypes（如 ["LEADER","HR"] 或 ["LEADER","FINANCE"]），
 * 无需改动硬编码流程 —— 对应简历“策略节点的动态生成与编排”。
 */
@Component
public class NodeFactory {

    private final Map<String, ApprovalNode> registry = new HashMap<>();

    public NodeFactory(LeaderApprovalNode leader, HrApprovalNode hr, FinanceApprovalNode finance) {
        registry.put("LEADER", leader);
        registry.put("HR", hr);
        registry.put("FINANCE", finance);
    }

    public ApprovalNode build(List<String> types) {
        ApprovalNode head = null;
        ApprovalNode cur = null;
        for (String t : types) {
            ApprovalNode node = registry.get(t.toUpperCase());
            if (node == null) {
                continue;
            }
            if (head == null) {
                head = node;
                cur = node;
            } else {
                cur.setNext(node);
                cur = node;
            }
        }
        return head;
    }
}
