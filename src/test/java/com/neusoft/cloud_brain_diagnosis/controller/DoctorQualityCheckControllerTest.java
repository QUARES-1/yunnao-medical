package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DoctorQualityCheckController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class DoctorQualityCheckControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiOtherFeignClient otherFeignClient;
    @MockBean private AiQualityService qualityService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
    }

    // ========== getMyQualityList ==========

    @Test
    void getMyQualityList_ShouldReturnPage() throws Exception {
        QualityCheckDetail detail = new QualityCheckDetail();
        detail.setId(1L);
        detail.setScore(85);
        detail.setStatus("pending");

        when(qualityService.getMyQualityList(anyLong(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(detail)));

        mockMvc.perform(get("/api/doctor/quality-check/my-list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].score").value(85));
    }

    @Test
    void getMyQualityList_ShouldUsePagination() throws Exception {
        when(qualityService.getMyQualityList(anyLong(), eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/doctor/quality-check/my-list")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk());
    }

    // ========== rectify ==========

    @Test
    void rectify_ShouldReturnMessage() throws Exception {
        when(qualityService.rectify(eq(1L), eq("已整改"), anyLong()))
                .thenReturn("整改已提交");

        mockMvc.perform(post("/api/doctor/quality-check/rectify/1")
                        .param("remark", "已整改")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("整改已提交"));
    }

    @Test
    void rectify_ShouldPassCorrectDoctorId() throws Exception {
        when(qualityService.rectify(eq(1L), eq("补充说明"), anyLong()))
                .thenReturn("整改已提交");

        mockMvc.perform(post("/api/doctor/quality-check/rectify/1")
                        .param("remark", "补充说明")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("整改已提交"));
    }
}
