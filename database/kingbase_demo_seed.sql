SET client_encoding = 'UTF8';

-- 云脑诊疗平台：本地金仓统一演示数据
-- 目标库：cloud_brain_diagnosis
-- 说明：本脚本会清空旧演示数据，并重建一套干净、统一、可联调的数据。

DELETE FROM prescription_ai_review;
DELETE FROM medication_guide;
DELETE FROM stock_forecast;
DELETE FROM critical_value_warning;
DELETE FROM examination_ai_interpretation;
DELETE FROM examination_ai_review;
DELETE FROM follow_up_record;
DELETE FROM follow_up_plan;
DELETE FROM medical_record_ai_generate;
DELETE FROM quality_check_detail;
DELETE FROM quality_check_record;
DELETE FROM operation_ai_report;
DELETE FROM ai_chat_record;
DELETE FROM ai_knowledge_base;
DELETE FROM triage_record;
DELETE FROM prescription;
DELETE FROM examination;
DELETE FROM medical_record;
DELETE FROM registration;
DELETE FROM medicine;
DELETE FROM medicine_category;
DELETE FROM examination_item;
DELETE FROM doctor;
DELETE FROM department;
DELETE FROM patient;
DELETE FROM staff_account;
DELETE FROM admin;

INSERT INTO admin(id, username, password, name, create_time) VALUES
(1, 'admin', '123456', '高思晗管理员', NOW()),
(2, 'admin01', '123456', '平台运营管理员', NOW());

INSERT INTO staff_account(id, username, password, name, phone, role, enabled, create_time, update_time) VALUES
(1, 'pharmacy01', '123456', '王药师', '13800001001', 'pharmacy', B'1', NOW(), NOW()),
(2, 'lab01', '123456', '李检验师', '13800001002', 'lab', B'1', NOW(), NOW());

INSERT INTO department(id, name, description, sort, create_time, update_time) VALUES
(1, '外科', '负责普通外科、创伤处理和术后复诊等诊疗服务。', 1, NOW(), NOW()),
(2, '儿科', '负责儿童常见病、生长发育和儿童呼吸消化系统疾病。', 2, NOW(), NOW()),
(3, '妇产科', '负责妇科疾病、产前咨询、孕期管理和产后复查。', 3, NOW(), NOW()),
(4, '眼科', '负责视力检查、眼表疾病、白内障和眼底相关疾病。', 4, NOW(), NOW()),
(5, '耳鼻喉科', '负责耳、鼻、咽喉及相关头颈部疾病诊疗。', 5, NOW(), NOW()),
(6, '口腔科', '负责牙体牙髓、口腔修复、牙周和口腔预防保健。', 6, NOW(), NOW()),
(7, '皮肤科', '负责皮炎、湿疹、痤疮、过敏及皮肤感染等疾病。', 7, NOW(), NOW()),
(8, '神经内科', '负责头痛、眩晕、脑血管病、癫痫和睡眠障碍。', 8, NOW(), NOW()),
(9, '神经外科', '负责颅脑外伤、神经系统肿瘤和脊髓相关疾病。', 9, NOW(), NOW()),
(10, '精神科', '负责焦虑、抑郁、睡眠障碍和心理行为问题。', 10, NOW(), NOW()),
(11, '心血管内科', '负责高血压、冠心病、心律失常和心衰管理。', 11, NOW(), NOW()),
(12, '呼吸内科', '负责咳嗽、哮喘、肺炎、慢阻肺和呼吸道感染。', 12, NOW(), NOW()),
(13, '消化内科', '负责胃炎、肠炎、消化不良、肝胆胰相关疾病。', 13, NOW(), NOW()),
(14, '内分泌科', '负责糖尿病、甲状腺疾病、痛风和代谢综合征。', 14, NOW(), NOW()),
(15, '肾内科', '负责尿检异常、肾炎、肾功能异常和慢性肾病。', 15, NOW(), NOW()),
(16, '泌尿外科', '负责泌尿系结石、前列腺疾病和排尿异常。', 16, NOW(), NOW()),
(17, '骨科', '负责骨折、关节病、颈肩腰腿痛和运动损伤。', 17, NOW(), NOW()),
(18, '康复科', '负责术后康复、慢病康复和功能训练指导。', 18, NOW(), NOW()),
(19, '检验科', '负责血液、尿液、生化、免疫等检验项目。', 19, NOW(), NOW()),
(20, '影像科', '负责彩超、CT、DR、MRI等影像检查。', 20, NOW(), NOW());

INSERT INTO doctor(id, username, password, name, phone, title, department_id, department_name, specialty, introduction, avatar, create_time, update_time) VALUES
(1,'doctor01','123456','张子','13810000001','主任医师',16,'泌尿外科','泌尿系结石、前列腺疾病、排尿异常','从事泌尿外科临床工作15年，擅长泌尿系结石和前列腺疾病诊疗。',NULL,NOW(),NOW()),
(2,'doctor02','123456','孙达','13810000002','主治医师',16,'泌尿外科','前列腺炎、尿频尿急、泌尿感染','熟悉泌尿外科常见病诊治，注重患者长期随访。',NULL,NOW(),NOW()),
(3,'doctor03','123456','张文','13810000003','住院医师',13,'消化内科','胃炎、反流、腹痛腹泻','擅长消化道常见病初诊和复诊管理。',NULL,NOW(),NOW()),
(4,'doctor04','123456','华功','13810000004','主治医师',18,'康复科','颈肩腰腿痛、术后康复','擅长运动康复和慢病功能恢复。',NULL,NOW(),NOW()),
(5,'doctor05','123456','沈兴','13810000005','主任医师',3,'妇产科','妇科炎症、月经异常、孕期管理','长期从事妇产科门诊工作，擅长妇科常见病和孕期咨询。',NULL,NOW(),NOW()),
(6,'doctor06','123456','林知远','13810000006','主任医师',11,'心血管内科','高血压、冠心病、心律失常','擅长慢病管理和心血管风险评估。',NULL,NOW(),NOW()),
(7,'doctor07','123456','周明川','13810000007','副主任医师',12,'呼吸内科','咳嗽、慢阻肺、肺部感染','擅长呼吸道感染和慢性咳嗽诊治。',NULL,NOW(),NOW()),
(8,'doctor08','123456','苏清禾','13810000008','主治医师',2,'儿科','儿童呼吸道疾病、消化系统疾病','擅长儿童发热、咳嗽和腹泻等常见病。',NULL,NOW(),NOW()),
(9,'doctor09','123456','陈屿安','13810000009','副主任医师',17,'骨科','颈肩腰腿痛、运动损伤、骨关节病','擅长骨科慢病和运动损伤诊疗。',NULL,NOW(),NOW()),
(10,'doctor10','123456','顾云舒','13810000010','主任医师',4,'眼科','屈光不正、干眼症、白内障','擅长眼科常见病和眼底筛查。',NULL,NOW(),NOW()),
(11,'doctor11','123456','刘娜','13810000011','副主任医师',1,'外科','阑尾炎、胆囊疾病、创伤处理','擅长普通外科常见病和术后复诊。',NULL,NOW(),NOW()),
(12,'doctor12','123456','赵清','13810000012','主治医师',1,'外科','甲状腺结节、体表肿物、伤口处理','擅长外科门诊小手术和创面处理。',NULL,NOW(),NOW()),
(13,'doctor13','123456','温以宁','13810000013','主任医师',12,'呼吸内科','肺炎、哮喘、慢性咳嗽','擅长呼吸系统感染和哮喘规范治疗。',NULL,NOW(),NOW()),
(14,'doctor14','123456','贺星河','13810000014','副主任医师',8,'神经内科','头痛、眩晕、脑血管病','擅长头晕头痛和卒中风险评估。',NULL,NOW(),NOW()),
(15,'doctor15','123456','秦越','13810000015','主治医师',14,'内分泌科','糖尿病、甲状腺疾病、痛风','擅长血糖管理和甲状腺功能异常诊疗。',NULL,NOW(),NOW()),
(16,'doctor16','123456','唐屿','13810000016','副主任医师',15,'肾内科','尿检异常、肾炎、肾功能异常','擅长慢性肾病和尿检异常评估。',NULL,NOW(),NOW()),
(17,'doctor17','123456','许棠','13810000017','主治医师',5,'耳鼻喉科','鼻炎、咽喉炎、中耳炎','擅长耳鼻喉科常见病和过敏性鼻炎。',NULL,NOW(),NOW()),
(18,'doctor18','123456','严明','13810000018','主任医师',6,'口腔科','牙周病、牙体牙髓、口腔修复','擅长牙周治疗和口腔修复。',NULL,NOW(),NOW()),
(19,'doctor19','123456','郑书','13810000019','副主任医师',7,'皮肤科','湿疹、皮炎、痤疮、荨麻疹','擅长过敏性皮肤病和慢性皮肤炎症。',NULL,NOW(),NOW()),
(20,'doctor20','123456','姜序','13810000020','主任医师',10,'精神科','焦虑、抑郁、睡眠障碍','擅长情绪障碍和睡眠问题评估。',NULL,NOW(),NOW()),
(21,'doctor21','123456','谢予安','13810000021','主治医师',19,'检验科','检验报告解读、危急值复核','负责检验结果审核和报告质量控制。',NULL,NOW(),NOW()),
(22,'doctor22','123456','郑丽','13810000022','副主任医师',20,'影像科','彩超、CT、MRI影像诊断','擅长腹部超声和胸部影像诊断。',NULL,NOW(),NOW()),
(23,'doctor23','123456','王明','13810000023','主治医师',11,'心血管内科','高血压、胸闷、心悸','擅长心血管常见症状评估。',NULL,NOW(),NOW()),
(24,'doctor24','123456','曹仁','13810000024','副主任医师',13,'消化内科','胃溃疡、胆囊炎、脂肪肝','擅长消化系统慢病管理。',NULL,NOW(),NOW()),
(25,'doctor25','123456','吴山','13810000025','住院医师',2,'儿科','儿童发热、咳嗽、腹泻','负责儿科常见病初诊与随访。',NULL,NOW(),NOW()),
(26,'doctor26','123456','施桂','13810000026','主治医师',3,'妇产科','月经异常、盆腔炎、宫颈筛查','擅长妇科常见病与女性健康管理。',NULL,NOW(),NOW()),
(27,'doctor27','123456','吕安','13810000027','主治医师',4,'眼科','结膜炎、干眼、视疲劳','擅长眼表疾病和视疲劳处理。',NULL,NOW(),NOW()),
(28,'doctor28','123456','薛乔','13810000028','副主任医师',8,'神经内科','失眠、偏头痛、短暂性脑缺血','擅长神经系统常见症状鉴别。',NULL,NOW(),NOW()),
(29,'doctor29','123456','邵怀','13810000029','主任医师',9,'神经外科','颅脑外伤、脑肿瘤、脊髓疾病','擅长神经外科疾病评估和术后复查。',NULL,NOW(),NOW()),
(30,'doctor30','123456','邱然','13810000030','副主任医师',5,'耳鼻喉科','鼻窦炎、听力下降、眩晕','擅长耳鼻喉慢性病诊疗。',NULL,NOW(),NOW()),
(31,'doctor31','123456','梁溪','13810000031','主治医师',6,'口腔科','龋齿、牙髓炎、智齿冠周炎','擅长牙痛诊治和口腔基础治疗。',NULL,NOW(),NOW()),
(32,'doctor32','123456','陆遥','13810000032','主治医师',7,'皮肤科','痤疮、真菌感染、过敏性皮炎','擅长皮肤科门诊常见病。',NULL,NOW(),NOW()),
(33,'doctor33','123456','白芷','13810000033','主治医师',14,'内分泌科','甲亢、甲减、糖尿病','擅长内分泌指标解读和用药指导。',NULL,NOW(),NOW()),
(34,'doctor34','123456','乔木','13810000034','副主任医师',15,'肾内科','蛋白尿、血尿、水肿','擅长肾脏病早期筛查。',NULL,NOW(),NOW()),
(35,'doctor35','123456','叶知秋','13810000035','住院医师',17,'骨科','扭伤、骨质疏松、关节疼痛','负责骨科常见病初诊。',NULL,NOW(),NOW()),
(36,'doctor36','123456','傅青','13810000036','主治医师',18,'康复科','运动损伤康复、肩颈疼痛','擅长康复训练方案制定。',NULL,NOW(),NOW()),
(37,'doctor37','123456','程澈','13810000037','副主任医师',10,'精神科','焦虑障碍、抑郁状态、心理咨询','擅长心理评估和药物随访。',NULL,NOW(),NOW()),
(38,'doctor38','123456','顾北','13810000038','主治医师',20,'影像科','胸片、CT、骨关节影像','擅长常规影像报告解读。',NULL,NOW(),NOW()),
(39,'doctor39','123456','苗青','13810000039','主治医师',19,'检验科','血常规、生化、尿常规复核','负责检验指标复核与危急值上报。',NULL,NOW(),NOW()),
(40,'doctor40','123456','罗川','13810000040','副主任医师',9,'神经外科','椎管疾病、脑外伤复查','擅长神经外科术后随访。',NULL,NOW(),NOW());

INSERT INTO patient(id, openid, login_account, password_hash, name, gender, age, phone, id_card, address, allergy_history, avatar, create_time, update_time) VALUES
(1, 'local-demo-wechat-patient', 'patient01', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '高思晗', '女', 22, '13900000001', '210102200401010021', '辽宁省大连市甘井子区', '无已知药物过敏史', '/static/avatar/patient-demo.png', NOW(), NOW()),
(2, 'demo-openid-zheng', 'patient02', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '郑行', '男', 31, '13900000002', '210102199501010031', '辽宁省沈阳市和平区', '青霉素过敏', NULL, NOW(), NOW()),
(3, 'demo-openid-nan', 'patient03', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '南栀', '女', 28, '13900000003', '210102199801010028', '辽宁省大连市沙河口区', '无', NULL, NOW(), NOW()),
(4, 'demo-openid-yang', 'patient04', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '杨海', '男', 46, '13900000004', '210102198001010046', '辽宁省鞍山市铁东区', '磺胺类药物过敏', NULL, NOW(), NOW()),
(5, 'demo-openid-yan', 'patient05', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '严明', '男', 52, '13900000005', '210102197401010052', '辽宁省大连市中山区', '无', NULL, NOW(), NOW()),
(6, 'demo-openid-li', 'patient06', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '李素', '女', 64, '13900000006', '210102196201010064', '辽宁省营口市站前区', '阿司匹林不耐受', NULL, NOW(), NOW()),
(7, 'demo-openid-wu', 'patient07', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '吴山', '男', 36, '13900000007', '210102199001010036', '辽宁省丹东市振兴区', '无', NULL, NOW(), NOW()),
(8, 'demo-openid-mei', 'patient08', '$2a$10$dwYU4IQ7OZOF.g.cuzu.YeEHVxcWrV6.NkzpL3u0kCo3PsNOfgk7C', '姜序', '女', 41, '13900000008', '210102198501010041', '辽宁省大连市西岗区', '头孢类药物皮疹史', NULL, NOW(), NOW());

INSERT INTO medicine_category(id, name, sort) VALUES
(1,'抗感染药',1),(2,'呼吸系统用药',2),(3,'消化系统用药',3),(4,'心血管系统用药',4),(5,'解热镇痛药',5),(6,'外用药',6),(7,'内分泌用药',7),(8,'中成药',8);

INSERT INTO medicine(id, name, category_id, category_name, specification, unit, price, stock, manufacturer, description, create_time) VALUES
(1,'阿莫西林胶囊',1,'抗感染药','0.25g*24粒','盒',18.50,320,'华北制药股份有限公司','用于敏感菌感染，青霉素过敏者禁用。',NOW()),
(2,'头孢克肟分散片',1,'抗感染药','50mg*12片','盒',32.80,58,'齐鲁制药有限公司','用于呼吸道、泌尿道等敏感菌感染。',NOW()),
(3,'左氧氟沙星片',1,'抗感染药','0.1g*12片','盒',19.60,24,'扬子江药业集团','喹诺酮类抗菌药，儿童及孕妇慎用。',NOW()),
(4,'布洛芬缓释胶囊',5,'解热镇痛药','0.3g*20粒','盒',15.50,18,'中美天津史克制药','用于发热、头痛、关节痛等。',NOW()),
(5,'对乙酰氨基酚片',5,'解热镇痛药','0.5g*12片','盒',9.80,210,'东北制药集团','退热镇痛，避免超量服用。',NOW()),
(6,'氨溴索口服液',2,'呼吸系统用药','100ml','瓶',22.60,76,'上海信谊药厂','祛痰药，适用于痰液黏稠。',NOW()),
(7,'复方甘草片',2,'呼吸系统用药','100片','瓶',12.00,9,'太极集团重庆桐君阁','镇咳祛痰，低库存需及时补货。',NOW()),
(8,'孟鲁司特钠片',2,'呼吸系统用药','10mg*5片','盒',46.80,36,'杭州默沙东制药','用于哮喘和过敏性鼻炎辅助治疗。',NOW()),
(9,'奥美拉唑肠溶胶囊',3,'消化系统用药','20mg*14粒','盒',24.50,95,'阿斯利康制药有限公司','抑酸药，用于胃食管反流和胃炎。',NOW()),
(10,'铝碳酸镁咀嚼片',3,'消化系统用药','0.5g*20片','盒',28.60,44,'拜耳医药保健有限公司','胃黏膜保护药，餐后嚼服。',NOW()),
(11,'蒙脱石散',3,'消化系统用药','3g*10袋','盒',16.80,14,'博福-益普生制药','止泻药，需与其他药物间隔服用。',NOW()),
(12,'硝苯地平控释片',4,'心血管系统用药','30mg*7片','盒',36.00,26,'拜耳医药保健有限公司','降压药，需规律服用。',NOW()),
(13,'阿托伐他汀钙片',4,'心血管系统用药','20mg*7片','盒',42.00,63,'辉瑞制药有限公司','调脂药，睡前服用更常见。',NOW()),
(14,'缬沙坦胶囊',4,'心血管系统用药','80mg*7粒','盒',31.20,8,'北京诺华制药有限公司','降压药，低库存预警。',NOW()),
(15,'二甲双胍缓释片',7,'内分泌用药','0.5g*30片','瓶',21.80,122,'中美上海施贵宝','糖尿病常用药，餐中或餐后服用。',NOW()),
(16,'左甲状腺素钠片',7,'内分泌用药','50μg*100片','盒',29.50,45,'德国默克公司','甲状腺替代治疗，清晨空腹服用。',NOW()),
(17,'双氯芬酸钠凝胶',6,'外用药','20g','支',18.90,66,'北京诺华制药有限公司','外用止痛，避免接触眼口。',NOW()),
(18,'炉甘石洗剂',6,'外用药','100ml','瓶',8.60,11,'上海运佳黄浦制药','用于瘙痒性皮肤病，使用前摇匀。',NOW()),
(19,'莫匹罗星软膏',6,'外用药','10g','支',25.00,33,'中美天津史克制药','外用抗感染药。',NOW()),
(20,'蒲地蓝消炎口服液',8,'中成药','10ml*10支','盒',39.80,52,'济川药业集团','用于咽痛、扁桃体炎等辅助治疗。',NOW()),
(21,'连花清瘟胶囊',8,'中成药','0.35g*24粒','盒',29.80,16,'以岭药业股份有限公司','用于感冒发热、咽痛等。',NOW()),
(22,'银杏叶提取物片',4,'心血管系统用药','40mg*30片','盒',42.00,70,'德国威玛舒培博士药厂','改善循环，遵医嘱服用。',NOW()),
(23,'氯雷他定片',2,'呼吸系统用药','10mg*6片','盒',18.00,39,'拜耳医药保健有限公司','抗过敏药，可能引起嗜睡。',NOW()),
(24,'碳酸钙维D片',7,'内分泌用药','600mg*30片','瓶',38.80,88,'惠氏制药有限公司','补钙及维生素D。',NOW()),
(25,'云南白药气雾剂',6,'外用药','85g+30g','盒',68.00,7,'云南白药集团','跌打损伤外用，低库存。',NOW());

INSERT INTO examination_item(id, name, type, price, description) VALUES
(1,'血常规','检验',35.00,'白细胞、红细胞、血红蛋白、血小板等基础血液检查。'),
(2,'尿常规','检验',28.00,'尿蛋白、尿糖、尿酮体、红细胞和白细胞等尿液筛查。'),
(3,'肝功能','检验',68.00,'ALT、AST、胆红素和白蛋白等肝脏功能指标。'),
(4,'肾功能','检验',72.00,'肌酐、尿素氮、尿酸等肾脏功能指标。'),
(5,'血脂四项','检验',55.00,'总胆固醇、甘油三酯、高低密度脂蛋白。'),
(6,'空腹血糖','检验',18.00,'空腹血糖水平检测。'),
(7,'电解质','检验',46.00,'钾、钠、氯、钙等电解质水平。'),
(8,'甲状腺功能','检验',158.00,'TSH、FT3、FT4等甲状腺功能指标。'),
(9,'凝血功能','检验',86.00,'PT、APTT、INR等凝血指标。'),
(10,'心肌损伤标志物','检验',168.00,'肌钙蛋白、肌酸激酶同工酶等。'),
(11,'C反应蛋白','检验',45.00,'炎症反应指标。'),
(12,'幽门螺杆菌检测','检验',96.00,'呼气试验或相关抗原检测。'),
(13,'胸部X线','检查',80.00,'胸部正位片，筛查肺部感染、结节等。'),
(14,'腹部彩超','检查',120.00,'肝胆胰脾肾等腹部脏器超声。'),
(15,'妇科彩超','检查',128.00,'子宫、附件等妇科超声检查。'),
(16,'心电图','检查',30.00,'十二导联心电图。'),
(17,'头颅CT','检查',260.00,'头颅CT平扫。'),
(18,'胸部CT','检查',320.00,'胸部CT平扫。'),
(19,'膝关节MRI','检查',680.00,'膝关节磁共振检查。'),
(20,'胃镜','检查',360.00,'电子胃镜检查。');

INSERT INTO registration(id, patient_id, patient_name, doctor_id, doctor_name, department_id, department_name, registration_date, time_slot, status, create_time, update_time) VALUES
(1,1,'高思晗',7,'周明川',12,'呼吸内科',CURRENT_DATE - INTERVAL '8 day','上午','已就诊',NOW() - INTERVAL '8 day',NOW() - INTERVAL '8 day'),
(2,1,'高思晗',13,'温以宁',12,'呼吸内科',CURRENT_DATE - INTERVAL '3 day','下午','已就诊',NOW() - INTERVAL '3 day',NOW() - INTERVAL '3 day'),
(3,1,'高思晗',6,'林知远',11,'心血管内科',CURRENT_DATE,'上午','待就诊',NOW() - INTERVAL '1 hour',NOW() - INTERVAL '1 hour'),
(4,1,'高思晗',15,'秦越',14,'内分泌科',CURRENT_DATE + INTERVAL '1 day','下午','待就诊',NOW(),NOW()),
(5,2,'郑行',1,'张子',16,'泌尿外科',CURRENT_DATE,'下午','待就诊',NOW(),NOW()),
(6,3,'南栀',5,'沈兴',3,'妇产科',CURRENT_DATE,'上午','就诊中',NOW(),NOW()),
(7,4,'杨海',11,'刘娜',1,'外科',CURRENT_DATE - INTERVAL '1 day','上午','已就诊',NOW() - INTERVAL '1 day',NOW() - INTERVAL '1 day'),
(8,5,'严明',24,'曹仁',13,'消化内科',CURRENT_DATE - INTERVAL '2 day','上午','已就诊',NOW() - INTERVAL '2 day',NOW() - INTERVAL '2 day'),
(9,6,'李素',16,'唐屿',15,'肾内科',CURRENT_DATE - INTERVAL '4 day','下午','已就诊',NOW() - INTERVAL '4 day',NOW() - INTERVAL '4 day'),
(10,7,'吴山',17,'许棠',5,'耳鼻喉科',CURRENT_DATE + INTERVAL '2 day','上午','待就诊',NOW(),NOW()),
(11,8,'姜序',19,'郑书',7,'皮肤科',CURRENT_DATE + INTERVAL '3 day','下午','待就诊',NOW(),NOW()),
(12,1,'高思晗',3,'张文',13,'消化内科',CURRENT_DATE - INTERVAL '15 day','上午','已就诊',NOW() - INTERVAL '15 day',NOW() - INTERVAL '15 day'),
(13,1,'高思晗',10,'顾云舒',4,'眼科',CURRENT_DATE - INTERVAL '20 day','下午','已取消',NOW() - INTERVAL '20 day',NOW() - INTERVAL '19 day');

INSERT INTO medical_record(id, registration_id, patient_id, patient_name, doctor_id, doctor_name, department_id, chief_complaint, present_illness, past_history, physical_examination, diagnosis, treatment, create_time, update_time) VALUES
(1,1,1,'高思晗',7,'周明川',12,'咳嗽、咽痛3天','受凉后出现咽痛、干咳，偶有低热，无胸痛气促。','否认慢性肺病史。','咽部充血，双肺呼吸音清，未闻及明显啰音。','上呼吸道感染','多饮水，规律休息，必要时复诊；给予对症药物治疗。',NOW() - INTERVAL '8 day',NOW() - INTERVAL '8 day'),
(2,2,1,'高思晗',13,'温以宁',12,'咳嗽仍有痰5天','咳嗽较前减轻，仍有少量白痰，夜间明显。','无药物过敏史。','咽部轻度充血，肺部未闻及湿啰音。','急性支气管炎恢复期','继续祛痰治疗，避免辛辣刺激，3日后随访。',NOW() - INTERVAL '3 day',NOW() - INTERVAL '3 day'),
(3,12,1,'高思晗',3,'张文',13,'餐后胃部不适1周','餐后上腹胀痛，偶有反酸，无呕血黑便。','否认胃溃疡病史。','上腹部轻压痛，无反跳痛。','慢性胃炎','规律饮食，给予抑酸和胃黏膜保护治疗。',NOW() - INTERVAL '15 day',NOW() - INTERVAL '15 day'),
(4,7,4,'杨海',11,'刘娜',1,'右下腹疼痛半天','右下腹持续性疼痛，伴恶心，无腹泻。','既往体健。','右下腹压痛，轻度反跳痛。','急性阑尾炎待排','完善血常规和腹部彩超，必要时外科处理。',NOW() - INTERVAL '1 day',NOW() - INTERVAL '1 day');

INSERT INTO prescription(id, registration_id, patient_id, patient_name, doctor_id, doctor_name, department_id, status, total_amount, drugs, create_time, dispense_time) VALUES
(1,1,1,'高思晗',7,'周明川',12,'已发药',68.90,'[{"medicineId":6,"name":"氨溴索口服液","specification":"100ml","unit":"瓶","price":22.60,"quantity":1,"usage":"口服，一次10ml，一日3次，连续3日"},{"medicineId":20,"name":"蒲地蓝消炎口服液","specification":"10ml*10支","unit":"盒","price":39.80,"quantity":1,"usage":"口服，一次10ml，一日3次，连续3日"},{"medicineId":5,"name":"对乙酰氨基酚片","specification":"0.5g*12片","unit":"盒","price":9.80,"quantity":1,"usage":"发热时口服，一次1片，间隔6小时"}]',NOW() - INTERVAL '8 day',NOW() - INTERVAL '8 day' + INTERVAL '2 hour'),
(2,2,1,'高思晗',13,'温以宁',12,'已发药',75.20,'[{"medicineId":6,"name":"氨溴索口服液","specification":"100ml","unit":"瓶","price":22.60,"quantity":1,"usage":"口服，一次10ml，一日3次"},{"medicineId":8,"name":"孟鲁司特钠片","specification":"10mg*5片","unit":"盒","price":46.80,"quantity":1,"usage":"睡前口服，一次1片"},{"medicineId":23,"name":"氯雷他定片","specification":"10mg*6片","unit":"盒","price":18.00,"quantity":1,"usage":"口服，一日1次，一次1片"}]',NOW() - INTERVAL '3 day',NOW() - INTERVAL '3 day' + INTERVAL '1 hour'),
(3,12,1,'高思晗',3,'张文',13,'待发药',53.10,'[{"medicineId":9,"name":"奥美拉唑肠溶胶囊","specification":"20mg*14粒","unit":"盒","price":24.50,"quantity":1,"usage":"早餐前口服，一次1粒，一日1次"},{"medicineId":10,"name":"铝碳酸镁咀嚼片","specification":"0.5g*20片","unit":"盒","price":28.60,"quantity":1,"usage":"餐后嚼服，一次1片，一日3次"}]',NOW() - INTERVAL '15 day',NULL),
(4,7,4,'杨海',11,'刘娜',1,'待发药',47.00,'[{"medicineId":2,"name":"头孢克肟分散片","specification":"50mg*12片","unit":"盒","price":32.80,"quantity":1,"usage":"口服，一次1片，一日2次"},{"medicineId":4,"name":"布洛芬缓释胶囊","specification":"0.3g*20粒","unit":"盒","price":15.50,"quantity":1,"usage":"疼痛时口服，一次1粒"}]',NOW() - INTERVAL '1 day',NULL),
(5,8,5,'严明',24,'曹仁',13,'已发药',24.50,'[{"medicineId":9,"name":"奥美拉唑肠溶胶囊","specification":"20mg*14粒","unit":"盒","price":24.50,"quantity":1,"usage":"早餐前口服，一次1粒"}]',NOW() - INTERVAL '2 day',NOW() - INTERVAL '2 day' + INTERVAL '1 hour'),
(6,9,6,'李素',16,'唐屿',15,'待发药',21.80,'[{"medicineId":15,"name":"二甲双胍缓释片","specification":"0.5g*30片","unit":"瓶","price":21.80,"quantity":1,"usage":"餐中口服，一次1片，一日2次"}]',NOW() - INTERVAL '4 day',NULL);

INSERT INTO examination(id, registration_id, patient_id, patient_name, doctor_id, doctor_name, department_id, item_id, item_name, type, status, result, result_images, create_time, complete_time) VALUES
(1,1,1,'高思晗',7,'周明川',12,1,'血常规','检验','已完成','白细胞计数(WBC): 11.8 ×10^9/L，参考范围 3.5-9.5，偏高；中性粒细胞百分比(NEUT%): 78.6%，参考范围 40-75，偏高；血红蛋白(HGB): 128 g/L，参考范围 115-150，正常；红细胞计数(RBC): 4.30 ×10^12/L，参考范围 3.8-5.1，正常；血小板计数(PLT): 220 ×10^9/L，参考范围 125-350，正常。',NULL,NOW() - INTERVAL '8 day',NOW() - INTERVAL '8 day' + INTERVAL '30 minute'),
(2,1,1,'高思晗',7,'周明川',12,13,'胸部X线','检查','已完成','双肺纹理稍增多，未见明显实变影；心影大小正常；双膈面光整。',NULL,NOW() - INTERVAL '8 day',NOW() - INTERVAL '8 day' + INTERVAL '45 minute'),
(3,2,1,'高思晗',13,'温以宁',12,11,'C反应蛋白','检验','已完成','C反应蛋白(CRP): 18 mg/L，参考范围 0-10，偏高。',NULL,NOW() - INTERVAL '3 day',NOW() - INTERVAL '3 day' + INTERVAL '20 minute'),
(4,12,1,'高思晗',3,'张文',13,3,'肝功能','检验','已完成','丙氨酸氨基转移酶(ALT): 25 U/L，参考范围 7-40，正常；天门冬氨酸氨基转移酶(AST): 22 U/L，参考范围 13-35，正常；总胆红素(TBIL): 12 μmol/L，参考范围 3.4-20.5，正常。',NULL,NOW() - INTERVAL '15 day',NOW() - INTERVAL '15 day' + INTERVAL '35 minute'),
(5,12,1,'高思晗',3,'张文',13,20,'胃镜','检查','待检查',NULL,NULL,NOW() - INTERVAL '15 day',NULL),
(6,7,4,'杨海',11,'刘娜',1,1,'血常规','检验','已完成','白细胞计数(WBC): 16.3 ×10^9/L，参考范围 3.5-9.5，明显偏高；中性粒细胞百分比(NEUT%): 86.2%，参考范围 40-75，偏高；C反应蛋白(CRP): 55 mg/L，参考范围 0-10，明显偏高。',NULL,NOW() - INTERVAL '1 day',NOW() - INTERVAL '1 day' + INTERVAL '30 minute'),
(7,7,4,'杨海',11,'刘娜',1,14,'腹部彩超','检查','已完成','右下腹可见管状低回声结构，管径约8mm，局部压痛明显，考虑阑尾炎可能。',NULL,NOW() - INTERVAL '1 day',NOW() - INTERVAL '1 day' + INTERVAL '1 hour'),
(8,8,5,'严明',24,'曹仁',13,6,'空腹血糖','检验','已完成','空腹血糖(GLU): 22.6 mmol/L，参考范围 3.9-6.1，危急偏高；尿酮体: ++，参考范围 阴性，异常。',NULL,NOW() - INTERVAL '2 day',NOW() - INTERVAL '2 day' + INTERVAL '25 minute'),
(9,9,6,'李素',16,'唐屿',15,2,'尿常规','检验','已完成','尿蛋白(PRO): ++，参考范围 阴性，偏高；尿白细胞(WBC): 28 /HP，参考范围 0-5，偏高；尿红细胞(RBC): 15 /HP，参考范围 0-3，偏高；尿糖(GLU): 阴性，参考范围 阴性，正常。',NULL,NOW() - INTERVAL '4 day',NOW() - INTERVAL '4 day' + INTERVAL '20 minute'),
(10,9,6,'李素',16,'唐屿',15,4,'肾功能','检验','已完成','血肌酐(CREA): 136 μmol/L，参考范围 45-84，偏高；尿素氮(BUN): 9.5 mmol/L，参考范围 2.9-8.2，偏高；尿酸(UA): 420 μmol/L，参考范围 155-357，偏高。',NULL,NOW() - INTERVAL '4 day',NOW() - INTERVAL '4 day' + INTERVAL '25 minute'),
(11,3,1,'高思晗',6,'林知远',11,16,'心电图','检查','待检查',NULL,NULL,NOW() - INTERVAL '1 hour',NULL),
(12,3,1,'高思晗',6,'林知远',11,5,'血脂四项','检验','待检查',NULL,NULL,NOW() - INTERVAL '1 hour',NULL),
(13,10,7,'吴山',17,'许棠',5,1,'血常规','检验','待检查',NULL,NULL,NOW(),NULL),
(14,11,8,'姜序',19,'郑书',7,11,'C反应蛋白','检验','待检查',NULL,NULL,NOW(),NULL);

INSERT INTO examination_ai_interpretation(id, examination_id, patient_id, abnormal_items, interpretation_patient, interpretation_pro, suggestions, review_reminder, raw_response, create_time) VALUES
(1,1,1,'[{"name":"白细胞计数","value":"11.8 ×10^9/L","reference":"3.5-9.5","status":"偏高"},{"name":"中性粒细胞百分比","value":"78.6%","reference":"40-75","status":"偏高"}]','白细胞和中性粒细胞偏高，常见于近期感染或炎症反应，需要结合发热、咳嗽、咽痛等症状判断。','感染相关指标升高，结合症状倾向急性上呼吸道感染。','如症状加重或高热不退，请及时复诊；不建议自行使用抗生素。','症状缓解后3-5天可复查血常规。','AI检验报告自动解读结果',NOW() - INTERVAL '8 day'),
(2,3,1,'[{"name":"C反应蛋白","value":"18 mg/L","reference":"0-10","status":"偏高"}]','CRP偏高提示体内存在炎症反应，结合咳嗽咳痰情况建议继续观察。','轻中度炎症反应，符合支气管炎恢复期。','继续按医嘱用药，若胸闷、气促或发热加重需复诊。','建议3-5天后结合症状决定是否复查。','AI检验报告自动解读结果',NOW() - INTERVAL '3 day'),
(3,8,5,'[{"name":"空腹血糖","value":"22.6 mmol/L","reference":"3.9-6.1","status":"危急偏高"},{"name":"尿酮体","value":"++","reference":"阴性","status":"异常"}]','血糖明显升高且尿酮体阳性，存在高血糖危急风险，应尽快就医。','高血糖危急值，需要排查酮症酸中毒风险。','建议立即联系医生或急诊处理。','处理后需复查血糖、尿酮体和电解质。','AI危急值解读结果',NOW() - INTERVAL '2 day');

INSERT INTO critical_value_warning(id, examination_id, patient_id, patient_name, patient_phone, doctor_id, doctor_name, critical_items, warning_level, status, sms_sent, lab_remark, doctor_remark, create_time, doctor_confirm_time) VALUES
(1,8,5,'严明','13900000005',24,'曹仁','空腹血糖 22.6 mmol/L，尿酮体 ++，疑似高血糖危急值','high','pending',TRUE,'检验科已标记加急，并建议医生立即查看。',NULL,NOW() - INTERVAL '2 day',NULL),
(2,6,4,'杨海','13900000004',11,'刘娜','白细胞 16.3 ×10^9/L，CRP 55 mg/L，提示急性感染风险','medium','confirmed',TRUE,'结果已优先复核，建议结合腹痛情况处理。','已联系患者复诊，安排外科进一步评估。',NOW() - INTERVAL '1 day',NOW() - INTERVAL '1 day' + INTERVAL '15 minute');

INSERT INTO examination_ai_review(id, examination_id, patient_id, lab_staff_id, review_result, review_score, abnormal_items, logic_issues, history_compare, warnings, suggestions, raw_response, review_time) VALUES
(1,1,1,2,'manual',88,'[{"name":"白细胞计数","value":"11.8","reference":"3.5-9.5","status":"偏高","level":"moderate"},{"name":"中性粒细胞百分比","value":"78.6","reference":"40-75","status":"偏高","level":"moderate"}]','白细胞升高与中性粒细胞比例升高一致，无明显逻辑矛盾。','较上次无历史对照，建议人工复核感染指标。','感染指标偏高，需要检验师确认后发布。','建议人工复核后发布。','AI审核：人工复核',NOW() - INTERVAL '8 day'),
(2,3,1,2,'pass',94,'[{"name":"C反应蛋白","value":"18","reference":"0-10","status":"偏高","level":"moderate"}]','单项炎症指标偏高，与临床感染表现相符。','较前次炎症指标下降。','无危急值。','可自动通过，医生结合临床查看。','AI审核：自动通过',NOW() - INTERVAL '3 day'),
(3,8,5,2,'manual',76,'[{"name":"空腹血糖","value":"22.6","reference":"3.9-6.1","status":"危急偏高","level":"severe"},{"name":"尿酮体","value":"++","reference":"阴性","status":"异常","level":"severe"}]','血糖危急值与尿酮体阳性存在临床相关性。','较历史血糖明显升高。','触发危急值预警，需医生确认。','建议人工复核并同步危急值预警。','AI审核：人工复核并预警',NOW() - INTERVAL '2 day'),
(4,9,6,2,'pass',91,'[{"name":"尿蛋白","value":"++","reference":"阴性","status":"偏高","level":"moderate"},{"name":"尿白细胞","value":"28 /HP","reference":"0-5","status":"偏高","level":"moderate"}]','尿蛋白与白细胞升高提示泌尿系统炎症可能。','较历史尿检异常波动不大。','无危急值。','自动通过，建议肾内科结合症状处理。','AI审核：自动通过',NOW() - INTERVAL '4 day'),
(5,10,6,2,'manual',84,'[{"name":"血肌酐","value":"136 μmol/L","reference":"45-84","status":"偏高","level":"moderate"}]','肾功能指标与尿检异常相关，需要结合病史判断。','较历史肌酐轻度升高。','无危急值。','建议人工复核肾功能异常。','AI审核：人工复核',NOW() - INTERVAL '4 day'),
(6,14,8,2,'reject',52,'[{"name":"C反应蛋白","value":"-5 mg/L","reference":"0-10","status":"逻辑异常","level":"severe"}]','CRP数值为负数，不符合生理和检验逻辑。','无法与历史结果比较。','数据录入明显错误。','退回重测或重新录入结果。','AI审核：退回重测',NOW() - INTERVAL '1 hour');

INSERT INTO medication_guide(id, prescription_id, patient_id, patient_age, patient_gender, drugs_json, guide_content, raw_response, print_count, create_time) VALUES
(1,1,1,22,'女','[{"name":"氨溴索口服液","usage":"一次10ml，一日3次"},{"name":"蒲地蓝消炎口服液","usage":"一次10ml，一日3次"},{"name":"对乙酰氨基酚片","usage":"发热时服用"}]','用药时间：氨溴索和蒲地蓝可饭后服用；对乙酰氨基酚仅在发热或明显疼痛时使用。饮食禁忌：避免饮酒、辛辣刺激和熬夜，多饮水。不良反应提醒：如出现皮疹、明显胃部不适、呼吸困难，请立即停药并就医。','AI个性化用药指导',1,NOW() - INTERVAL '8 day'),
(2,2,1,22,'女','[{"name":"氨溴索口服液","usage":"一次10ml，一日3次"},{"name":"孟鲁司特钠片","usage":"睡前一次1片"},{"name":"氯雷他定片","usage":"一日1次"}]','用药时间：孟鲁司特建议睡前服用，氯雷他定每日固定时间服用。饮食禁忌：避免饮酒，少吃辛辣油腻。注意事项：若出现嗜睡、皮疹或胸闷，请及时联系医生。','AI个性化用药指导',0,NOW() - INTERVAL '3 day');

INSERT INTO follow_up_plan(id, patient_id, doctor_id, registration_id, disease, plan_type, total_times, completed_times, status, create_time) VALUES
(1,1,13,2,'上呼吸道感染康复随访','短期康复随访',3,1,'进行中',NOW() - INTERVAL '3 day'),
(2,1,3,12,'门诊治疗后康复随访','消化系统随访',3,0,'进行中',NOW() - INTERVAL '15 day');

INSERT INTO follow_up_record(id, plan_id, patient_id, follow_up_time, questionnaire_json, answer_json, ai_analysis, abnormal_flag, status, doctor_remark, create_time) VALUES
(1,1,1,CURRENT_DATE,'{"title":"第1次随访","fields":["体温","症状变化","服药情况","伤口情况"]}','{"temperature":"36.7","symptom":"咳嗽减轻，无发热","medicine":"按时服药","wound":"无"}','恢复趋势良好，暂无异常信号，建议继续休息并完成疗程。',FALSE,'已完成',NULL,NOW() - INTERVAL '1 day'),
(2,1,1,CURRENT_DATE + INTERVAL '2 day','{"title":"第2次随访","fields":["体温","咳嗽情况","痰液颜色","服药情况"]}',NULL,NULL,FALSE,'待填写',NULL,NOW()),
(3,1,1,CURRENT_DATE + INTERVAL '5 day','{"title":"第3次随访","fields":["整体恢复","是否复诊","是否仍有症状"]}',NULL,NULL,FALSE,'待填写',NULL,NOW()),
(4,2,1,CURRENT_DATE + INTERVAL '1 day','{"title":"第1次随访","fields":["胃痛程度","反酸情况","饮食情况","服药情况"]}',NULL,NULL,FALSE,'待填写',NULL,NOW()),
(5,2,1,CURRENT_DATE + INTERVAL '4 day','{"title":"第2次随访","fields":["腹胀情况","黑便呕血","饮食恢复"]}',NULL,NULL,FALSE,'待填写',NULL,NOW()),
(6,2,1,CURRENT_DATE + INTERVAL '7 day','{"title":"第3次随访","fields":["整体恢复","是否需要复诊"]}',NULL,NULL,FALSE,'待填写',NULL,NOW());

INSERT INTO ai_knowledge_base(id, category, question, answer, keywords, sort, status, create_time, update_time) VALUES
(1,'挂号流程','门诊挂号时间是什么时候？','门诊挂号时间为每日 07:30-16:30，上午号建议 11:00 前到院，下午号建议 15:30 前到院。急诊 24 小时开放。','挂号时间,门诊时间,预约',1,1,NOW(),NOW()),
(2,'挂号流程','忘记预约时间怎么办？','可在患者端“挂号记录”查看预约日期、时段和医生；如需取消，请在就诊前通过挂号详情页取消。','预约时间,挂号记录,取消预约',2,1,NOW(),NOW()),
(3,'取药流程','取药流程怎么走？','医生开具处方后，请先完成缴费，再到一楼门诊药房取号等候；药师核对姓名、处方和药品后完成发药。','取药,药房,处方,缴费',3,1,NOW(),NOW()),
(4,'取药流程','药房在哪里？','门诊药房在一楼大厅右侧，靠近收费窗口。发药窗口分为西药窗口和中成药窗口。','药房位置,一楼,取药窗口',4,1,NOW(),NOW()),
(5,'报告查询','检验报告在哪里查看？','血常规、尿常规、生化等检验报告可在患者端“检查报告”查看；通常采样后 30 分钟到 2 小时内出具。','检验报告,报告查询,血常规',5,1,NOW(),NOW()),
(6,'报告查询','检查报告在哪里查看？','彩超、CT、DR、MRI 等检查报告可在患者端“检查报告”查看，影像片请按影像科提示领取或扫码查看。','检查报告,影像报告,CT,彩超',6,1,NOW(),NOW()),
(7,'楼层导航','彩超在哪里做？','彩超检查在门诊楼 2 楼超声医学科，请按预约时间提前 10 分钟到达。','彩超,超声,二楼,超声医学科',7,1,NOW(),NOW()),
(8,'楼层导航','CT在哪里做？','CT 检查在门诊楼 1 楼影像中心，入口在急诊通道旁。请携带检查申请单并按叫号进入。','CT,影像中心,一楼',8,1,NOW(),NOW()),
(9,'楼层导航','抽血在哪里？','抽血窗口在门诊楼 2 楼检验科采血区，空腹项目请尽量上午完成采样。','抽血,采血,检验科,二楼',9,1,NOW(),NOW()),
(10,'楼层导航','尿常规在哪里留样？','尿常规留样在门诊楼 2 楼检验科自助留样区，留样后将样本交至检验接收窗口。','尿常规,留样,检验科',10,1,NOW(),NOW()),
(11,'缴费流程','在哪里缴费？','可在患者端线上缴费；也可以到一楼大厅收费窗口或自助机缴费。','缴费,收费窗口,自助机',11,1,NOW(),NOW()),
(12,'急诊服务','急诊在哪里？','急诊入口在医院东门，24 小时开放。胸痛、呼吸困难、意识不清、大量出血等情况请立即前往急诊。','急诊,东门,24小时',12,1,NOW(),NOW()),
(13,'医保服务','医保窗口在哪里？','医保咨询窗口在一楼大厅服务台旁，可办理医保咨询和报销流程说明。','医保,报销,咨询窗口',13,1,NOW(),NOW()),
(14,'住院办理','住院手续在哪里办理？','住院办理在一楼住院服务中心，需携带身份证、医保卡和医生开具的入院通知。','住院,入院,办理',14,1,NOW(),NOW()),
(15,'检查准备','腹部彩超需要空腹吗？','腹部彩超通常需要空腹 6-8 小时；泌尿系彩超可能需要憋尿，请按预约单提示准备。','腹部彩超,空腹,憋尿',15,1,NOW(),NOW()),
(16,'检查准备','抽血前能吃饭吗？','血糖、肝功能、血脂等项目一般需要空腹 8-12 小时；普通血常规通常不强制空腹。','抽血,空腹,血脂,肝功能',16,1,NOW(),NOW()),
(17,'报告查询','报告迟迟没出来怎么办？','请先确认检查是否完成。若超过预计时间仍未出报告，可咨询二楼检验科服务台或一楼导诊台。','报告延迟,检验科服务台',17,1,NOW(),NOW()),
(18,'院内服务','导诊台在哪里？','导诊台在门诊一楼大厅入口处，可咨询科室位置、就诊流程和报告领取问题。','导诊台,一楼大厅,咨询',18,1,NOW(),NOW()),
(19,'复诊流程','复诊还需要重新挂号吗？','大多数复诊需要重新预约挂号；医生特别交代的短期复查，请按医生说明执行。','复诊,重新挂号,复查',19,1,NOW(),NOW()),
(20,'用药流程','处方药可以代取吗？','可由家属代取，但需出示患者就诊信息或处方二维码，药师会核对患者身份和药品信息。','代取药,处方药,家属',20,1,NOW(),NOW());

INSERT INTO ai_chat_record(id, user_id, user_type, session_id, question, answer, category, source, feedback, raw_response, create_time) VALUES
(1,1,'patient','s001','彩超在哪里做？','彩超检查在门诊楼2楼超声医学科，请提前10分钟到达。','楼层导航','知识库优先','useful','命中知识库：彩超位置',NOW() - INTERVAL '5 day'),
(2,1,'patient','s001','CT在哪里？','CT检查在门诊楼1楼影像中心，入口在急诊通道旁。','楼层导航','知识库优先','useful','命中知识库：CT位置',NOW() - INTERVAL '5 day'),
(3,1,'patient','s002','取药流程怎么走？','请先完成缴费，再到一楼门诊药房取号等候，药师核对后发药。','取药流程','知识库优先','useful','命中知识库：取药流程',NOW() - INTERVAL '3 day'),
(4,2,'patient','s003','检验报告在哪里查？','可在患者端“检查报告”查看，采样后通常30分钟到2小时出具。','报告查询','知识库优先','useful','命中知识库：报告查询',NOW() - INTERVAL '2 day'),
(5,1,'patient','s004','腹部彩超要空腹吗？','腹部彩超通常需要空腹6-8小时，泌尿系彩超可能需要憋尿。','检查准备','知识库优先','useful','命中知识库：检查准备',NOW() - INTERVAL '1 day'),
(6,3,'patient','s005','急诊在哪里？','急诊入口在医院东门，24小时开放。','急诊服务','知识库优先','useful','命中知识库：急诊位置',NOW() - INTERVAL '1 day');

INSERT INTO triage_record(id, patient_id, chief_complaint, recommend_department_id, recommend_department, recommend_doctor_ids, confidence, ai_analysis, raw_response, status, create_time) VALUES
(1,1,'咳嗽咽痛三天，伴低热和乏力。',12,'呼吸内科','7,13',92,'症状以呼吸道感染表现为主，建议呼吸内科就诊。','AI分诊结果', '已完成', NOW() - INTERVAL '8 day'),
(2,1,'餐后胃部不适、反酸、腹胀。',13,'消化内科','3,24',89,'症状符合消化系统疾病表现，建议消化内科就诊。','AI分诊结果', '已完成', NOW() - INTERVAL '15 day'),
(3,2,'尿频尿急，排尿疼痛。',16,'泌尿外科','1,2',91,'泌尿系统感染或结石需鉴别，建议泌尿外科。','AI分诊结果', '已完成', NOW() - INTERVAL '1 day'),
(4,3,'月经不规律，下腹隐痛。',3,'妇产科','5,26',88,'建议妇产科完善相关检查。','AI分诊结果', '已完成', NOW() - INTERVAL '1 day'),
(5,4,'右下腹疼痛伴恶心。',1,'外科','11,12',94,'需警惕急腹症，建议外科优先就诊。','AI分诊结果', '已完成', NOW() - INTERVAL '1 day');

INSERT INTO operation_ai_report(id, report_type, start_date, end_date, summary, key_metrics, trends_analysis, suggestions, warnings, forecasts, raw_response, create_time) VALUES
(1,'日报',CURRENT_DATE - INTERVAL '1 day',CURRENT_DATE,'本日AI能力运行平稳，智能分诊、就医问答、检验解读和药品库存预测均有调用。','{"aiTriageCount":42,"triageAccuracy":"91.4%","aiChatCount":86,"knowledgeHitRate":"84.8%","reportInterpretationCount":18,"medicationGuideCount":12,"criticalWarningCount":2}','智能问答调用量较昨日增长8%，知识库命中率稳定。','补充楼层导航和检查准备类知识；继续优化危急值处理闭环。','药房低库存药品有复方甘草片、缬沙坦胶囊、云南白药气雾剂。','未来一周呼吸系统用药需求可能上升。','AI运营报告',NOW() - INTERVAL '1 day'),
(2,'周报',CURRENT_DATE - INTERVAL '7 day',CURRENT_DATE,'本周AI功能覆盖诊前、诊中、诊后多个环节，患者端使用较活跃。','{"aiTriageCount":186,"triageAccuracy":"91.4%","aiChatCount":342,"knowledgeHitRate":"84.8%","qualityCheckCount":5}','智能分诊调用量较上周增长5%，用药指导调用偏低。','在患者首页突出用药指导入口；补齐药房库存预测展示。','知识库命中率较目标低2个百分点。','下周呼吸系统和解热镇痛药需求上升。','AI运营报告',NOW() - INTERVAL '2 day'),
(3,'月报',CURRENT_DATE - INTERVAL '30 day',CURRENT_DATE,'本月AI质检、危急值预警和随访模块完成演示闭环。','{"qualityPassRate":"86.0%","followUpCompleteRate":"67%","criticalProcessRate":"100%"}','随访完成率仍有提升空间。','增加随访提醒频次和异常反馈提示；优化医生端弹窗提醒。','部分检验报告仍需人工复核。','预计下月检验解读量增长15%。','AI运营报告',NOW() - INTERVAL '5 day');

INSERT INTO quality_check_record(id, check_type, check_date, checker_type, total_count, pass_count, avg_score, problem_summary, improvement_suggestions, create_time) VALUES
(1,'病历质检',CURRENT_DATE - INTERVAL '1 day','AI',10,8,88.50,'个别病历治疗意见较简单，诊断依据描述不足。','建议医生补充鉴别诊断和复诊条件。',NOW() - INTERVAL '1 day'),
(2,'处方质检',CURRENT_DATE - INTERVAL '2 day','AI',12,10,91.20,'少量处方用法用量不够清晰。','建议处方提交前展示用法完整性提醒。',NOW() - INTERVAL '2 day'),
(3,'病历质检',CURRENT_DATE - INTERVAL '7 day','AI',18,15,86.40,'部分主诉与现病史重复。','建议使用病历生成模板规范结构。',NOW() - INTERVAL '7 day');

INSERT INTO quality_check_detail(id, record_id, target_id, target_type, doctor_id, doctor_name, score, problems, suggestions, status, rectify_remark, create_time) VALUES
(1,1,1,'medical_record',7,'周明川',88,'治疗意见较简略。','补充复诊条件和生活方式建议。','待整改',NULL,NOW() - INTERVAL '1 day'),
(2,1,2,'medical_record',13,'温以宁',92,'无明显问题。','保持病历结构完整。','已通过',NULL,NOW() - INTERVAL '1 day'),
(3,2,3,'prescription',3,'张文',86,'用法说明可进一步细化。','补充饭前饭后及疗程说明。','待整改',NULL,NOW() - INTERVAL '2 day');

INSERT INTO stock_forecast(id, medicine_id, category_id, forecast_type, forecast_period, forecast_data, total_forecast_amount, total_purchase_amount, purchase_suggestions, factors, raw_response, create_time) VALUES
(1,7,2,'月度预测','未来30天','{"daily":[3,4,3,5],"trend":"上升"}',120,180,'复方甘草片库存偏低，建议采购180瓶。','呼吸道感染就诊量上升，季节因素明显。','AI库存预测',NOW()),
(2,14,4,'月度预测','未来30天','{"daily":[1,2,1,2],"trend":"平稳"}',48,80,'缬沙坦胶囊库存偏低，建议采购80盒。','慢病复诊稳定，现有库存不足。','AI库存预测',NOW()),
(3,25,6,'月度预测','未来30天','{"daily":[1,1,2,1],"trend":"平稳"}',35,60,'云南白药气雾剂库存偏低，建议采购60盒。','运动损伤和外伤用药稳定。','AI库存预测',NOW()),
(4,4,5,'月度预测','未来30天','{"daily":[4,5,6,5],"trend":"上升"}',160,120,'布洛芬库存偏低，建议采购120盒。','发热患者增加。','AI库存预测',NOW());

INSERT INTO prescription_ai_review(id, prescription_id, patient_id, patient_gender, patient_age, patient_weight, doctor_id, drugs_json, review_result, review_score, drug_interactions, allergy_risks, dosage_issues, warnings, suggestions, raw_response, review_time) VALUES
(1,1,1,'女',22,52,7,'[{"name":"氨溴索口服液"},{"name":"蒲地蓝消炎口服液"},{"name":"对乙酰氨基酚片"}]','pass',93,'未发现明显相互作用。','未命中已知过敏史。','剂量在常规范围内。','避免与含酒精制剂同服。','按医嘱服用，症状加重及时复诊。','AI处方审核',NOW() - INTERVAL '8 day'),
(2,4,4,'男',46,74,11,'[{"name":"头孢克肟分散片"},{"name":"布洛芬缓释胶囊"}]','warning',78,'无严重相互作用。','需确认头孢类过敏史。','布洛芬胃肠刺激风险。','胃病患者慎用布洛芬。','药师发药前核对过敏史。','AI处方审核',NOW() - INTERVAL '1 day');

SELECT setval('admin_id_seq', (SELECT COALESCE(MAX(id),1) FROM admin));
SELECT setval('staff_account_id_seq', (SELECT COALESCE(MAX(id),1) FROM staff_account));
SELECT setval('department_id_seq', (SELECT COALESCE(MAX(id),1) FROM department));
SELECT setval('doctor_id_seq', (SELECT COALESCE(MAX(id),1) FROM doctor));
SELECT setval('patient_id_seq', (SELECT COALESCE(MAX(id),1) FROM patient));
SELECT setval('medicine_category_id_seq', (SELECT COALESCE(MAX(id),1) FROM medicine_category));
SELECT setval('medicine_id_seq', (SELECT COALESCE(MAX(id),1) FROM medicine));
SELECT setval('examination_item_id_seq', (SELECT COALESCE(MAX(id),1) FROM examination_item));
SELECT setval('registration_id_seq', (SELECT COALESCE(MAX(id),1) FROM registration));
SELECT setval('medical_record_id_seq', (SELECT COALESCE(MAX(id),1) FROM medical_record));
SELECT setval('prescription_id_seq', (SELECT COALESCE(MAX(id),1) FROM prescription));
SELECT setval('examination_id_seq', (SELECT COALESCE(MAX(id),1) FROM examination));
SELECT setval('examination_ai_interpretation_id_seq', (SELECT COALESCE(MAX(id),1) FROM examination_ai_interpretation));
SELECT setval('critical_value_warning_id_seq', (SELECT COALESCE(MAX(id),1) FROM critical_value_warning));
SELECT setval('examination_ai_review_id_seq', (SELECT COALESCE(MAX(id),1) FROM examination_ai_review));
SELECT setval('medication_guide_id_seq', (SELECT COALESCE(MAX(id),1) FROM medication_guide));
SELECT setval('follow_up_plan_id_seq', (SELECT COALESCE(MAX(id),1) FROM follow_up_plan));
SELECT setval('follow_up_record_id_seq', (SELECT COALESCE(MAX(id),1) FROM follow_up_record));
SELECT setval('ai_knowledge_base_id_seq', (SELECT COALESCE(MAX(id),1) FROM ai_knowledge_base));
SELECT setval('ai_chat_record_id_seq', (SELECT COALESCE(MAX(id),1) FROM ai_chat_record));
SELECT setval('triage_record_id_seq', (SELECT COALESCE(MAX(id),1) FROM triage_record));
SELECT setval('operation_ai_report_id_seq', (SELECT COALESCE(MAX(id),1) FROM operation_ai_report));
SELECT setval('quality_check_record_id_seq', (SELECT COALESCE(MAX(id),1) FROM quality_check_record));
SELECT setval('quality_check_detail_id_seq', (SELECT COALESCE(MAX(id),1) FROM quality_check_detail));
SELECT setval('stock_forecast_id_seq', (SELECT COALESCE(MAX(id),1) FROM stock_forecast));
SELECT setval('prescription_ai_review_id_seq', (SELECT COALESCE(MAX(id),1) FROM prescription_ai_review));

SELECT 'seed completed' AS result;
