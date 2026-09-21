package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.request.InsuranceVerificationRequest;
import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.dto.response.InsuranceVerificationResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.service.InsuranceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller xử lý xác thực bảo hiểm y tế (BHYT) với Full Resilience Stack.
 *
 * Đáp ứng Bài tập 6:
 * - TimeLimiter ngắt kết nối sau 3s (hoặc 1s cấu hình qua Config Server).
 * - Retry tự động thử lại 3 lần nếu có sự cố.
 * - Circuit Breaker ngắt mạch nếu tỷ lệ lỗi vượt quá 60%.
 * - Fallback trả về giá chưa chiết khấu kèm ghi chú "Xác thực bảo hiểm sau".
 * - Phân trang cho endpoint GET theo chuẩn AGENTS.md.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final List<InsuranceVerificationResponse> insuranceHistory = new CopyOnWriteArrayList<>();

    /**
     * API xác thực bảo hiểm y tế (Bài tập 6)
     */
    @PostMapping("/verify")
    public CompletableFuture<ResponseEntity<ApiResponse<InsuranceVerificationResponse>>> verifyInsurance(
            @Valid @RequestBody InsuranceVerificationRequest request,
            @RequestParam(required = false) Integer delayMs) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[VERIFY-INSURANCE] [cid:{}] Tiếp nhận xác thực BHYT: {} cho đơn thuốc: {}",
                correlationId, request.getInsuranceCardNumber(), request.getPrescriptionCode());

        return insuranceService.verifyInsurance(request, delayMs)
                .thenApply(response -> {
                    insuranceHistory.add(0, response);
                    return ResponseEntity.ok(ApiResponse.success(response, response.getNote(), correlationId));
                });
    }

    /**
     * Endpoint kiểm tra trạng thái Circuit Breaker bảo hiểm (insuranceCB)
     */
    @GetMapping("/circuit-breaker-state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInsuranceCircuitBreakerState() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);

        Map<String, Object> metrics = new HashMap<>();
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("insuranceCB");
            metrics.put("name", cb.getName());
            metrics.put("state", cb.getState().name());
            metrics.put("failureRate", cb.getMetrics().getFailureRate() + "%");
            metrics.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            metrics.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            metrics.put("totalBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(metrics, "Lấy thông số Circuit Breaker bảo hiểm thành công", correlationId));
    }

    /**
     * Endpoint GET danh sách lịch sử xác thực bảo hiểm có phân trang (Chuẩn AGENTS.md)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<InsuranceVerificationResponse>>> getInsuranceHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        int totalElements = insuranceHistory.size();
        int fromIndex = page * size;
        List<InsuranceVerificationResponse> content;

        if (fromIndex >= totalElements) {
            content = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            content = insuranceHistory.subList(fromIndex, toIndex);
        }

        Page<InsuranceVerificationResponse> historyPage = new PageImpl<>(content, PageRequest.of(page, size), totalElements);
        return ResponseEntity.ok(ApiResponse.success(historyPage, "Lấy lịch sử xác thực bảo hiểm thành công", correlationId));
    }
}
