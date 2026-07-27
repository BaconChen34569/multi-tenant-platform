package com.example.mt.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mt.common.Result;
import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.ApprovalTask;
import com.example.mt.mapper.ApprovalTaskMapper;
import com.example.mt.mapper.DeptMapper;
import com.example.mt.mapper.EmployeeMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DeptMapper deptMapper;
    private final EmployeeMapper employeeMapper;
    private final ApprovalTaskMapper approvalTaskMapper;

    public DashboardController(DeptMapper deptMapper, EmployeeMapper employeeMapper, ApprovalTaskMapper approvalTaskMapper) {
        this.deptMapper = deptMapper;
        this.employeeMapper = employeeMapper;
        this.approvalTaskMapper = approvalTaskMapper;
    }

    /**
     * 平台概览统计。所有 count 走租户插件自动注入的 tenant_id 条件，
     * 因此返回的是“当前租户”的数据，天然隔离。
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        long deptCount = deptMapper.selectCount(null);
        long employeeCount = employeeMapper.selectCount(null);
        long pendingCount = approvalTaskMapper.selectCount(
                new LambdaQueryWrapper<ApprovalTask>().eq(ApprovalTask::getStatus, "PENDING"));
        long approvedCount = approvalTaskMapper.selectCount(
                new LambdaQueryWrapper<ApprovalTask>().eq(ApprovalTask::getStatus, "APPROVED"));
        Map<String, Object> m = Map.of(
                "deptCount", deptCount,
                "employeeCount", employeeCount,
                "pendingCount", pendingCount,
                "approvedCount", approvedCount
        );
        return Result.ok(m);
    }

    /**
     * 返回后端解析到的当前租户，用于直观验证“多租户隔离”生效。
     */
    @GetMapping("/tenant")
    public Result<Long> currentTenant() {
        return Result.ok(TenantContext.get());
    }
}
