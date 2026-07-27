package com.example.mt.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mt.approval.ApprovalContext;
import com.example.mt.approval.ApprovalNode;
import com.example.mt.approval.ChainExecutor;
import com.example.mt.approval.NodeFactory;
import com.example.mt.common.Result;
import com.example.mt.entity.ApprovalTask;
import com.example.mt.mapper.ApprovalTaskMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final NodeFactory nodeFactory;
    private final ChainExecutor chainExecutor;
    private final ApprovalTaskMapper approvalTaskMapper;

    public ApprovalController(NodeFactory nodeFactory, ChainExecutor chainExecutor, ApprovalTaskMapper approvalTaskMapper) {
        this.nodeFactory = nodeFactory;
        this.chainExecutor = chainExecutor;
        this.approvalTaskMapper = approvalTaskMapper;
    }

    /**
     * 责任链 + 工厂演示：立即执行整条审批链，返回执行日志。
     * 不落库，用于直观展示“节点如何被组装并执行”。
     */
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

    /**
     * 提交审批单：落库为 PENDING 状态，作为工作流的起点。
     * 提交时即执行责任链，把节点级执行轨迹写入 execution_log（演示“提交即跑链 + 轨迹落库”）。
     * tenant_id 由 MyBatis-Plus 租户插件在 insert 时自动注入，业务代码零侵入。
     */
    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody ApprovalRequest req) {
        ApprovalTask task = new ApprovalTask();
        task.setTitle(req.getTitle());
        task.setApplicant(req.getApplicant());
        task.setAmount(req.getAmount());
        task.setNodeTypes(String.join(",", req.getNodeTypes()));
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());

        // 提交即执行责任链：记录节点级执行轨迹，历史可回溯
        ApprovalContext ctx = new ApprovalContext();
        ctx.setApplicant(req.getApplicant());
        ctx.setTitle(req.getTitle());
        ctx.setAmount(req.getAmount());
        ApprovalNode head = nodeFactory.build(req.getNodeTypes());
        if (head != null) {
            chainExecutor.execute(head, ctx);
            task.setExecutionLog(ctx.getLog().toString());
        }

        approvalTaskMapper.insert(task);
        return Result.ok(task.getId());
    }

    /**
     * 当前租户的待办列表（status = PENDING）。
     * 查询自动附带 tenant_id 条件，天然隔离。
     */
    @GetMapping("/todos")
    public Result<List<ApprovalTask>> todos() {
        LambdaQueryWrapper<ApprovalTask> w = new LambdaQueryWrapper<>();
        w.eq(ApprovalTask::getStatus, "PENDING").orderByDesc(ApprovalTask::getCreateTime);
        return Result.ok(approvalTaskMapper.selectList(w));
    }

    /**
     * 审批历史，可按状态过滤（PENDING / APPROVED / REJECTED）。
     */
    @GetMapping("/history")
    public Result<List<ApprovalTask>> history(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<ApprovalTask> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(ApprovalTask::getStatus, status);
        }
        w.orderByDesc(ApprovalTask::getCreateTime);
        return Result.ok(approvalTaskMapper.selectList(w));
    }

    /**
     * 审批决策：通过或驳回，更新任务状态，并把最终结论追加进责任链轨迹。
     */
    @PostMapping("/decide")
    public Result<String> decide(@RequestBody DecideRequest req) {
        ApprovalTask task = approvalTaskMapper.selectById(req.getId());
        if (task == null) {
            return Result.fail("任务不存在");
        }
        task.setStatus(req.getDecision());

        // 把人工最终决策写回同一根责任链轨迹，避免“链显示通过、状态却驳回”的矛盾
        String verdict = "APPROVED".equals(req.getDecision()) ? "已通过" : "已驳回";
        String base = task.getExecutionLog() == null ? "" : task.getExecutionLog();
        task.setExecutionLog((base.isEmpty() ? "" : base + " → ") + "最终审批：" + verdict);

        approvalTaskMapper.updateById(task);
        return Result.ok("已" + ("APPROVED".equals(req.getDecision()) ? "通过" : "驳回"));
    }

    @Data
    public static class ApprovalRequest {
        private String applicant;
        private String title;
        private double amount;
        private List<String> nodeTypes;
    }

    @Data
    public static class DecideRequest {
        private Long id;
        private String decision; // APPROVED / REJECTED
    }
}
