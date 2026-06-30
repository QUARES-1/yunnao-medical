package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.QualityCheckDetail;
import com.neusoft.ai.entity.QualityCheckRecord;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiQualityService {
    Map<String, Object> startQualityCheck(String checkType, Integer sampleSize);
    Page<QualityCheckRecord> getCheckList(Integer page, Integer size);
    QualityCheckRecord getCheckDetail(Long id);
    Page<QualityCheckDetail> getCheckDetails(Long recordId, Integer page, Integer size);
    Map<String, Object> getDoctorStats();
    Page<QualityCheckDetail> getMyQualityList(Long doctorId, Integer page, Integer size);
    String rectify(Long id, String remark, Long doctorId);
}
