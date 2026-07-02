package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiChatFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI就医辅助", description = "智能客服、健康顾问、用药咨询等")
public class AiChatController {

    private final AiChatFeignClient chatFeignClient;
    private final AiChatService chatService;

    /**
     * 发送问题（智能客服）
     */
    @PostMapping("/chat")
    @Operation(summary = "智能客服", description = "公开接口，患者提问，返回AI回答")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        String question = String.valueOf(request.getOrDefault("question", ""));
        String sessionId = String.valueOf(request.getOrDefault("sessionId", "guest-" + UUID.randomUUID()));
        if (question.trim().isEmpty()) {
            return Result.error("请输入要咨询的问题");
        }
        return Result.success(chatService.chat(question, sessionId, null, "guest"));
    }

    /**
     * 智能客服流式回答（游客可用）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "智能客服流式回答", description = "SSE逐字输出AI回答，适用于小程序健康顾问未登录场景")
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                String question = String.valueOf(request.getOrDefault("question", ""));
                String sessionId = String.valueOf(request.getOrDefault("sessionId", "guest-" + UUID.randomUUID()));
                if (question.trim().isEmpty()) {
                    sendError(emitter, "请输入要咨询的问题");
                    return;
                }
                Map<String, Object> result = chatService.chat(question, sessionId, null, "guest");
                streamAnswer(emitter, result);
            } catch (Exception e) {
                sendError(emitter, e.getMessage() == null ? "AI流式回答失败" : e.getMessage());
            }
        });
        return emitter;
    }

    /**
     * 问答历史（患者）
     */
    @GetMapping("/chat/history")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "问答历史", description = "患者自己的问答历史记录")
    public Result<Map<String, Object>> getChatHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return chatFeignClient.getChatHistory(page, size);
    }

    /**
     * 反馈评价
     */
    @PostMapping("/chat/feedback/{id}")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "评价回答", description = "对AI回答点赞/点踩")
    public Result<String> feedback(@PathVariable Long id, @RequestParam String feedback) {
        return chatFeignClient.feedback(id, feedback);
    }

    /**
     * 健康咨询
     */
    @PostMapping("/health-consult")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "健康顾问", description = "患者健康问题咨询")
    public Result<Map<String, Object>> healthConsult(@RequestBody Map<String, Object> request) {
        String question = String.valueOf(request.getOrDefault("question", ""));
        Boolean includeHistory = Boolean.valueOf(String.valueOf(request.getOrDefault("includeHistory", "true")));
        if (question.trim().isEmpty()) {
            return Result.error("请输入要咨询的问题");
        }
        return Result.success(chatService.healthConsult(question, UserContext.getUserId(), includeHistory));
    }

    /**
     * 健康顾问流式回答（患者登录后使用）
     */
    @PostMapping(value = "/health-consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "健康顾问流式回答", description = "SSE逐字输出AI健康建议，可结合患者历史信息")
    public SseEmitter healthConsultStream(@RequestBody Map<String, Object> request) {
        Long patientId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                String question = String.valueOf(request.getOrDefault("question", ""));
                Boolean includeHistory = Boolean.valueOf(String.valueOf(request.getOrDefault("includeHistory", "true")));
                if (question.trim().isEmpty()) {
                    sendError(emitter, "请输入要咨询的问题");
                    return;
                }
                Map<String, Object> result = chatService.healthConsult(question, patientId, includeHistory);
                streamAnswer(emitter, result);
            } catch (Exception e) {
                sendError(emitter, e.getMessage() == null ? "AI健康顾问流式回答失败" : e.getMessage());
            }
        });
        return emitter;
    }

    /**
     * 咨询历史（患者）
     */
    @GetMapping("/consult/history")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "咨询历史", description = "患者健康咨询历史")
    public Result<Map<String, Object>> getConsultHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return chatFeignClient.getConsultHistory(page, size);
    }

    private void streamAnswer(SseEmitter emitter, Map<String, Object> result) throws IOException, InterruptedException {
        String answer = String.valueOf(result.getOrDefault("answer", "暂时没有生成有效回答，请稍后再试。"));
        for (int i = 0; i < answer.length(); i++) {
            emitter.send(SseEmitter.event().name("delta").data(String.valueOf(answer.charAt(i))));
            Thread.sleep(24L);
        }
        Map<String, Object> meta = new HashMap<>(result);
        meta.remove("answer");
        emitter.send(SseEmitter.event().name("done").data(JSONUtil.toJsonStr(meta)));
        emitter.complete();
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }
}
