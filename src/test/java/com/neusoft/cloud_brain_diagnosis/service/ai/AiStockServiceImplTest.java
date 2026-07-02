package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.repository.StockForecastRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiStockServiceImplTest {

    @Mock private StockForecastRepository forecastRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiStockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        stockService = new AiStockServiceImpl(forecastRepository, medicineRepository, prescriptionRepository, aiApiUtil);
    }

    // ========== generateStockForecast() ==========

    @Test
    void generateStockForecast_ShouldGenerateForecast_WithMedicines() {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("阿莫西林");
        medicine.setSpecification("500mg");
        medicine.setCategoryName("抗生素");
        medicine.setStock(100);
        medicine.setPrice(BigDecimal.valueOf(10));
        medicine.setUnit("盒");

        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus("已完成");
        prescription.setCreateTime(LocalDateTime.now().minusDays(5));
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":5}]");

        when(medicineRepository.findAll()).thenReturn(List.of(medicine));
        when(prescriptionRepository.findAll()).thenReturn(List.of(prescription));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"purchaseSuggestions\":[\"建议采购阿莫西林\"],\"factors\":[\"近期消耗上升\"]}");
        when(forecastRepository.save(any())).thenAnswer(inv -> {
            StockForecast f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        var result = stockService.generateStockForecast("monthly", LocalDateTime.now().plusMonths(1).toString().substring(0, 7));

        assertEquals(100L, result.get("id"));
        assertEquals("monthly", result.get("forecastType"));
        assertEquals(1, result.get("medicineCount"));
        verify(forecastRepository).save(any(StockForecast.class));
    }

    @Test
    void generateStockForecast_ShouldUseCurrentMonth_WhenForecastPeriodIsNull() {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("感冒灵");
        medicine.setStock(50);
        medicine.setPrice(BigDecimal.valueOf(5));
        medicine.setUnit("盒");

        when(medicineRepository.findAll()).thenReturn(List.of(medicine));
        when(prescriptionRepository.findAll()).thenReturn(List.of());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");
        when(forecastRepository.save(any())).thenAnswer(inv -> {
            StockForecast f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        var result = stockService.generateStockForecast("monthly", null);

        assertNotNull(result.get("forecastPeriod"));
    }

    @Test
    void generateStockForecast_ShouldUseDefaultSuggestions_WhenAiReturnsEmpty() {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("测试药品");
        medicine.setStock(100);
        medicine.setPrice(BigDecimal.valueOf(10));
        medicine.setUnit("盒");

        when(medicineRepository.findAll()).thenReturn(List.of(medicine));
        when(prescriptionRepository.findAll()).thenReturn(List.of());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");
        when(forecastRepository.save(any())).thenAnswer(inv -> {
            StockForecast f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        stockService.generateStockForecast("monthly", "2026-08");

        verify(forecastRepository).save(argThat(f ->
                f.getPurchaseSuggestions().contains("高风险") &&
                f.getPurchaseSuggestions().contains("积压")
        ));
    }

    @Test
    void generateStockForecast_ShouldCalculateRiskLevels_Correctly() {
        Medicine medicineHighRisk = new Medicine();
        medicineHighRisk.setId(1L);
        medicineHighRisk.setName("抗生素");
        medicineHighRisk.setStock(5);
        medicineHighRisk.setPrice(BigDecimal.valueOf(20));
        medicineHighRisk.setUnit("盒");

        Medicine medicineLowStock = new Medicine();
        medicineLowStock.setId(2L);
        medicineLowStock.setName("退热药");
        medicineLowStock.setStock(30);
        medicineLowStock.setPrice(BigDecimal.valueOf(15));
        medicineLowStock.setUnit("盒");

        when(medicineRepository.findAll()).thenReturn(List.of(medicineHighRisk, medicineLowStock));
        when(prescriptionRepository.findAll()).thenReturn(List.of());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");
        when(forecastRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stockService.generateStockForecast("monthly", "2026-01");

        verify(forecastRepository).save(argThat(f ->
                f.getForecastData().contains("高风险") ||
                f.getForecastData().contains("需补货")
        ));
    }

    // ========== getForecastDetail() ==========

    @Test
    void getForecastDetail_ShouldReturnForecast_WhenExists() {
        StockForecast forecast = new StockForecast();
        forecast.setId(1L);

        when(forecastRepository.findById(1L)).thenReturn(Optional.of(forecast));

        assertEquals(1L, stockService.getForecastDetail(1L).getId());
    }

    @Test
    void getForecastDetail_ShouldReturnNull_WhenNotExists() {
        when(forecastRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(stockService.getForecastDetail(99L));
    }
}
