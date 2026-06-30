package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.QualityCheckDetail;
import com.neusoft.ai.service.ai.AiQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/quality-check")
@RequiredArgsConstructor
@Tag(name = "医生质检查看", description = "医生查看质检问题并提交整改")
public class DoctorQualityCheckController {

    private final AiQualityService qualityService;

    @GetMapping("/my-list")
    @Operation(summary = "我的质检问题", description = "医生查看自己的质检问题")
    public Result<Page<QualityCheckDetail>> getMyQualityList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(qualityService.getMyQualityList(doctorId, page, size));
    }

    @PostMapping("/rectify/{id}")
    @Operation(summary = "提交整改", description = "医生提交整改说明")
    public Result<String> rectify(@PathVariable Long id, @RequestParam String remark) {
        Long doctorId = UserContext.getUserId();
        return Result.success(qualityService.rectify(id, remark, doctorId));
    }
}
