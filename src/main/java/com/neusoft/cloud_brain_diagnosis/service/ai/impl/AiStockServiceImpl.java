package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.repository.StockForecastRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiStockServiceImpl implements AiStockService {

    private final StockForecastRepository forecastRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateStockForecast(String forecastType, String forecastPeriod) {
        if (forecastPeriod == null || forecastPeriod.isBlank()) {
            forecastPeriod = LocalDate.now().plusMonths(1).toString().substring(0, 7);
        }

        List<Medicine> medicines = medicineRepository.findAll();
        List<Prescription> prescriptions = prescriptionRepository.findAll().stream()
                .filter(item -> !"已撤销".equals(item.getStatus()))
                .toList();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Integer> recent30 = consumption(prescriptions, now.minusDays(30), now);
        Map<Long, Integer> previous30 = consumption(prescriptions, now.minusDays(60), now.minusDays(30));
        Map<Long, Integer> recent90 = consumption(prescriptions, now.minusDays(90), now);

        long currentVisits = prescriptions.stream().filter(p -> inRange(p.getCreateTime(), now.minusDays(30), now)).count();
        long previousVisits = prescriptions.stream().filter(p -> inRange(p.getCreateTime(), now.minusDays(60), now.minusDays(30))).count();
        double visitFactor = previousVisits == 0 ? 1.0 : clamp((double) currentVisits / previousVisits, 0.85, 1.25);

        JSONArray forecastData = new JSONArray();
        BigDecimal totalForecastAmount = BigDecimal.ZERO;
        BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
        int targetMonth = Integer.parseInt(forecastPeriod.substring(5, 7));

        for (Medicine medicine : medicines) {
            int used30 = recent30.getOrDefault(medicine.getId(), 0);
            int usedPrev30 = previous30.getOrDefault(medicine.getId(), 0);
            int used90 = recent90.getOrDefault(medicine.getId(), used30);
            double trend = usedPrev30 == 0 ? (used30 > 0 ? 1.12 : 1.0)
                    : clamp((double) used30 / usedPrev30, 0.70, 1.50);
            double season = seasonFactor(medicine, targetMonth);
            double monthlyBase = used90 > 0 ? used90 / 3.0 : Math.max(4, medicine.getStock() * 0.12);
            int forecastConsume = (int) Math.ceil(monthlyBase * trend * season * visitFactor);
            int safetyStock = Math.max(5, (int) Math.ceil(forecastConsume * 0.20));
            int stock = Optional.ofNullable(medicine.getStock()).orElse(0);
            int suggestPurchase = Math.max(0, forecastConsume + safetyStock - stock);
            double dailyAverage = used90 / 90.0;
            int coverageDays = dailyAverage <= 0.01 ? 999 : (int) Math.floor(stock / dailyAverage);
            String riskLevel = suggestPurchase > 0 && coverageDays < 15 ? "高风险"
                    : suggestPurchase > 0 ? "需补货"
                    : stock > Math.max(20, forecastConsume * 3) ? "可能积压" : "库存合理";

            JSONObject row = new JSONObject();
            row.set("medicineId", medicine.getId());
            row.set("name", medicine.getName());
            row.set("specification", medicine.getSpecification());
            row.set("categoryName", medicine.getCategoryName());
            row.set("currentStock", stock);
            row.set("history30", used30);
            row.set("history90", used90);
            row.set("dailyAverage", round(dailyAverage));
            row.set("trendRate", round((trend - 1) * 100));
            row.set("seasonFactor", round(season));
            row.set("visitFactor", round(visitFactor));
            row.set("forecastConsume", forecastConsume);
            row.set("safetyStock", safetyStock);
            row.set("suggestPurchase", suggestPurchase);
            row.set("stockCoverageDays", coverageDays);
            row.set("riskLevel", riskLevel);
            row.set("unit", medicine.getUnit());
            row.set("reason", buildReason(trend, season, visitFactor, riskLevel));
            forecastData.add(row);

            BigDecimal price = Optional.ofNullable(medicine.getPrice()).orElse(BigDecimal.ZERO);
            totalForecastAmount = totalForecastAmount.add(price.multiply(BigDecimal.valueOf(forecastConsume)));
            totalPurchaseAmount = totalPurchaseAmount.add(price.multiply(BigDecimal.valueOf(suggestPurchase)));
        }

        String prompt = "预测周期：" + forecastPeriod
                + "。以下是系统按历史处方、库存、季节和就诊趋势计算的逐药数据："
                + forecastData
                + "。请输出采购建议和影响因素，不能虚构药品或数字。";
        String systemPrompt = "你是医院药房库存预测专家。请基于系统提供的真实数据进行AI库存分析，"
                + "以严格JSON返回：{\"purchaseSuggestions\":[\"...\"],\"factors\":[\"...\"]}。"
                + "建议需覆盖缺货风险、积压风险、分批采购和复核要求。";
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        JSONArray purchaseSuggestions = new JSONArray();
        JSONArray factors = new JSONArray();
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            if (json.getJSONArray("purchaseSuggestions") != null) purchaseSuggestions = json.getJSONArray("purchaseSuggestions");
            if (json.getJSONArray("factors") != null) factors = json.getJSONArray("factors");
        } catch (Exception ignored) {
        }
        if (purchaseSuggestions.isEmpty()) {
            purchaseSuggestions.add("优先采购高风险与需补货药品，建议分两批到货并在一周后复核实际消耗。");
            purchaseSuggestions.add("对可能积压药品暂停常规补货，并结合近效期批次安排院内调剂。");
        }
        factors.add("近30天与近90天处方消耗");
        factors.add("当前库存与20%安全库存");
        factors.add("季节性疾病和近期处方量趋势");

        StockForecast forecast = new StockForecast();
        forecast.setForecastType(forecastType == null ? "monthly" : forecastType);
        forecast.setForecastPeriod(forecastPeriod);
        forecast.setForecastData(forecastData.toString());
        forecast.setPurchaseSuggestions(purchaseSuggestions.toString());
        forecast.setFactors(factors.toString());
        forecast.setTotalForecastAmount(totalForecastAmount.setScale(2, RoundingMode.HALF_UP));
        forecast.setTotalPurchaseAmount(totalPurchaseAmount.setScale(2, RoundingMode.HALF_UP));
        forecast.setRawResponse(aiResponse);
        forecastRepository.save(forecast);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", forecast.getId());
        result.put("forecastType", forecast.getForecastType());
        result.put("forecastPeriod", forecastPeriod);
        result.put("medicineCount", medicines.size());
        result.put("createTime", forecast.getCreateTime());
        return result;
    }

    private Map<Long, Integer> consumption(List<Prescription> prescriptions, LocalDateTime start, LocalDateTime end) {
        Map<Long, Integer> result = new HashMap<>();
        for (Prescription prescription : prescriptions) {
            if (!inRange(prescription.getCreateTime(), start, end) || prescription.getDrugs() == null) continue;
            try {
                for (Object value : JSONUtil.parseArray(prescription.getDrugs())) {
                    JSONObject drug = JSONUtil.parseObj(value);
                    Long medicineId = drug.getLong("medicineId");
                    if (medicineId == null) medicineId = drug.getLong("id");
                    int quantity = Optional.ofNullable(drug.getInt("quantity")).orElse(0);
                    if (medicineId != null && quantity > 0) result.merge(medicineId, quantity, Integer::sum);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private double seasonFactor(Medicine medicine, int month) {
        String text = (Optional.ofNullable(medicine.getName()).orElse("") + Optional.ofNullable(medicine.getCategoryName()).orElse(""));
        boolean winter = month <= 2 || month >= 11;
        boolean summer = month >= 6 && month <= 8;
        if (winter && containsAny(text, "感冒", "退热", "止咳", "抗病毒", "呼吸", "阿莫西林")) return 1.30;
        if (summer && containsAny(text, "胃肠", "止泻", "蒙脱石", "益生菌", "消化")) return 1.25;
        if (summer && containsAny(text, "感冒", "退热", "止咳")) return 0.92;
        return 1.0;
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private String buildReason(double trend, double season, double visit, String risk) {
        List<String> reasons = new ArrayList<>();
        if (trend > 1.08) reasons.add("近期消耗上升");
        if (trend < 0.92) reasons.add("近期消耗下降");
        if (season > 1.05) reasons.add("进入季节性需求高峰");
        if (season < 0.95) reasons.add("季节需求回落");
        if (visit > 1.05) reasons.add("近期处方量增加");
        if (reasons.isEmpty()) reasons.add("需求总体平稳");
        reasons.add(risk);
        return String.join("，", reasons);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public StockForecast getForecastDetail(Long id) {
        return forecastRepository.findById(id).orElse(null);
    }

    @Override
    public Page<StockForecast> getForecastList(String forecastType, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (forecastType != null && !forecastType.isEmpty()) {
            return forecastRepository.findByForecastTypeOrderByCreateTimeDesc(forecastType, pageRequest);
        }
        return forecastRepository.findByOrderByCreateTimeDesc(pageRequest);
    }
}
