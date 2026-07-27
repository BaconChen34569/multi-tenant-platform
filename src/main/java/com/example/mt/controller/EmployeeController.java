package com.example.mt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mt.common.Result;
import com.example.mt.entity.Employee;
import com.example.mt.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 员工分页查询，支持按姓名关键字模糊搜索。
     * 返回 MyBatis-Plus 的 IPage（records + total + 当前页），前端据此渲染分页器。
     * 底层命中 Redis 缓存，翻页不再重复查库。
     */
    @GetMapping("/list")
    public Result<IPage<Employee>> list(@RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(employeeService.page(keyword, page, pageSize));
    }

    @PostMapping
    public Result<Long> add(@RequestBody Employee emp) {
        return Result.ok(employeeService.add(emp));
    }

    @PutMapping
    public Result<String> update(@RequestBody Employee emp) {
        employeeService.update(emp);
        return Result.ok("updated");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.ok("deleted");
    }
}
