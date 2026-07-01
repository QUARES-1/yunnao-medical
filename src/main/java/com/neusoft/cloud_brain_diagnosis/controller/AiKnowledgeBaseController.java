package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "AI知识库管理", description = "管理员管理AI知识库条目")
public class AiKnowledgeBaseController {

    private final AiOtherFeignClient otherFeignClient;

    /**
     * 知识库列表
     */
    @GetMapping("/list")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "知识库列表", description = "管理员查看知识库分页列表")
    public Result<Map<String, Object>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return otherFeignClient.getKnowledgeList(category, keyword, page, size);
    }

    /**
     * 新增知识库
     */
    @PostMapping("/add")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "新增知识库", description = "管理员新增知识库条目")
    public Result<String> add(@RequestBody Map<String, Object> knowledge) {
        return otherFeignClient.addKnowledge(knowledge);
    }

    /**
     * 修改知识库
     */
    @PutMapping("/update")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "修改知识库", description = "管理员修改知识库条目")
    public Result<String> update(@RequestBody Map<String, Object> knowledge) {
        return otherFeignClient.updateKnowledge(knowledge);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/delete/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "删除知识库", description = "管理员删除知识库条目")
    public Result<String> delete(@PathVariable Long id) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        return otherFeignClient.deleteKnowledge(id);
    }
}
