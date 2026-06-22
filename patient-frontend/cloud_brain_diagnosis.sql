-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: cloud_brain_diagnosis
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gfn44sntic2k93auag97juyij` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort` int DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1t68827l97cwyxo9r1u6t4p7d` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,'2026-06-19 12:38:48.000000','心脏及血管疾病诊疗','心血管内科',1,'2026-06-19 12:38:48.000000'),(2,'2026-06-19 12:38:48.000000','呼吸系统常见病诊疗','呼吸内科',2,'2026-06-19 12:38:48.000000'),(3,'2026-06-19 12:38:48.000000','儿童常见病与生长发育','儿科',3,'2026-06-19 12:38:48.000000'),(4,'2026-06-19 12:38:48.000000','骨关节与运动损伤诊疗','骨科',4,'2026-06-19 12:38:48.000000'),(5,'2026-06-19 12:38:48.000000','眼部疾病与视力健康','眼科',5,'2026-06-19 12:38:48.000000'),(6,'2026-06-19 17:12:48.000000','胃肠、肝胆胰等消化系统疾病诊疗','消化内科',6,'2026-06-19 17:12:48.000000'),(7,'2026-06-19 17:12:48.000000','脑血管及神经系统疾病诊疗','神经内科',7,'2026-06-19 17:12:48.000000'),(8,'2026-06-19 17:12:48.000000','糖尿病、甲状腺及代谢疾病诊疗','内分泌科',8,'2026-06-19 17:12:48.000000'),(9,'2026-06-19 17:12:48.000000','常见皮肤病与医学护肤指导','皮肤科',9,'2026-06-19 17:12:48.000000'),(10,'2026-06-19 17:12:48.000000','耳鼻咽喉常见病与听力健康','耳鼻喉科',10,'2026-06-19 17:12:48.000000'),(11,'2026-06-19 17:12:48.000000','女性常见病与健康管理','妇科',11,'2026-06-19 17:12:48.000000'),(12,'2026-06-19 17:12:48.000000','常见病初诊、慢病管理与健康咨询','全科医学科',12,'2026-06-19 17:12:48.000000');
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `doctor`
--

DROP TABLE IF EXISTS `doctor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `department_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `introduction` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `specialty` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `username` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ctspax8ladjoxfko6qkhxwmpb` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor`
--

LOCK TABLES `doctor` WRITE;
/*!40000 ALTER TABLE `doctor` DISABLE KEYS */;
INSERT INTO `doctor` VALUES (1,NULL,'2026-06-19 12:38:48.000000',1,'心血管内科','从事心血管疾病临床诊疗二十余年，注重个体化诊疗与长期健康管理。','林知远','123456','13800000001','高血压、冠心病、心律失常','主任医师','2026-06-19 12:38:48.000000','doctor_lin'),(2,NULL,'2026-06-19 12:38:48.000000',2,'呼吸内科','擅长呼吸系统常见病及慢性疾病规范化管理。','周明川','123456','13800000002','哮喘、慢阻肺、肺部感染','副主任医师','2026-06-19 12:38:48.000000','doctor_zhou'),(3,NULL,'2026-06-19 12:38:48.000000',3,'儿科','关注儿童身心健康，为家庭提供温暖细致的诊疗服务。','苏清禾','123456','13800000003','儿童呼吸道疾病、消化系统疾病','主治医师','2026-06-19 12:38:48.000000','doctor_su'),(4,NULL,'2026-06-19 12:38:48.000000',4,'骨科','专注骨关节疾病和运动损伤的综合治疗与康复指导。','陈屿安','123456','13800000004','颈肩腰腿痛、运动损伤、骨关节病','副主任医师','2026-06-19 12:38:48.000000','doctor_chen'),(5,NULL,'2026-06-19 12:38:48.000000',5,'眼科','长期从事眼科临床与视力健康管理工作。','顾云舒','123456','13800000005','屈光不正、干眼症、白内障','主任医师','2026-06-19 12:38:48.000000','doctor_gu'),(6,NULL,'2026-06-19 17:12:48.000000',1,'心血管内科','专注心血管慢病管理及危险因素综合干预。','沈砚舟','123456','13800000101','高血压、心力衰竭、冠心病','副主任医师','2026-06-19 17:12:48.000000','doctor_shen_yz'),(7,NULL,'2026-06-19 17:12:48.000000',1,'心血管内科','重视循证诊疗与患者长期随访。','许安宁','123456','13800000102','心律失常、胸痛、血脂异常','主治医师','2026-06-19 17:12:48.000000','doctor_xu_an'),(8,NULL,'2026-06-19 17:12:48.000000',2,'呼吸内科','从事呼吸与危重症临床工作二十余年。','程景行','123456','13800000201','慢性咳嗽、肺结节、肺部感染','主任医师','2026-06-19 17:12:48.000000','doctor_cheng_jx'),(9,NULL,'2026-06-19 17:12:48.000000',2,'呼吸内科','擅长呼吸慢病规范化诊疗及健康教育。','温以宁','123456','13800000202','支气管哮喘、慢阻肺、过敏性咳嗽','主治医师','2026-06-19 17:12:48.000000','doctor_wen_yn'),(10,NULL,'2026-06-19 17:12:48.000000',3,'儿科','关注儿童常见病、营养与生长发育。','唐星野','123456','13800000301','儿童发热、呼吸道感染、生长发育','副主任医师','2026-06-19 17:12:48.000000','doctor_tang_xy'),(11,NULL,'2026-06-19 17:12:48.000000',3,'儿科','以耐心沟通和家庭参与为诊疗特色。','叶童安','123456','13800000302','小儿消化不良、过敏、儿童保健','主治医师','2026-06-19 17:12:48.000000','doctor_ye_ta'),(12,NULL,'2026-06-19 17:12:48.000000',4,'骨科','擅长脊柱与关节疾病的综合治疗。','贺知衡','123456','13800000401','腰椎间盘突出、关节炎、骨质疏松','主任医师','2026-06-19 17:12:48.000000','doctor_he_zh'),(13,NULL,'2026-06-19 17:12:48.000000',4,'骨科','专注运动损伤和术后康复指导。','陆闻川','123456','13800000402','肩膝损伤、骨折、运动康复','主治医师','2026-06-19 17:12:48.000000','doctor_lu_wc'),(14,NULL,'2026-06-19 17:12:48.000000',5,'眼科','长期从事眼表疾病与屈光健康诊疗。','江望舒','123456','13800000501','近视防控、干眼症、角膜疾病','副主任医师','2026-06-19 17:12:48.000000','doctor_jiang_ws'),(15,NULL,'2026-06-19 17:12:48.000000',5,'眼科','注重青少年视力管理与用眼指导。','白清越','123456','13800000502','儿童近视、结膜炎、视疲劳','主治医师','2026-06-19 17:12:48.000000','doctor_bai_qy'),(16,NULL,'2026-06-19 17:12:48.000000',6,'消化内科','从事消化系统疾病临床与内镜诊疗。','谢怀瑾','123456','13800000601','胃炎、消化性溃疡、肝功能异常','主任医师','2026-06-19 17:12:48.000000','doctor_xie_hj'),(17,NULL,'2026-06-19 17:12:48.000000',6,'消化内科','重视消化慢病管理与生活方式干预。','纪南乔','123456','13800000602','反流性食管炎、脂肪肝、肠易激综合征','副主任医师','2026-06-19 17:12:48.000000','doctor_ji_nq'),(18,NULL,'2026-06-19 17:12:48.000000',6,'消化内科','擅长常见消化系统症状的规范评估。','孟书言','123456','13800000603','腹痛、腹胀、便秘、幽门螺杆菌感染','主治医师','2026-06-19 17:12:48.000000','doctor_meng_sy'),(19,NULL,'2026-06-19 17:12:48.000000',7,'神经内科','专注脑血管病及神经退行性疾病诊疗。','宋闻溪','123456','13800000701','脑卒中、帕金森病、认知障碍','主任医师','2026-06-19 17:12:48.000000','doctor_song_wx'),(20,NULL,'2026-06-19 17:12:48.000000',7,'神经内科','擅长头痛眩晕及睡眠相关疾病诊治。','季临川','123456','13800000702','偏头痛、眩晕、失眠','副主任医师','2026-06-19 17:12:48.000000','doctor_ji_lc'),(21,NULL,'2026-06-19 17:12:48.000000',7,'神经内科','关注神经系统疾病早期识别与康复管理。','顾言清','123456','13800000703','周围神经病、癫痫、面神经炎','主治医师','2026-06-19 17:12:48.000000','doctor_gu_yq'),(22,NULL,'2026-06-19 17:12:48.000000',8,'内分泌科','长期从事内分泌代谢疾病临床诊疗。','俞静姝','123456','13800000801','糖尿病、甲状腺疾病、肥胖症','主任医师','2026-06-19 17:12:48.000000','doctor_yu_js'),(23,NULL,'2026-06-19 17:12:48.000000',8,'内分泌科','擅长代谢综合征与个体化体重管理。','周予安','123456','13800000802','血糖异常、高尿酸血症、骨质疏松','副主任医师','2026-06-19 17:12:48.000000','doctor_zhou_ya'),(24,NULL,'2026-06-19 17:12:48.000000',8,'内分泌科','注重慢病随访与生活方式指导。','林霁月','123456','13800000803','妊娠期糖尿病、甲状腺结节、血脂异常','主治医师','2026-06-19 17:12:48.000000','doctor_lin_jy'),(25,NULL,'2026-06-19 17:12:48.000000',9,'皮肤科','从事皮肤病与医学美容临床工作多年。','段清和','123456','13800000901','湿疹、银屑病、痤疮','主任医师','2026-06-19 17:12:48.000000','doctor_duan_qh'),(26,NULL,'2026-06-19 17:12:48.000000',9,'皮肤科','擅长敏感肌和色素性皮肤病管理。','夏若岚','123456','13800000902','皮炎、荨麻疹、色素性疾病','副主任医师','2026-06-19 17:12:48.000000','doctor_xia_rl'),(27,NULL,'2026-06-19 17:12:48.000000',9,'皮肤科','关注皮肤屏障修复及科学护肤。','黎初晴','123456','13800000903','脱发、真菌感染、儿童皮肤病','主治医师','2026-06-19 17:12:48.000000','doctor_li_cq'),(28,NULL,'2026-06-19 17:12:48.000000',10,'耳鼻喉科','擅长耳鼻咽喉常见病及疑难病诊治。','秦朗川','123456','13800001001','鼻窦炎、中耳炎、咽喉疾病','主任医师','2026-06-19 17:12:48.000000','doctor_qin_lc'),(29,NULL,'2026-06-19 17:12:48.000000',10,'耳鼻喉科','专注听力障碍与眩晕疾病诊疗。','傅听澜','123456','13800001002','耳鸣、听力下降、前庭性眩晕','副主任医师','2026-06-19 17:12:48.000000','doctor_fu_tl'),(30,NULL,'2026-06-19 17:12:48.000000',10,'耳鼻喉科','重视儿童耳鼻喉疾病的早期干预。','安以沫','123456','13800001003','过敏性鼻炎、腺样体肥大、扁桃体炎','主治医师','2026-06-19 17:12:48.000000','doctor_an_ym'),(31,NULL,'2026-06-19 17:12:48.000000',11,'妇科','从事妇科常见病及女性健康管理。','姜云岫','123456','13800001101','妇科炎症、月经异常、围绝经期管理','主任医师','2026-06-19 17:12:48.000000','doctor_jiang_yx'),(32,NULL,'2026-06-19 17:12:48.000000',11,'妇科','注重女性全生命周期健康服务。','罗知微','123456','13800001102','宫颈疾病、子宫肌瘤、内分泌调理','副主任医师','2026-06-19 17:12:48.000000','doctor_luo_zw'),(33,NULL,'2026-06-19 17:12:48.000000',11,'妇科','擅长常见妇科问题与健康咨询。','裴晚宁','123456','13800001103','痛经、备孕咨询、妇科体检','主治医师','2026-06-19 17:12:48.000000','doctor_pei_wn'),(34,NULL,'2026-06-19 17:12:48.000000',12,'全科医学科','提供连续、综合、以患者为中心的健康服务。','钟行简','123456','13800001201','常见病初诊、多病共存、慢病管理','主任医师','2026-06-19 17:12:48.000000','doctor_zhong_xj'),(35,NULL,'2026-06-19 17:12:48.000000',12,'全科医学科','擅长健康风险评估与家庭健康管理。','乔一禾','123456','13800001202','健康体检解读、高血压、糖尿病管理','副主任医师','2026-06-19 17:12:48.000000','doctor_qiao_yh'),(36,NULL,'2026-06-19 17:12:48.000000',12,'全科医学科','关注常见症状鉴别与合理转诊。','方时安','123456','13800001203','发热、乏力、头晕、健康咨询','主治医师','2026-06-19 17:12:48.000000','doctor_fang_sa');
/*!40000 ALTER TABLE `doctor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `examination`
--

DROP TABLE IF EXISTS `examination`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `examination` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `complete_time` datetime(6) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `doctor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_id` bigint DEFAULT NULL,
  `patient_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registration_id` bigint DEFAULT NULL,
  `result` text COLLATE utf8mb4_unicode_ci,
  `result_images` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `examination`
--

LOCK TABLES `examination` WRITE;
/*!40000 ALTER TABLE `examination` DISABLE KEYS */;
/*!40000 ALTER TABLE `examination` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `examination_item`
--

DROP TABLE IF EXISTS `examination_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `examination_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `examination_item`
--

LOCK TABLES `examination_item` WRITE;
/*!40000 ALTER TABLE `examination_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `examination_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medical_record`
--

DROP TABLE IF EXISTS `medical_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chief_complaint` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `diagnosis` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `doctor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `past_history` text COLLATE utf8mb4_unicode_ci,
  `patient_id` bigint DEFAULT NULL,
  `patient_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `physical_examination` text COLLATE utf8mb4_unicode_ci,
  `present_illness` text COLLATE utf8mb4_unicode_ci,
  `registration_id` bigint DEFAULT NULL,
  `treatment` text COLLATE utf8mb4_unicode_ci,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_record`
--

LOCK TABLES `medical_record` WRITE;
/*!40000 ALTER TABLE `medical_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `medical_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medicine`
--

DROP TABLE IF EXISTS `medicine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medicine` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint DEFAULT NULL,
  `category_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manufacturer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `specification` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stock` int DEFAULT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medicine`
--

LOCK TABLES `medicine` WRITE;
/*!40000 ALTER TABLE `medicine` DISABLE KEYS */;
/*!40000 ALTER TABLE `medicine` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medicine_category`
--

DROP TABLE IF EXISTS `medicine_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medicine_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medicine_category`
--

LOCK TABLES `medicine_category` WRITE;
/*!40000 ALTER TABLE `medicine_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `medicine_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient`
--

DROP TABLE IF EXISTS `patient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `openid` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_91awipe9r3hg4y60m38qb06ql` (`openid`),
  UNIQUE KEY `UK_9gxe97j2ngjjvtkig6b6jvy91` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient`
--

LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
INSERT INTO `patient` VALUES (1,NULL,20,NULL,'2026-06-19 12:47:42.222587','女',NULL,'高同学','dev-tourist-patient','13800002026','2026-06-19 12:47:42.222587'),(2,NULL,NULL,'/upload/db694e8920814f4fbdb547723759d0a7.jpeg','2026-06-19 14:07:42.958020',NULL,NULL,'高思晗','or88gxji8pye3SuB4of-vMa_FkdE',NULL,'2026-06-19 21:10:03.417222');
/*!40000 ALTER TABLE `patient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prescription`
--

DROP TABLE IF EXISTS `prescription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `dispense_time` datetime(6) DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `doctor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `drugs` text COLLATE utf8mb4_unicode_ci,
  `patient_id` bigint DEFAULT NULL,
  `patient_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registration_id` bigint DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_amount` decimal(38,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescription`
--

LOCK TABLES `prescription` WRITE;
/*!40000 ALTER TABLE `prescription` DISABLE KEYS */;
/*!40000 ALTER TABLE `prescription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registration`
--

DROP TABLE IF EXISTS `registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `department_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `doctor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_id` bigint DEFAULT NULL,
  `patient_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registration_date` date DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time_slot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration`
--

LOCK TABLES `registration` WRITE;
/*!40000 ALTER TABLE `registration` DISABLE KEYS */;
INSERT INTO `registration` VALUES (1,'2026-06-19 14:10:14.650784',1,'心血管内科',1,'林知远',2,'微信用户','2026-06-20','待就诊','下午','2026-06-19 14:10:14.650784'),(2,'2026-06-19 17:25:02.716766',1,'心血管内科',1,'林知远',2,'微信用户','2026-06-19','待就诊','上午','2026-06-19 17:25:02.716766'),(3,'2026-06-19 17:29:20.269127',1,'心血管内科',1,'林知远',2,'微信用户','2026-06-21','已取消','上午','2026-06-19 17:29:28.107465'),(4,'2026-06-19 17:55:25.135169',1,'心血管内科',1,'林知远',2,'微信用户','2026-06-21','已取消','下午','2026-06-19 17:59:13.518130'),(5,'2026-06-19 18:02:39.799314',3,'儿科',3,'苏清禾',2,'微信用户','2026-06-21','已取消','下午','2026-06-19 18:02:48.870640'),(6,'2026-06-19 18:03:07.756741',3,'儿科',3,'苏清禾',2,'微信用户','2026-06-22','已取消','上午','2026-06-19 18:04:19.228165'),(7,'2026-06-19 18:07:04.665135',3,'儿科',3,'苏清禾',2,'微信用户','2026-06-23','已取消','下午','2026-06-19 18:07:20.626961'),(8,'2026-06-19 18:08:48.324918',2,'呼吸内科',9,'温以宁',2,'微信用户','2026-06-22','待就诊','上午','2026-06-19 18:08:48.324918'),(9,'2026-06-19 18:11:01.623985',4,'骨科',4,'陈屿安',2,'微信用户','2026-06-19','待就诊','上午','2026-06-19 18:11:01.623985');
/*!40000 ALTER TABLE `registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'cloud_brain_diagnosis'
--

--
-- Dumping routines for database 'cloud_brain_diagnosis'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-19 21:50:48
