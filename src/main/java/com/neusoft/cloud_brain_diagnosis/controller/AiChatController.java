package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI就医辅助", description = "智能客服、健康顾问、用药咨询等")
public class AiChatController {

    private final AiChatService chatService;

    /**
     * 发送问题（智能客服）
     */
    @PostMapping("/chat")
    @Operation(summary = "智能客服", description = "公开接口，患者提问，返回AI回答")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        String sessionId = (String) request.get("sessionId");
        return Result.success(chatService.chat(question, sessionId, null, null));
    }

    /**
     * 问答历史（患者）
     */
    @GetMapping("/chat/history")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "问答历史", description = "患者自己的问答历史记录")
    public Result<Page<AiChatRecord>> getChatHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(chatService.getChatHistory(userId, "patient", page, size));
    }

    /**
     * 反馈评价
     */
    @PostMapping("/chat/feedback/{id}")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "评价回答", description = "对AI回答点赞/点踩")
    public Result<String> feedback(@PathVariable Long id, @RequestParam String feedback) {
        return Result.success(chatService.feedback(id, feedback));
    }

    /**
     * 健康咨询
     */
    @PostMapping("/health-consult")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "健康顾问", description = "患者健康问题咨询")
    public Result<Map<String, Object>> healthConsult(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        Boolean includeHistory = (Boolean) request.getOrDefault("includeHistory", false);
        Long patientId = UserContext.getUserId();
        return Result.success(chatService.healthConsult(question, patientId, includeHistory));
    }

    /**
     * 咨询历史（患者）
     */
    @GetMapping("/consult/history")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "咨询历史", description = "患者健康咨询历史")
    public Result<Page<AiChatRecord>> getConsultHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(chatService.getChatHistory(userId, "patient", page, size));
    }
}
