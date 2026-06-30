package com.neusoft.ai.controller;

import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.AiKnowledgeBase;
import com.neusoft.ai.service.ai.AiKnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "AI知识库管理", description = "管理员管理AI知识库条目")
public class AiKnowledgeBaseController {

    private final AiKnowledgeBaseService knowledgeBaseService;

    @GetMapping("/list")
    @Operation(summary = "知识库列表")
    public Result<Page<AiKnowledgeBase>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(knowledgeBaseService.getKnowledgeList(category, keyword, page, size));
    }

    @PostMapping("/add")
    @Operation(summary = "新增知识库")
    public Result<String> add(@RequestBody AiKnowledgeBase knowledge) {
        return Result.success(knowledgeBaseService.addKnowledge(knowledge));
    }

    @PutMapping("/update")
    @Operation(summary = "修改知识库")
    public Result<String> update(@RequestBody AiKnowledgeBase knowledge) {
        return Result.success(knowledgeBaseService.updateKnowledge(knowledge));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除知识库")
    public Result<String> delete(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.deleteKnowledge(id));
    }

    @PostMapping("/search")
    @Operation(summary = "搜索知识库")
    public Result<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        return Result.success(knowledgeBaseService.searchKnowledge(request.get("question")));
    }
}
