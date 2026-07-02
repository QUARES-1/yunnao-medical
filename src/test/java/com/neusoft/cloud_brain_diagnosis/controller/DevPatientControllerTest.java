package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DevPatientController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
@ActiveProfiles("dev")
class DevPatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PatientRepository patientRepository;
    @MockBean private JwtUtil jwtUtil;

    @Test
    void patientLogin_ShouldReturnExistingPatient() throws Exception {
        Patient existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setOpenid("dev-tourist-patient");
        existingPatient.setName("高同学");
        existingPatient.setPhone("13800002026");
        existingPatient.setGender("女");
        existingPatient.setAge(20);

        when(patientRepository.findByOpenid("dev-tourist-patient"))
                .thenReturn(Optional.of(existingPatient));
        when(jwtUtil.generateToken(eq(1L), eq(RoleEnum.PATIENT.getCode())))
                .thenReturn("dev-token-12345");

        mockMvc.perform(post("/api/dev/patient-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("dev-token-12345"))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.name").value("高同学"))
                .andExpect(jsonPath("$.data.phone").value("13800002026"));
    }

    @Test
    void patientLogin_ShouldCreatePatient_WhenNotExists() throws Exception {
        when(patientRepository.findByOpenid("dev-tourist-patient"))
                .thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(2L);
            return patient;
        });
        when(jwtUtil.generateToken(eq(2L), eq(RoleEnum.PATIENT.getCode())))
                .thenReturn("dev-token-67890");

        mockMvc.perform(post("/api/dev/patient-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("dev-token-67890"))
                .andExpect(jsonPath("$.data.patientId").value(2))
                .andExpect(jsonPath("$.data.name").value("高同学"))
                .andExpect(jsonPath("$.data.needCompleteInfo").value(false));

        verify(patientRepository).save(any(Patient.class));
    }
}
