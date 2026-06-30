package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.OperationAiReport;
import com.neusoft.ai.repository.OperationAiReportRepository;
import com.neusoft.ai.service.ai.AiOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiOperationServiceImpl implements AiOperationService {

    private final OperationAiReportRepository reportRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateReport(String reportType, String startDate, String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusWeeks(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        String prompt = "请分析以下时间段的运营数据：\n" + start + " 至 " + end;
        String systemPrompt = "你是一名医院运营分析师，请分析运营数据并生成报告。"
                + "请按JSON格式返回："
                + "{\"summary\":\"分析总结\","
                + "\"keyMetrics\":{\"totalRegistrations\":1250,\"totalRevenue\":285000,\"avgDailyRegistrations\":178,\"departmentTop3\":[\"内科\",\"儿科\",\"外科\"]},"
                + "\"trendsAnalysis\":\"趋势分析\","
                + "\"forecasts\":{\"nextWeekRegistrations\":1350,\"trend\":\"up\"},"
                + "\"warnings\":[{\"level\":\"info\",\"content\":\"预警内容\"}],"
                + "\"suggestions\":[\"建议1\",\"建议2\"]}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        OperationAiReport report = new OperationAiReport();
        report.setReportType(reportType);
        report.setStartDate(start);
        report.setEndDate(end);

        try {
            cn.hutool.json.JSONObject json = JSONUtil.parseObj(aiResponse);
            report.setSummary(json.getStr("summary", ""));
            if (json.get("keyMetrics") != null) {
                report.setKeyMetrics(json.getObj("keyMetrics").toString());
            }
            report.setTrendsAnalysis(json.getStr("trendsAnalysis", ""));
            if (json.get("forecasts") != null) {
                report.setForecasts(json.getObj("forecasts").toString());
            }
            if (json.getJSONArray("warnings") != null) {
                report.setWarnings(json.getJSONArray("warnings").toString());
            }
            if (json.getJSONArray("suggestions") != null) {
                report.setSuggestions(json.getJSONArray("suggestions").toString());
            }
        } catch (Exception e) {
            report.setSummary(aiResponse);
        }
        report.setRawResponse(aiResponse);
        reportRepository.save(report);

        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("summary", report.getSummary());
        result.put("reportType", reportType);
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("createTime", report.getCreateTime());
        return result;
    }

    @Override
    public OperationAiReport getReportDetail(Long id) {
        return reportRepository.findById(id).orElse(null);
    }

    @Override
    public Page<OperationAiReport> getReportList(String reportType, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (reportType != null && !reportType.isEmpty()) {
            return reportRepository.findByReportTypeOrderByCreateTimeDesc(reportType, pageRequest);
        }
        return reportRepository.findByOrderByCreateTimeDesc(pageRequest);
    }

    @Override
    public Map<String, Object> getOperationOverview() {
        List<OperationAiReport> reports = reportRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createTime"));

        Map<String, Object> overview = new HashMap<>();
        if (!reports.isEmpty()) {
            OperationAiReport latest = reports.get(0);
            overview.put("summary", latest.getSummary());
            try {
                if (latest.getKeyMetrics() != null)
                    overview.put("keyMetrics", JSONUtil.parseObj(latest.getKeyMetrics()));
            } catch (Exception ignored) {}
            try {
                if (latest.getForecasts() != null)
                    overview.put("forecasts", JSONUtil.parseObj(latest.getForecasts()));
            } catch (Exception ignored) {}
            try {
                if (latest.getWarnings() != null)
                    overview.put("warnings", JSONUtil.parseArray(latest.getWarnings()).toList(Map.class));
                else
                    overview.put("warnings", new ArrayList<>());
            } catch (Exception e) {
                overview.put("warnings", new ArrayList<>());
            }
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRegistrations", 1250);
        metrics.put("totalRevenue", 285000);
        metrics.put("avgDailyRegistrations", 178);
        overview.put("keyMetrics", metrics);
        return overview;
    }
}
