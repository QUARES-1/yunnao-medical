package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiKnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "AI知识库管理", description = "管理员管理AI知识库条目")
public class AiKnowledgeBaseController {

    private final AiKnowledgeBaseService knowledgeBaseService;

    @GetMapping("/list")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "知识库列表", description = "从主业务库查询AI知识库条目，不依赖AI微服务在线状态")
    public Result<Map<String, Object>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<AiKnowledgeBase> pageData = knowledgeBaseService.getKnowledgeList(category, keyword, page, size);
        return Result.success(toPageMap(pageData));
    }

    @PostMapping("/add")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "新增知识")
    public Result<String> add(@RequestBody Map<String, Object> knowledge) {
        return Result.success(knowledgeBaseService.addKnowledge(toEntity(knowledge)));
    }

    @PutMapping("/update")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "修改知识")
    public Result<String> update(@RequestBody Map<String, Object> knowledge) {
        return Result.success(knowledgeBaseService.updateKnowledge(toEntity(knowledge)));
    }

    @DeleteMapping("/delete/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "删除知识")
    public Result<String> delete(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.deleteKnowledge(id));
    }

    private AiKnowledgeBase toEntity(Map<String, Object> map) {
        AiKnowledgeBase item = new AiKnowledgeBase();
        item.setId(toLong(map.get("id")));
        item.setCategory(toStringValue(map.get("category")));
        item.setQuestion(toStringValue(map.get("question")));
        item.setAnswer(toStringValue(map.get("answer")));
        item.setKeywords(toStringValue(map.get("keywords")));
        item.setSort(toInteger(map.get("sort")));
        item.setStatus(toInteger(map.get("status")));
        return item;
    }

    private Map<String, Object> toPageMap(Page<?> pageData) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", pageData.getContent());
        data.put("records", pageData.getContent());
        data.put("totalElements", pageData.getTotalElements());
        data.put("total", pageData.getTotalElements());
        data.put("totalPages", pageData.getTotalPages());
        data.put("number", pageData.getNumber());
        data.put("page", pageData.getNumber() + 1);
        data.put("size", pageData.getSize());
        return data;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
