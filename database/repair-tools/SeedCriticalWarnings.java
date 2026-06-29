import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class SeedCriticalWarnings {
    record CaseData(String item, String result, String critical) {}
    static final CaseData[] CASES = {
        new CaseData("血常规危急值复查",
                "血红蛋白(HGB): 48 g/L，参考范围 115-150，危急偏低；红细胞计数(RBC): 2.10 ×10^12/L，参考范围 3.8-5.1，偏低。",
                "血红蛋白严重偏低（48 g/L），存在重度贫血及组织缺氧风险"),
        new CaseData("急诊血糖检测",
                "血糖(GLU): 22.6 mmol/L，参考范围 3.9-6.1，危急偏高；尿酮体: ++。",
                "血糖严重偏高（22.6 mmol/L），伴尿酮体阳性，存在高血糖急症风险"),
        new CaseData("心肌损伤标志物",
                "肌钙蛋白(cTnI): 2.36 ng/mL，参考范围 <0.04，危急偏高；肌酸激酶同工酶(CK-MB): 18.5 ng/mL，参考范围 <5.0，显著偏高。",
                "肌钙蛋白显著升高（2.36 ng/mL），警惕急性心肌损伤")
    };
    static String value(List<String> lines,String key){return lines.stream().map(String::trim).filter(x->x.startsWith(key+":")).map(x->x.substring(x.indexOf(':')+1).split("#",2)[0].trim()).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception{
        var lines=Files.readAllLines(Path.of(args[0]));Class.forName("com.mysql.cj.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(value(lines,"url"),value(lines,"username"),value(lines,"password"))){
            c.setAutoCommit(false);seed(c,101,1,"张子");c.commit();
        }
    }
    static void seed(Connection c,long patientId,long doctorId,String doctorName)throws Exception{
        String patientName,phone;
        try(var ps=c.prepareStatement("select name,phone from patient where id=?")){ps.setLong(1,patientId);try(var rs=ps.executeQuery()){rs.next();patientName=rs.getString(1);phone=rs.getString(2);}}
        try(var check=c.prepareStatement("select count(*) from examination where patient_id=? and item_name='急诊血糖检测'")){check.setLong(1,patientId);try(var rs=check.executeQuery()){rs.next();if(rs.getInt(1)>0){System.out.println("patient "+patientId+" already seeded");return;}}}
        String examSql="insert into examination(patient_id,patient_name,doctor_id,doctor_name,department_id,item_name,type,result,status,create_time,complete_time) values(?,?,?,?,1,?,'检验',?,'已完成',?,?)";
        String warnSql="insert into critical_value_warning(examination_id,patient_id,patient_name,patient_phone,doctor_id,doctor_name,critical_items,warning_level,status,lab_remark,sms_sent,create_time) values(?,?,?,?,?,?,?,'critical','pending','AI已标记加急，请优先复核并立即通知开单医生',0,?)";
        try(var exam=c.prepareStatement(examSql,Statement.RETURN_GENERATED_KEYS);var warn=c.prepareStatement(warnSql)){
            for(int i=0;i<CASES.length;i++){var x=CASES[i];var time=LocalDateTime.now().minusMinutes(i*18L+5);
                exam.setLong(1,patientId);exam.setString(2,patientName);exam.setLong(3,doctorId);exam.setString(4,doctorName);exam.setString(5,x.item);exam.setString(6,x.result);exam.setTimestamp(7,Timestamp.valueOf(time.minusMinutes(20)));exam.setTimestamp(8,Timestamp.valueOf(time));exam.executeUpdate();
                try(var keys=exam.getGeneratedKeys()){keys.next();warn.setLong(1,keys.getLong(1));warn.setLong(2,patientId);warn.setString(3,patientName);warn.setString(4,phone);warn.setLong(5,doctorId);warn.setString(6,doctorName);warn.setString(7,x.critical);warn.setTimestamp(8,Timestamp.valueOf(time.plusSeconds(5)));warn.executeUpdate();}
            }
        }
        System.out.println("patient "+patientId+": 3 critical warnings inserted");
    }
}
