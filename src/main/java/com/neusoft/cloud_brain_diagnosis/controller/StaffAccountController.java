package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.service.StaffAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffAccountController {
    private final StaffAccountService service;

    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        return Result.success(service.login(body.get("username"), body.get("password"), body.get("role")));
    }

    @GetMapping("/info")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.LAB})
    public Result<StaffAccount> info() {
        return Result.success(service.getInfo(UserContext.getUserId()));
    }

    @PutMapping("/change-password")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.LAB})
    public Result<String> changePassword(@RequestBody Map<String, String> body) {
        return Result.success(service.changePassword(UserContext.getUserId(), body.get("oldPassword"), body.get("newPassword")));
    }

    @PostMapping("/create")
    @RequireLogin(RoleEnum.ADMIN)
    public Result<StaffAccount> create(@RequestBody StaffAccount account) {
        StaffAccount saved = service.create(account);
        saved.setPassword(null);
        return Result.success(saved);
    }
}
