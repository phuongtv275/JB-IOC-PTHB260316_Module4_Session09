package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.scheduler.MedicineExpiryBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller cho phép kích hoạt quét kho dược thủ công (phục vụ kiểm thử tức thì cho Exercise 06).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchScannerController {

    private final MedicineExpiryBatchScheduler batchScheduler;

    @PostMapping("/scan-expiry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerExpiryScan() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[BATCH-API] [cid:{}] Nhận yêu cầu kích hoạt quét thuốc sắp hết hạn thủ công", correlationId);

        int alertCount = batchScheduler.scanAndAlertExpiringMedicines();

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("alertCount", alertCount, "status", "SCAN_COMPLETED"),
                "Quét kho thành công, đã phát hiện và gửi cảnh báo cho " + alertCount + " loại thuốc",
                correlationId
        ));
    }
}
