package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.service.StaffAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffAccountController {
    private final StaffAccountService staffAccountService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        return Result.success(staffAccountService.login(body.get("username"), body.get("password"), body.get("role")));
    }

    @GetMapping("/info")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.LAB})
    public Result<StaffAccount> info(@RequestHeader("Authorization") String authorization) {
        return Result.success(staffAccountService.getInfo(userId(authorization)));
    }

    @PutMapping("/change-password")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.LAB})
    public Result<String> changePassword(@RequestHeader("Authorization") String authorization,
                                         @RequestBody Map<String, String> body) {
        return Result.success(staffAccountService.changePassword(userId(authorization), body.get("oldPassword"), body.get("newPassword")));
    }

    @PostMapping("/create")
    @RequireLogin(RoleEnum.ADMIN)
    public Result<StaffAccount> create(@RequestBody StaffAccount account) {
        return Result.success(staffAccountService.create(account));
    }

    private Long userId(String authorization) {
        String token = authorization == null ? "" : authorization.replaceFirst("(?i)^Bearer\\s+", "");
        return jwtUtil.getUserIdFromToken(token);
    }
}
