package com.example.mt.controller;

import com.example.mt.common.Result;
import com.example.mt.entity.Dept;
import com.example.mt.entity.DeptTreeNode;
import com.example.mt.service.DeptService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping("/tree")
    public Result<List<Dept>> tree() {
        return Result.ok(deptService.tree());
    }

    /**
     * 带统计的部门树接口（重查询，用于压测缓存收益）。
     * 压测时让 JMeter 打这个端点，对比 开/关缓存 的 P95 与命中率。
     */
    @GetMapping("/tree-stats")
    public Result<List<DeptTreeNode>> treeStats() {
        return Result.ok(deptService.treeWithStats());
    }

    /** 部门分页查询（支持按部门名搜索） */
    @GetMapping("/page")
    public Result<IPage<Dept>> page(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String keyword) {
        return Result.ok(deptService.page(page, size, keyword));
    }
}
