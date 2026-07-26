package com.example.mt.controller;

import com.example.mt.approval.ApprovalContext;
import com.example.mt.approval.ApprovalNode;
import com.example.mt.approval.ChainExecutor;
import com.example.mt.approval.NodeFactory;
import com.example.mt.common.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final NodeFactory nodeFactory;
    private final ChainExecutor chainExecutor;

    public ApprovalController(NodeFactory nodeFactory, ChainExecutor chainExecutor) {
        this.nodeFactory = nodeFactory;
        this.chainExecutor = chainExecutor;
    }

    @PostMapping("/run")
    public Result<String> run(@RequestBody ApprovalRequest req) {
        ApprovalContext ctx = new ApprovalContext();
        ctx.setApplicant(req.getApplicant());
        ctx.setTitle(req.getTitle());
        ctx.setAmount(req.getAmount());

        ApprovalNode head = nodeFactory.build(req.getNodeTypes());
        if (head == null) {
            return Result.fail("审批节点为空");
        }
        chainExecutor.execute(head, ctx);
        return Result.ok(ctx.getLog().toString());
    }

    @Data
    public static class ApprovalRequest {
        private String applicant;
        private String title;
        private double amount;
        private List<String> nodeTypes;
    }
}
