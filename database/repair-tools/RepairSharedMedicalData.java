import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class RepairSharedMedicalData {
    static String value(List<String> l,String k){return l.stream().map(String::trim).filter(x->x.startsWith(k+":")).map(x->x.substring(x.indexOf(':')+1).split("#",2)[0].trim()).findFirst().orElseThrow();}
    static final String[] NAMES={"外科","儿科","妇产科","眼科","耳鼻喉科","口腔科","皮肤科","神经内科","心血管内科","呼吸内科","消化内科","骨科","泌尿外科","急诊科","中医科","康复科","放射科","检验科","药剂科","内科"};
    public static void main(String[] a)throws Exception{
        var l=Files.readAllLines(Path.of(a[0]));Class.forName("com.mysql.cj.jdbc.Driver");
        try(var c=DriverManager.getConnection(value(l,"url"),value(l,"username"),value(l,"password"))){
            c.setAutoCommit(false);backup(c);departments(c);prescriptions(c);c.commit();
        }
    }
    static void backup(Connection c)throws Exception{
        String[] ts={"department","doctor","patient","prescription","examination","critical_value_warning","follow_up_plan","follow_up_record"};
        try(var s=c.createStatement()){for(String t:ts){String b="backup_20260628_"+t;s.execute("create table if not exists "+b+" like "+t);s.execute("insert ignore into "+b+" select * from "+t);}}
    }
    static void departments(Connection c)throws Exception{
        try(var s=c.createStatement()){
            for(String t:new String[]{"doctor","registration","examination"})try{s.executeUpdate("update "+t+" set department_id=mod(department_id-1,20)+1 where department_id>20");}catch(SQLException ignored){}
            try{s.executeUpdate("update triage_record set recommend_department_id=mod(recommend_department_id-1,20)+1 where recommend_department_id>20");}catch(SQLException ignored){}
            s.executeUpdate("delete from department where id>20");
        }
        try(var p=c.prepareStatement("update department set name=?,description=? where id=?")){
            for(int i=0;i<NAMES.length;i++){p.setString(1,NAMES[i]);p.setString(2,NAMES[i]+"提供规范化专科诊疗服务，由经验丰富的医护团队负责诊断、治疗与健康管理。");p.setInt(3,i+1);p.addBatch();}p.executeBatch();
        }
        try(var p=c.prepareStatement("update registration set department_name=? where department_id=?")){for(int i=0;i<NAMES.length;i++){p.setString(1,NAMES[i]);p.setInt(2,i+1);p.addBatch();}p.executeBatch();}
        System.out.println("departments normalized to 20");
    }
    static void prescriptions(Connection c)throws Exception{
        try(var p=c.prepareStatement("select count(*) from prescription where patient_id=101");var r=p.executeQuery()){r.next();if(r.getInt(1)>0)return;}
        String sql="insert into prescription(patient_id,patient_name,doctor_id,doctor_name,department_id,drugs,total_amount,status,create_time,dispense_time) values(101,'高思晗',1,'张子',13,?,?,?,?,?)";
        String[] drugs={
            "[{\"name\":\"阿奇霉素片\",\"specification\":\"0.25g×6片\",\"quantity\":1,\"unit\":\"盒\",\"usage\":\"口服，一次2片，一日1次，连续3日\"},{\"name\":\"盐酸氨溴索片\",\"specification\":\"30mg×20片\",\"quantity\":1,\"unit\":\"盒\",\"usage\":\"口服，一次1片，一日3次\"}]",
            "[{\"name\":\"对乙酰氨基酚片\",\"specification\":\"0.5g×12片\",\"quantity\":1,\"unit\":\"盒\",\"usage\":\"发热时口服，一次1片，间隔6小时以上\"},{\"name\":\"奥美拉唑肠溶胶囊\",\"specification\":\"20mg×14粒\",\"quantity\":1,\"unit\":\"盒\",\"usage\":\"早餐前30分钟口服，一日1次\"}]",
            "[{\"name\":\"阿托伐他汀钙片\",\"specification\":\"20mg×14片\",\"quantity\":1,\"unit\":\"盒\",\"usage\":\"每晚口服，一次1片\"}]"
        };String[] st={"已发药","已发药","待发药"};
        try(var p=c.prepareStatement(sql)){for(int i=0;i<3;i++){p.setString(1,drugs[i]);p.setBigDecimal(2,new java.math.BigDecimal(i==0?"68.50":i==1?"45.00":"38.00"));p.setString(3,st[i]);p.setTimestamp(4,Timestamp.valueOf(LocalDateTime.now().minusDays(i*5L+1)));if("已发药".equals(st[i]))p.setTimestamp(5,Timestamp.valueOf(LocalDateTime.now().minusDays(i*5L)));else p.setNull(5,Types.TIMESTAMP);p.addBatch();}p.executeBatch();}
        System.out.println("3 prescriptions inserted");
    }
}
