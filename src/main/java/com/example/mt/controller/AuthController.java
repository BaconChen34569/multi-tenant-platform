package com.example.mt.controller;

import com.example.mt.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 演示用登录：真实环境应校验密码并签发 JWT（JWT 中携带 tenantId）。
     * 前端随后通过 X-Tenant-Id 请求头切换租户。
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        return Result.ok("login-ok");
    }
}
