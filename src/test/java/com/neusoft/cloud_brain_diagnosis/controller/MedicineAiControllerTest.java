package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.feign.AiStockFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = MedicineAiController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class MedicineAiControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiStockFeignClient stockFeignClient;
    @MockBean private AiStockService stockService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());
    }

    // ========== stock forecast ==========

    @Test
    void generateForecast_ShouldReturnResult() throws Exception {
        when(stockService.generateStockForecast(anyString(), any()))
                .thenReturn(Map.of("id", 1L, "forecastType", "monthly"));

        mockMvc.perform(post("/api/medicine/ai/stock-forecast/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forecastType\":\"monthly\",\"forecastPeriod\":\"2026-01\"}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.forecastType").value("monthly"));
    }

    @Test
    void generateForecast_ShouldUseDefaultType_WhenNotProvided() throws Exception {
        when(stockService.generateStockForecast(eq("monthly"), isNull()))
                .thenReturn(Map.of("id", 1L));

        mockMvc.perform(post("/api/medicine/ai/stock-forecast/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getForecastDetail_ShouldReturnForecast() throws Exception {
        StockForecast forecast = new StockForecast();
        forecast.setId(1L);
        forecast.setForecastType("monthly");

        when(stockService.getForecastDetail(1L)).thenReturn(forecast);

        mockMvc.perform(get("/api/medicine/ai/stock-forecast/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getForecastList_ShouldReturnPage() throws Exception {
        StockForecast forecast = new StockForecast();
        forecast.setId(1L);

        when(stockService.getForecastList(isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(forecast)));

        mockMvc.perform(get("/api/medicine/ai/stock-forecast/list")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getForecastList_ShouldFilterByType() throws Exception {
        StockForecast forecast = new StockForecast();
        forecast.setId(1L);
        forecast.setForecastType("weekly");

        when(stockService.getForecastList(eq("weekly"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(forecast)));

        mockMvc.perform(get("/api/medicine/ai/stock-forecast/list")
                        .param("forecastType", "weekly")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].forecastType").value("weekly"));
    }
}
