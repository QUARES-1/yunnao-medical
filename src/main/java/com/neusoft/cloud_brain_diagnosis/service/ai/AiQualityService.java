package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiQualityService {
    /**
     * 发起AI质检
     */
    Map<String, Object> startQualityCheck(String checkType, Integer sampleSize);

    /**
     * 质检记录列表
     */
    Page<QualityCheckRecord> getCheckList(Integer page, Integer size);

    /**
     * 质检详情
     */
    QualityCheckRecord getCheckDetail(Long id);

    /**
     * 问题明细列表
     */
    Page<QualityCheckDetail> getCheckDetails(Long recordId, Integer page, Integer size);

    /**
     * 医生质检统计
     */
    Map<String, Object> getDoctorStats();

    /**
     * 医生查看我的问题
     */
    Page<QualityCheckDetail> getMyQualityList(Long doctorId, Integer page, Integer size);

    /**
     * 提交整改
     */
    String rectify(Long id, String remark, Long doctorId);
}
