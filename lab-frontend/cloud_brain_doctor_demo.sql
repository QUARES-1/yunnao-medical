-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: cloud_brain_doctor_demo
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'2026-06-20 18:44:10.373793','系统管理员','$2a$10$G/ulDUk8ZzU1GTQGNcQKG.csQ7ogYeWy82E1boGzbTvuP1roPNlwu','admin');
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,NULL,'诊治脑血管疾病、偏头痛、失眠等神经系统疾病','神经内科',1,NULL),(2,NULL,'诊治脑肿瘤、脑外伤等需手术的神经系统疾病','神经外科',2,NULL),(3,NULL,'诊治抑郁症、焦虑症等精神心理疾病','精神科',3,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor`
--

LOCK TABLES `doctor` WRITE;
/*!40000 ALTER TABLE `doctor` DISABLE KEYS */;
INSERT INTO `doctor` VALUES (1,NULL,'2026-06-19 22:52:34.032731',1,'神经内科','专注神经内科常见病与疑难病诊疗','张医生','$2a$10$e9Nfvjmov/cmnAmP77No9uj.qocCHPSw3.fWHQ7xWheQ.PKXymQQi','13555853258','脑血管疾病、偏头痛、失眠','主任医师','2026-06-20 12:42:27.384984','doctor01'),(2,NULL,'2026-06-20 11:56:40.000000',1,'神经内科',NULL,'李医生','$2a$10$2jrZgx8zEV/chKKW8XAlhOTnq1P4XOLqxjGDhSWsaTpd3HxIxQ5lq','13800000002',NULL,'副主任医师','2026-06-20 12:23:12.895192','doctor02');
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `examination`
--

LOCK TABLES `examination` WRITE;
/*!40000 ALTER TABLE `examination` DISABLE KEYS */;
INSERT INTO `examination` VALUES (1,NULL,'2026-06-19 22:54:28.727164',1,1,'张医生',1,'头部CT',1,'张明',1,NULL,NULL,'待检查','检查'),(2,NULL,'2026-06-19 22:56:30.539424',1,1,'张医生',1,'头部CT',2,'李秀英',2,NULL,NULL,'待检查','检查'),(3,NULL,'2026-06-19 22:56:44.537113',1,1,'张医生',2,'脑部MRI',2,'李秀英',2,NULL,NULL,'待检查','检查'),(4,NULL,'2026-06-19 23:14:54.706615',1,1,'张医生',1,'头部CT',2,'李秀英',2,NULL,NULL,'待检查','检查'),(5,NULL,'2026-06-19 23:14:58.761752',1,1,'张医生',5,'血常规',2,'李秀英',2,NULL,NULL,'待检查','检验'),(6,NULL,'2026-06-19 23:15:03.635239',1,1,'张医生',7,'血脂四项',2,'李秀英',2,NULL,NULL,'待检查','检验'),(7,NULL,'2026-06-20 12:21:28.476485',1,1,'张医生',8,'血糖',2,'李秀英',2,NULL,NULL,'已撤销','检验');
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `examination_item`
--

LOCK TABLES `examination_item` WRITE;
/*!40000 ALTER TABLE `examination_item` DISABLE KEYS */;
INSERT INTO `examination_item` VALUES (1,'通过CT扫描检查脑部结构，用于排查脑出血、脑肿瘤等','头部CT',280.00,'检查'),(2,'磁共振成像，更清晰地显示脑组织细节','脑部MRI',680.00,'检查'),(3,'记录脑电活动，用于癫痫等疾病诊断','脑电图',150.00,'检查'),(4,'检测脑血管血流速度，评估脑血管状况','经颅多普勒',200.00,'检查'),(5,'检测血液中红细胞、白细胞、血小板等指标','血常规',35.00,'检验'),(6,'检测转氨酶、胆红素等肝功能指标','肝功能',80.00,'检验'),(7,'检测总胆固醇、甘油三酯、高密度脂蛋白、低密度脂蛋白','血脂四项',120.00,'检验'),(8,'检测空腹血糖水平，筛查糖尿病','血糖',25.00,'检验');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_record`
--

LOCK TABLES `medical_record` WRITE;
/*!40000 ALTER TABLE `medical_record` DISABLE KEYS */;
INSERT INTO `medical_record` VALUES (1,'头痛3天，加重1天',NULL,1,'偏头痛',1,'doctor01','高血压病史5年，规律服用降压药',1,'张明','T:36.5℃，P:78次/分，R:18次/分，BP:145/90mmHg，神志清楚，双侧瞳孔等大等圆，对光反射灵敏','患者3天前无明显诱因出现头痛，为持续性胀痛，以双侧颞部为主，1天前症状加重，伴恶心，无呕吐，无发热',4,'1.布洛芬缓释胶囊 止痛\n2.盐酸氟桂利嗪胶囊 改善脑血管\n3.注意休息，避免劳累\n4.一周后复查',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medicine`
--

LOCK TABLES `medicine` WRITE;
/*!40000 ALTER TABLE `medicine` DISABLE KEYS */;
INSERT INTO `medicine` VALUES (1,NULL,NULL,NULL,'广谱抗生素，用于敏感菌引起的感染','华北制药','阿莫西林胶囊',15.50,'0.5g*24粒',100,'盒'),(2,NULL,NULL,NULL,'解热镇痛抗炎药，用于头痛、牙痛、关节痛','中美史克','布洛芬缓释胶囊',22.00,'0.3g*20粒',80,'盒'),(3,NULL,NULL,NULL,'钙离子拮抗剂，用于偏头痛、眩晕','西安杨森','盐酸氟桂利嗪胶囊',28.00,'5mg*20粒',59,'盒'),(4,NULL,NULL,NULL,'营养神经药，用于周围神经病变','卫材中国','甲钴胺片',35.00,'0.5mg*36片',50,'盒'),(5,NULL,NULL,NULL,'抗精神病药，用于精神分裂症','礼来制药','奥氮平片',89.00,'5mg*7片',30,'盒'),(6,NULL,NULL,NULL,'抗抑郁药，用于抑郁症、强迫症','辉瑞制药','盐酸舍曲林片',75.00,'50mg*14片',40,'盒'),(7,NULL,NULL,NULL,'改善脑部血液循环，用于脑血管疾病','德国威玛','银杏叶提取物片',42.00,'40mg*30片',69,'盒'),(8,NULL,NULL,NULL,'抗癫痫药，用于癫痫、三叉神经痛','上海三维','卡马西平片',18.00,'0.1g*100片',60,'瓶'),(9,NULL,NULL,NULL,'钙通道阻滞剂，用于蛛网膜下腔出血后脑血管痉挛','拜耳医药','尼莫地平片',25.00,'20mg*50片',54,'瓶'),(10,NULL,NULL,NULL,'镇静催眠药，用于失眠、焦虑','华海药业','艾司唑仑片',12.00,'1mg*20片',45,'盒');
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
  `allergy_history` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_91awipe9r3hg4y60m38qb06ql` (`openid`),
  UNIQUE KEY `UK_9gxe97j2ngjjvtkig6b6jvy91` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient`
--

LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
INSERT INTO `patient` VALUES (1,NULL,45,NULL,NULL,'男',NULL,'张明',NULL,'13800138001',NULL,'青霉素过敏'),(2,NULL,62,NULL,NULL,'女',NULL,'李秀英',NULL,'13900139002',NULL,'无已知过敏史'),(3,NULL,35,NULL,NULL,'男',NULL,'王强',NULL,'13700137003',NULL,'海鲜过敏');
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescription`
--

LOCK TABLES `prescription` WRITE;
/*!40000 ALTER TABLE `prescription` DISABLE KEYS */;
INSERT INTO `prescription` VALUES (1,'2026-06-20 12:21:28.555543',1,NULL,1,'张医生','[{\"unit\": \"盒\", \"dosage\": \"每次1粒，每日2次\", \"quantity\": 1, \"medicineId\": 1, \"medicineName\": \"阿莫西林胶囊\", \"specification\": \"0.5g*24粒\"}]',2,'李秀英',2,'已撤销',15.50),(2,'2026-06-20 12:22:26.809191',1,NULL,1,'张医生','[{\"unit\": \"盒\", \"dosage\": \"每次1粒，每日2次，饭后服用\", \"quantity\": 2, \"medicineId\": 2, \"medicineName\": \"布洛芬缓释胶囊\", \"specification\": \"0.3g*20粒\"}]',2,'李秀英',2,'已撤销',44.00),(3,'2026-06-20 12:30:00.208942',1,NULL,1,'张医生','[{\"medicineId\":7,\"medicineName\":\"银杏叶提取物片\",\"specification\":\"40mg*30片\",\"quantity\":1,\"unit\":\"盒\",\"dosage\":\"1\"},{\"medicineId\":9,\"medicineName\":\"尼莫地平片\",\"specification\":\"20mg*50片\",\"quantity\":1,\"unit\":\"瓶\",\"dosage\":\"2\"},{\"medicineId\":3,\"medicineName\":\"盐酸氟桂利嗪胶囊\",\"specification\":\"5mg*20粒\",\"quantity\":1,\"unit\":\"盒\",\"dosage\":\"3\"}]',2,'李秀英',2,'待发药',95.00);
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration`
--

LOCK TABLES `registration` WRITE;
/*!40000 ALTER TABLE `registration` DISABLE KEYS */;
INSERT INTO `registration` VALUES (1,NULL,1,'神经内科',1,'doctor01',1,'张明','2026-06-20','已就诊','上午','2026-06-19 22:55:21.473736'),(2,NULL,1,'神经内科',1,'doctor01',2,'李秀英','2026-06-20','就诊中','上午','2026-06-19 22:55:47.988549'),(3,NULL,3,'精神科',1,'doctor01',3,'王强','2026-06-20','待就诊','下午',NULL),(4,NULL,1,'神经内科',1,'doctor01',1,'张明','2026-06-18','已就诊','上午',NULL),(5,'2026-06-20 11:56:40.000000',1,'神经内科',2,'李医生',1,'张明','2026-06-20','待就诊','下午','2026-06-20 11:56:40.000000');
/*!40000 ALTER TABLE `registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff_account`
--

DROP TABLE IF EXISTS `staff_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_78utkofcmp3jsi7dmuvh3jvsg` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_account`
--

LOCK TABLES `staff_account` WRITE;
/*!40000 ALTER TABLE `staff_account` DISABLE KEYS */;
INSERT INTO `staff_account` VALUES (1,'2026-06-20 23:23:25.679618',_binary '','药房一号窗口','$2a$10$Qs8oFiNiagQ4tqD9hSDb1e168eWZagIzRUAwfqLtalpUwwBm59JO2','13800000001','pharmacy','pharmacy01');
/*!40000 ALTER TABLE `staff_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'cloud_brain_doctor_demo'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-20 23:32:14
