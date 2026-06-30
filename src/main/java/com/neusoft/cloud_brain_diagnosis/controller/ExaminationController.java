package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationItem;
import com.neusoft.cloud_brain_diagnosis.service.ExaminationService;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/examination")
@RequiredArgsConstructor
@Tag(name = "检查检验管理", description = "检查申请、结果填写、报告查询")
public class ExaminationController {

    private final ExaminationService examinationService;
    private final ExaminationItemRepository examinationItemRepository;

    /**
     * 医生-开立检查
     */
    @PostMapping("/create")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-开立检查", description = "医生给患者开检查检验申请")
    public Result<Examination> createExamination(@RequestBody Examination examination) {
        return Result.success(examinationService.createExamination(examination, UserContext.getUserId()));
    }

    /**
     * 医生-撤销检查
     */
    @PutMapping("/cancel/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-撤销检查", description = "医生撤销本人开具且未检查的项目")
    public Result<String> cancelExamination(@PathVariable Long id) {
        return Result.success(examinationService.cancelExamination(id, UserContext.getUserId()));
    }

    /**
     * 医生-按挂号查询检查
     */
    @GetMapping("/registration/{registrationId}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-按挂号查询检查", description = "查询当前医生某次挂号下的检查项目")
    public Result<List<Examination>> getByRegistrationId(@PathVariable Long registrationId) {
        return Result.success(examinationService.getByRegistrationId(registrationId, UserContext.getUserId()));
    }

    /**
     * 检查详情（需要登录，患者/医生/检验科均可查看）
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "检查详情", description = "患者、医生、检验科都可以查看")
    public Result<Examination> getDetail(@PathVariable Long id) {
        return Result.success(examinationService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 患者-我的检查报告
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "患者-我的检查报告", description = "分页查询")
    public Result<Page<Examination>> getPatientList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(examinationService.getPatientList(patientId, page, size));
    }

    /**
     * 医生-我开的检查
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-我开的检查", description = "分页查询")
    public Result<Page<Examination>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(examinationService.getDoctorList(doctorId, page, size));
    }

    /**
     * 检验科-检查检验列表
     */
    @GetMapping("/lab/list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "检验科-检查检验列表", description = "分页查询检验科项目，可按状态筛选：待检查/已完成")
    public Result<Page<Examination>> getLabList(
            @RequestParam(defaultValue = "待检查") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getLabList(status, page, size));
    }

    /**
     * 检验科-填写检查结果
     */
    @PutMapping("/update-result")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "检验科-填写检查结果", description = "提交检查结果，状态改为已完成")
    public Result<String> updateResult(
            @RequestParam Long id,
            @RequestParam String result,
            @RequestParam(required = false) String resultImages) {
        return Result.success(examinationService.updateResult(id, result, resultImages));
    }

    @PutMapping(value = "/update-result", consumes = "application/json")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "检验科-填写检查结果", description = "提交检查结果，状态改为已完成")
    public Result<String> updateResult(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String result = body.get("result") == null ? null : String.valueOf(body.get("result"));
        String resultImages = body.get("resultImages") == null ? null : String.valueOf(body.get("resultImages"));
        return Result.success(examinationService.updateResult(id, result, resultImages));
    }

    /**
     * 检查项目列表
     */
    @GetMapping("/item/list")
    @Operation(summary = "检查项目列表", description = "公开接口，支持按类型筛选（检查/检验）")
    public Result<List<ExaminationItem>> getItemList(@RequestParam(required = false) String type) {
        return Result.success(examinationService.getItemList(type));
    }
    /**
     * 实训维护-重置检查项目目录
     */
    @PostMapping("/item/reset-demo-data")
    @Operation(summary = "实训维护-重置检查项目目录", description = "清理重复旧项目，生成一套规范的检查/检验项目目录")
    public Result<Map<String, Object>> resetDemoExaminationItems() {
        List<ExaminationItem> items = new ArrayList<>();
        items.add(buildItem("血常规", "检验", "28.00", "用于评估白细胞、红细胞、血红蛋白、血小板等基础血液指标。"));
        items.add(buildItem("尿常规", "检验", "18.00", "用于筛查尿蛋白、尿糖、尿酮体、红细胞、白细胞等泌尿系统相关指标。"));
        items.add(buildItem("便常规", "检验", "16.00", "用于检查消化道感染、潜血及寄生虫等情况。"));
        items.add(buildItem("肝功能", "检验", "65.00", "用于评估谷丙转氨酶、谷草转氨酶、胆红素和白蛋白等肝脏功能。"));
        items.add(buildItem("肾功能", "检验", "58.00", "用于评估肌酐、尿素氮、尿酸等肾脏代谢指标。"));
        items.add(buildItem("血糖", "检验", "12.00", "用于了解空腹或随机血糖水平，辅助判断糖代谢异常。"));
        items.add(buildItem("血脂四项", "检验", "45.00", "用于评估总胆固醇、甘油三酯、高密度和低密度脂蛋白。"));
        items.add(buildItem("糖化血红蛋白", "检验", "70.00", "用于反映近 2-3 个月平均血糖控制情况。"));
        items.add(buildItem("凝血功能", "检验", "55.00", "用于评估凝血酶原时间、活化部分凝血活酶时间等凝血指标。"));
        items.add(buildItem("电解质", "检验", "35.00", "用于检测钾、钠、氯、钙等电解质水平。"));
        items.add(buildItem("C反应蛋白", "检验", "38.00", "用于辅助判断炎症或感染活动程度。"));
        items.add(buildItem("甲状腺功能", "检验", "120.00", "用于检测 T3、T4、TSH 等甲状腺相关指标。"));
        items.add(buildItem("心肌损伤标志物", "检验", "150.00", "用于评估肌钙蛋白、肌酸激酶同工酶等心肌损伤相关指标。"));
        items.add(buildItem("乙肝两对半", "检验", "90.00", "用于筛查乙肝病毒感染及免疫状态。"));
        items.add(buildItem("血型鉴定", "检验", "25.00", "用于检测 ABO 血型和 Rh 血型。"));
        items.add(buildItem("心电图", "检查", "30.00", "用于评估心律失常、心肌缺血等心脏电生理情况。"));
        items.add(buildItem("胸部X光", "检查", "80.00", "用于观察肺部、胸廓和心影等基础影像情况。"));
        items.add(buildItem("腹部彩超", "检查", "120.00", "用于观察肝胆胰脾肾等腹部脏器情况。"));
        items.add(buildItem("泌尿系彩超", "检查", "110.00", "用于观察肾脏、输尿管、膀胱等泌尿系统结构。"));
        items.add(buildItem("妇科彩超", "检查", "130.00", "用于观察子宫、卵巢及盆腔情况。"));
        items.add(buildItem("颈部血管彩超", "检查", "160.00", "用于评估颈动脉斑块、血流速度和狭窄情况。"));
        items.add(buildItem("CT平扫", "检查", "260.00", "用于对头颅、胸腹部等部位进行断层影像检查。"));
        items.add(buildItem("头颅MRI", "检查", "520.00", "用于观察脑组织、脑血管及颅内病变情况。"));
        items.add(buildItem("胃镜", "检查", "320.00", "用于观察食管、胃和十二指肠黏膜情况。"));
        items.add(buildItem("肺功能", "检查", "180.00", "用于评估肺通气功能，辅助判断哮喘、慢阻肺等疾病。"));

        examinationItemRepository.deleteAll();
        List<ExaminationItem> saved = examinationItemRepository.saveAll(items);
        return Result.success(Map.of("count", saved.size(), "message", "检查项目目录已整理完成"));
    }

    private ExaminationItem buildItem(String name, String type, String price, String description) {
        ExaminationItem item = new ExaminationItem();
        item.setName(name);
        item.setType(type);
        item.setPrice(new BigDecimal(price));
        item.setDescription(description);
        return item;
    }
}
