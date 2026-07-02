package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(stockFeignClient.generateForecast(anyMap())).thenAnswer(inv -> {
            Map<String, Object> request = inv.getArgument(0);
            String forecastType = String.valueOf(request.getOrDefault("forecastType", "monthly"));
            Object period = request.get("forecastPeriod");
            return success(stockService.generateStockForecast(forecastType, period == null ? null : String.valueOf(period)));
        });
        when(stockFeignClient.getForecastDetail(anyLong()))
                .thenAnswer(inv -> success(stockService.getForecastDetail(inv.getArgument(0))));
        when(stockFeignClient.getForecastList(nullable(String.class), anyInt(), anyInt()))
                .thenAnswer(inv -> success(stockService.getForecastList(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result<Map<String, Object>> success(Object data) {
        return (Result) Result.success(data);
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

    @Test
    void buildStreamForecastText_ShouldSummarizeForecastRows() {
        MedicineAiController controller = new MedicineAiController(stockFeignClient);
        Map<String, Object> data = Map.of(
                "forecastPeriod", "2026-07",
                "forecastData", """
                        [
                          {"name":"A","riskLevel":"high","suggestPurchase":10,"unit":"box","currentStock":2},
                          {"name":"B","riskLevel":"normal","suggestPurchase":0,"unit":"box","currentStock":20},
                          "ignored-row"
                        ]
                        """
        );

        String text = ReflectionTestUtils.invokeMethod(controller, "buildStreamForecastText", data);

        assertNotNull(text);
        assertTrue(text.contains("2026-07"));
        assertTrue(text.contains("A"));
    }

    @Test
    void buildStreamForecastText_ShouldHandleInvalidForecastJson() {
        MedicineAiController controller = new MedicineAiController(stockFeignClient);
        Map<String, Object> data = Map.of("forecastData", "not-json");

        String text = ReflectionTestUtils.invokeMethod(controller, "buildStreamForecastText", data);

        assertNotNull(text);
        assertTrue(text.length() > 0);
    }

    @Test
    void streamForecast_ShouldReturnEmitter_WhenMedicinesAlreadyProvided() {
        MedicineAiController controller = new MedicineAiController(stockFeignClient);

        SseEmitter emitter = controller.streamForecast(Map.of(
                "forecastPeriod", "2026-07",
                "medicines", List.of(Map.of("name", "A", "suggestPurchase", 1))
        ));

        assertNotNull(emitter);
    }

    @Test
    void streamForecast_ShouldReturnEmitter_WhenGeneratedDataIsNull() {
        MedicineAiController controller = new MedicineAiController(stockFeignClient);
        when(stockFeignClient.generateForecast(anyMap())).thenReturn(Result.success(null));

        SseEmitter emitter = controller.streamForecast(Map.of("forecastType", "monthly"));

        assertNotNull(emitter);
    }

    @Test
    void streamForecast_ShouldReturnEmitter_WhenFeignThrows() {
        MedicineAiController controller = new MedicineAiController(stockFeignClient);
        when(stockFeignClient.generateForecast(anyMap())).thenThrow(new RuntimeException("service down"));

        assertDoesNotThrow(() -> controller.streamForecast(Map.of("forecastType", "monthly")));
    }
}
