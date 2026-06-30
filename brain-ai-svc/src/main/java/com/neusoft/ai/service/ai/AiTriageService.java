package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.TriageRecord;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiTriageService {
    Map<String, Object> consult(String chiefComplaint, Long patientId);
    Page<TriageRecord> getPatientList(Long patientId, Integer page, Integer size);
    TriageRecord getDetail(Long id);
}
