import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class SeedPatientExaminations {
    record Report(long itemId, String name, String result, String abnormalJson,
                  String patientText, String professionalText, String advice, String review) {}

    private static String value(List<String> lines, String key) {
        return lines.stream().map(String::trim)
                .filter(line -> line.startsWith(key + ":"))
                .map(line -> line.substring(line.indexOf(':') + 1).split("#", 2)[0].trim())
                .findFirst().orElseThrow();
    }

    private static final Report[] REPORTS = {
        new Report(20, "AI演示·血常规",
            "白细胞计数(WBC): 11.8 ×10^9/L，参考范围 3.5-9.5，偏高；中性粒细胞百分比(NEUT%): 78.6%，参考范围 40-75，偏高；血红蛋白(HGB): 128 g/L，参考范围 115-150，正常；血小板(PLT): 246 ×10^9/L，参考范围 125-350，正常。",
            "[{\"name\":\"白细胞计数\",\"value\":\"11.8\",\"unit\":\"×10^9/L\",\"reference\":\"3.5-9.5\",\"status\":\"升高\"},{\"name\":\"中性粒细胞百分比\",\"value\":\"78.6\",\"unit\":\"%\",\"reference\":\"40-75\",\"status\":\"升高\"}]",
            "白细胞和中性粒细胞轻度升高，常见于近期感染或炎症反应，需要结合是否发热、咳嗽、咽痛等症状判断。",
            "白细胞及中性粒细胞比例增高，倾向急性炎症或细菌感染，建议结合CRP、PCT及临床表现综合评估。",
            "如伴发热、咳嗽或明显不适，建议到呼吸内科或普通内科复诊；不要自行使用抗生素。",
            "症状缓解后3-5天复查血常规；若持续高热或症状加重，应及时就医。"),
        new Report(22, "AI演示·肝功能",
            "丙氨酸氨基转移酶(ALT): 68 U/L，参考范围 7-40，偏高；天门冬氨酸氨基转移酶(AST): 46 U/L，参考范围 13-35，偏高；总胆红素(TBIL): 15.2 μmol/L，参考范围 5-21，正常；白蛋白(ALB): 43.6 g/L，参考范围 40-55，正常。",
            "[{\"name\":\"丙氨酸氨基转移酶\",\"value\":\"68\",\"unit\":\"U/L\",\"reference\":\"7-40\",\"status\":\"升高\"},{\"name\":\"天门冬氨酸氨基转移酶\",\"value\":\"46\",\"unit\":\"U/L\",\"reference\":\"13-35\",\"status\":\"升高\"}]",
            "两项转氨酶轻度升高，提示肝细胞可能受到轻度损伤。熬夜、饮酒、脂肪肝或某些药物都可能造成这种变化。",
            "ALT、AST轻度升高，胆红素及白蛋白正常，提示轻度肝细胞损伤，需排查脂肪肝、饮酒、药物及病毒性肝炎。",
            "近期避免饮酒和熬夜，不要自行服用保健品；建议消化内科或肝病门诊结合腹部超声进一步评估。",
            "建议2-4周复查肝功能；若出现皮肤发黄、尿色明显加深或右上腹痛，应尽快就医。"),
        new Report(23, "AI演示·肾功能",
            "血肌酐(Cr): 96 μmol/L，参考范围 41-81，偏高；尿素氮(BUN): 7.2 mmol/L，参考范围 2.6-7.5，正常；尿酸(UA): 428 μmol/L，参考范围 155-357，偏高；估算肾小球滤过率(eGFR): 76 mL/min/1.73㎡，参考范围 ≥90，偏低。",
            "[{\"name\":\"血肌酐\",\"value\":\"96\",\"unit\":\"μmol/L\",\"reference\":\"41-81\",\"status\":\"升高\"},{\"name\":\"尿酸\",\"value\":\"428\",\"unit\":\"μmol/L\",\"reference\":\"155-357\",\"status\":\"升高\"},{\"name\":\"eGFR\",\"value\":\"76\",\"unit\":\"mL/min/1.73㎡\",\"reference\":\"≥90\",\"status\":\"降低\"}]",
            "肌酐和尿酸升高，同时肾小球滤过率偏低，提示肾脏过滤功能可能有轻度下降，也可能受到饮水少、近期高嘌呤饮食等因素影响。",
            "肌酐及尿酸升高、eGFR下降，需结合既往结果、尿常规、肾脏超声及血压判断是否存在早期肾功能异常。",
            "注意补充水分，减少动物内脏、浓肉汤和大量海鲜；建议肾内科复诊评估。",
            "建议1-2周内复查肾功能、尿常规和尿微量白蛋白；若尿量明显减少或水肿，应及时就医。"),
        new Report(24, "AI演示·血脂四项",
            "总胆固醇(TC): 5.82 mmol/L，参考范围 <5.2，偏高；甘油三酯(TG): 2.16 mmol/L，参考范围 <1.7，偏高；低密度脂蛋白(LDL-C): 3.68 mmol/L，参考范围 <3.4，偏高；高密度脂蛋白(HDL-C): 1.32 mmol/L，参考范围 >1.0，正常。",
            "[{\"name\":\"总胆固醇\",\"value\":\"5.82\",\"unit\":\"mmol/L\",\"reference\":\"<5.2\",\"status\":\"升高\"},{\"name\":\"甘油三酯\",\"value\":\"2.16\",\"unit\":\"mmol/L\",\"reference\":\"<1.7\",\"status\":\"升高\"},{\"name\":\"低密度脂蛋白\",\"value\":\"3.68\",\"unit\":\"mmol/L\",\"reference\":\"<3.4\",\"status\":\"升高\"}]",
            "血脂有多项升高，长期如此会增加动脉硬化和心脑血管疾病风险，但是否需要用药还要结合年龄、血压和其他危险因素判断。",
            "混合型血脂异常，以TC、TG和LDL-C升高为主，建议进行ASCVD总体风险评估后决定生活方式干预或降脂治疗。",
            "减少油炸食品、肥肉和甜饮料，每周进行规律运动；建议心内科或内分泌科评估。",
            "先进行8-12周生活方式调整后空腹复查血脂；若合并高血压、糖尿病，应更早复诊。"),
        new Report(25, "AI演示·空腹血糖",
            "空腹血糖(FPG): 6.42 mmol/L，参考范围 3.9-6.1，偏高；糖化血红蛋白(HbA1c): 6.1%，参考范围 4.0-6.0，偏高。",
            "[{\"name\":\"空腹血糖\",\"value\":\"6.42\",\"unit\":\"mmol/L\",\"reference\":\"3.9-6.1\",\"status\":\"升高\"},{\"name\":\"糖化血红蛋白\",\"value\":\"6.1\",\"unit\":\"%\",\"reference\":\"4.0-6.0\",\"status\":\"升高\"}]",
            "空腹血糖和糖化血红蛋白略高，提示近期平均血糖偏高，但一次结果不能直接诊断糖尿病。",
            "空腹血糖受损并伴HbA1c临界升高，考虑糖调节受损可能，需复测或行口服葡萄糖耐量试验明确。",
            "控制甜食和精制主食，规律运动并保持健康体重；建议内分泌科进一步评估。",
            "建议1-2周内复查空腹血糖，必要时进行OGTT；若出现明显口渴、多尿或体重下降，应尽快就医。"),
        new Report(26, "AI演示·电解质",
            "血钾(K+): 3.28 mmol/L，参考范围 3.5-5.3，偏低；血钠(Na+): 140 mmol/L，参考范围 137-147，正常；血氯(Cl-): 102 mmol/L，参考范围 99-110，正常；血钙(Ca2+): 2.31 mmol/L，参考范围 2.11-2.52，正常。",
            "[{\"name\":\"血钾\",\"value\":\"3.28\",\"unit\":\"mmol/L\",\"reference\":\"3.5-5.3\",\"status\":\"降低\"}]",
            "血钾轻度偏低，可能与进食不足、腹泻呕吐、出汗较多或某些利尿药有关。低钾可能引起乏力、心悸。",
            "轻度低钾血症，其余电解质正常，应结合消化道丢失、利尿剂使用及心电图表现寻找原因。",
            "可适当增加富含钾的食物，但不要自行大量服用补钾药；如有心悸、明显乏力应及时就医。",
            "建议3-7天复查电解质；服用利尿剂者应联系开药医生调整方案。")
    };

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(value(lines, "url"), value(lines, "username"), value(lines, "password"))) {
            c.setAutoCommit(false);
            for (long patientId : new long[]{101}) seedPatient(c, patientId);
            c.commit();
        }
    }

    private static void seedPatient(Connection c, long patientId) throws Exception {
        String patientName;
        try (PreparedStatement ps = c.prepareStatement("select name from patient where id=?")) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Patient not found: " + patientId);
                patientName = rs.getString(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "select count(*) from examination where patient_id=? and item_name like 'AI演示·%'")) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("patient " + patientId + " already seeded, skipped");
                    return;
                }
            }
        }

        String examSql = "insert into examination " +
                "(registration_id,patient_id,patient_name,doctor_id,doctor_name,department_id,item_id,item_name,type,result,result_images,status,create_time,complete_time) " +
                "values (null,?,?,?,?,?,?,?,?,?,null,?,?,?)";
        String aiSql = "insert into examination_ai_interpretation " +
                "(examination_id,patient_id,abnormal_items,interpretation_pro,interpretation_patient,suggestions,review_reminder,raw_response,create_time) " +
                "values (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement exam = c.prepareStatement(examSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement ai = c.prepareStatement(aiSql)) {
            for (int i = 0; i < REPORTS.length; i++) {
                Report r = REPORTS[i];
                LocalDateTime completed = LocalDateTime.now().minusDays(i * 6L + 1);
                exam.setLong(1, patientId);
                exam.setString(2, patientName);
                exam.setLong(3, 10);
                exam.setString(4, "郑娜");
                exam.setLong(5, 1);
                exam.setLong(6, r.itemId());
                exam.setString(7, r.name());
                exam.setString(8, "检验");
                exam.setString(9, r.result());
                exam.setString(10, "已完成");
                exam.setTimestamp(11, Timestamp.valueOf(completed.minusHours(2)));
                exam.setTimestamp(12, Timestamp.valueOf(completed));
                exam.executeUpdate();
                try (ResultSet keys = exam.getGeneratedKeys()) {
                    keys.next();
                    long examId = keys.getLong(1);
                    ai.setLong(1, examId);
                    ai.setLong(2, patientId);
                    ai.setString(3, r.abnormalJson());
                    ai.setString(4, r.professionalText());
                    ai.setString(5, r.patientText());
                    ai.setString(6, r.advice());
                    ai.setString(7, r.review());
                    ai.setString(8, "{\"source\":\"seed-demo\",\"structured\":true}");
                    ai.setTimestamp(9, Timestamp.valueOf(completed.plusMinutes(1)));
                    ai.executeUpdate();
                }
            }

            for (int i = 0; i < 2; i++) {
                exam.setLong(1, patientId);
                exam.setString(2, patientName);
                exam.setLong(3, 10);
                exam.setString(4, "郑娜");
                exam.setLong(5, 1);
                exam.setLong(6, i == 0 ? 7 : 18);
                exam.setString(7, i == 0 ? "AI演示·凝血功能" : "AI演示·甲状腺功能");
                exam.setString(8, "检验");
                exam.setNull(9, Types.LONGVARCHAR);
                exam.setString(10, "待检查");
                exam.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now().minusHours(i + 1)));
                exam.setNull(12, Types.TIMESTAMP);
                exam.executeUpdate();
            }
        }
        System.out.println("patient " + patientId + " seeded: 6 completed + 2 pending");
    }
}
