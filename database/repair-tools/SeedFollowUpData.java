import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class SeedFollowUpData {
    static String value(List<String> l,String k){return l.stream().map(String::trim).filter(x->x.startsWith(k+":")).map(x->x.substring(x.indexOf(':')+1).split("#",2)[0].trim()).findFirst().orElseThrow();}
    static final String Q="[{\"key\":\"recovery\",\"label\":\"整体恢复情况\",\"type\":\"radio\"},{\"key\":\"temperature\",\"label\":\"当前体温\"},{\"key\":\"symptoms\",\"label\":\"目前症状\"},{\"key\":\"medicine\",\"label\":\"服药情况\"},{\"key\":\"wound\",\"label\":\"伤口情况\"}]";
    public static void main(String[] args)throws Exception{
        var l=Files.readAllLines(Path.of(args[0]));
        Class.forName("com.mysql.cj.jdbc.Driver");
        try(var c=DriverManager.getConnection(value(l,"url"),value(l,"username"),value(l,"password"))){
            c.setAutoCommit(false);
            seed(c,101,1);
            c.commit();
        }
    }
    static void seed(Connection c,long patient,long doctor)throws Exception{
        try(var p=c.prepareStatement("select count(*) from follow_up_plan where patient_id=? and disease like '%康复随访%'")){p.setLong(1,patient);try(var r=p.executeQuery()){r.next();if(r.getInt(1)>0){System.out.println("patient "+patient+" already seeded");return;}}}
        long plan1=plan(c,patient,doctor,"上呼吸道感染康复随访","短期恢复随访",4,1);
        record(c,plan1,patient,LocalDateTime.now().minusDays(1),"completed",0,"{\"recovery\":\"略有好转\",\"temperature\":\"36.8\",\"symptoms\":\"偶有咳嗽\",\"medicine\":\"按时服药\",\"wound\":\"无\"}","恢复趋势良好，体温正常，可继续按医嘱服药并注意休息。");
        record(c,plan1,patient,LocalDateTime.now().plusHours(2),"pending",0,null,null);
        record(c,plan1,patient,LocalDateTime.now().plusDays(3),"pending",0,null,null);
        record(c,plan1,patient,LocalDateTime.now().plusDays(7),"pending",0,null,null);
        long plan2=plan(c,patient,doctor,"门诊治疗后康复随访","症状监测随访",3,1);
        record(c,plan2,patient,LocalDateTime.now().minusDays(2),"abnormal",1,"{\"recovery\":\"症状加重\",\"temperature\":\"39.2\",\"symptoms\":\"持续高热并伴胸闷\",\"medicine\":\"按时服药\",\"wound\":\"无\"}","检测到持续高热和胸闷等异常恢复信号，建议尽快联系医生并安排复诊。");
        record(c,plan2,patient,LocalDateTime.now().plusDays(1),"pending",0,null,null);
        record(c,plan2,patient,LocalDateTime.now().plusDays(5),"pending",0,null,null);
        System.out.println("patient "+patient+": 2 plans and 7 tasks inserted");
    }
    static long plan(Connection c,long patient,long doctor,String disease,String type,int total,int completed)throws Exception{
        try(var p=c.prepareStatement("insert into follow_up_plan(patient_id,doctor_id,disease,plan_type,total_times,completed_times,status,create_time) values(?,?,?,?,?,?,'ongoing',?)",Statement.RETURN_GENERATED_KEYS)){p.setLong(1,patient);p.setLong(2,doctor);p.setString(3,disease);p.setString(4,type);p.setInt(5,total);p.setInt(6,completed);p.setTimestamp(7,Timestamp.valueOf(LocalDateTime.now().minusDays(3)));p.executeUpdate();try(var k=p.getGeneratedKeys()){k.next();return k.getLong(1);}}
    }
    static void record(Connection c,long plan,long patient,LocalDateTime time,String status,int abnormal,String answer,String analysis)throws Exception{
        try(var p=c.prepareStatement("insert into follow_up_record(plan_id,patient_id,follow_up_time,questionnaire_json,answer_json,ai_analysis,status,abnormal_flag,create_time) values(?,?,?,?,?,?,?,?,?)")){p.setLong(1,plan);p.setLong(2,patient);p.setTimestamp(3,Timestamp.valueOf(time));p.setString(4,Q);p.setString(5,answer);p.setString(6,analysis);p.setString(7,status);p.setInt(8,abnormal);p.setTimestamp(9,Timestamp.valueOf(LocalDateTime.now().minusDays(3)));p.executeUpdate();}
    }
}
