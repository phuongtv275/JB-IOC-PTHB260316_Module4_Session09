package com.example.pharmacyservice.service.impl;

import com.example.pharmacyservice.dto.request.InsuranceVerificationRequest;
import com.example.pharmacyservice.dto.response.InsuranceVerificationResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.service.InsuranceService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service xác thực thẻ bảo hiểm y tế (BHYT) với Full Resilience Stack.
 *
 * Đáp ứng yêu cầu Bài tập 6:
 * - TimeLimiter (insuranceTimeout): Tự ngắt nếu server bảo hiểm không phản hồi trong 3s (hoặc 1s cấu hình qua Git).
 * - Retry (insuranceRetry): Tự động thử lại 3 lần nếu gặp lỗi kết nối (cách nhau 2s).
 * - Circuit Breaker (insuranceCB): Ngắt mạch nếu tỷ lệ timeout/lỗi liên tục vượt quá 60%.
 * - Fallback: Trả về giá thuốc gốc chưa chiết khấu kèm ghi chú "Xác thực bảo hiểm sau".
 */
@Slf4j
@RefreshScope
@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @PostConstruct
    public void onRefresh() {
        // Xóa instance cũ trong Registry để nạp cấu hình timeout mới từ Config Server
        try {
            timeLimiterRegistry.remove("insuranceTimeout");
            log.info("[TIME-LIMITER-REFRESH] Đã xóa cache insuranceTimeout để áp dụng timeout mới từ Config Server");
        } catch (Exception ignored) {
        }
    }

    @Override
    @CircuitBreaker(name = "insuranceCB", fallbackMethod = "verifyInsuranceFallback")
    @TimeLimiter(name = "insuranceTimeout")
    @Retry(name = "insuranceRetry")
    public CompletableFuture<InsuranceVerificationResponse> verifyInsurance(
            InsuranceVerificationRequest request,
            Integer simulatedDelayMs) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int delay = (simulatedDelayMs != null && simulatedDelayMs > 0) ? simulatedDelayMs : 200;

            log.info("[INSURANCE-CALL] [cid:{}] Gửi yêu cầu xác thực thẻ BHYT: {} (Độ trễ giả lập: {}ms)",
                    correlationId, request.getInsuranceCardNumber(), delay);

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[INSURANCE-INTERRUPT] [cid:{}] Tiến trình xác thực bị ngắt do Timeout!", correlationId);
                throw new RuntimeException("Thời gian chờ phản hồi bảo hiểm bị ngắt do TimeLimiter", e);
            }

            // Giả lập tính mức hưởng BHYT thành công (80% chiết khấu)
            BigDecimal totalAmount = request.getMedicineTotalAmount();
            BigDecimal discount = totalAmount.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal patientPay = totalAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[INSURANCE-SUCCESS] [cid:{}] Xác thực BHYT thành công trong {}ms. Tổng: {}, BHYT trả (80%): {}, Bệnh nhân trả: {}",
                    correlationId, executionTime, totalAmount, discount, patientPay);

            return InsuranceVerificationResponse.builder()
                    .patientName(request.getPatientName())
                    .insuranceCardNumber(request.getInsuranceCardNumber())
                    .prescriptionCode(request.getPrescriptionCode())
                    .originalAmount(totalAmount)
                    .discountAmount(discount)
                    .finalAmount(patientPay)
                    .coveragePercent(80.0)
                    .verified(true)
                    .note("Đã xác thực bảo hiểm y tế thành công (Hưởng 80%)")
                    .circuitBreakerState(getCircuitBreakerState())
                    .executionTimeMs(executionTime)
                    .processedAt(LocalDateTime.now())
                    .build();
        });
    }

    /**
     * Fallback Method theo yêu cầu Exercise 06:
     * "Nếu không xác thực được bảo hiểm, hệ thống trả về giá thuốc chưa chiết khấu kèm ghi chú 'Xác thực bảo hiểm sau'"
     */
    public CompletableFuture<InsuranceVerificationResponse> verifyInsuranceFallback(
            InsuranceVerificationRequest request,
            Integer simulatedDelayMs,
            Throwable ex) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.warn("[INSURANCE-FALLBACK] [cid:{}] KÍCH HOẠT FALLBACK BẢO HIỂM do: '{}'. Áp dụng giá gốc chưa chiết khấu.",
                correlationId, ex.getMessage());

        InsuranceVerificationResponse fallbackResponse = InsuranceVerificationResponse.builder()
                .patientName(request.getPatientName())
                .insuranceCardNumber(request.getInsuranceCardNumber())
                .prescriptionCode(request.getPrescriptionCode())
                .originalAmount(request.getMedicineTotalAmount())
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(request.getMedicineTotalAmount())
                .coveragePercent(0.0)
                .verified(false)
                .note("Xác thực bảo hiểm sau")
                .circuitBreakerState(getCircuitBreakerState())
                .executionTimeMs(0)
                .processedAt(LocalDateTime.now())
                .build();

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    @Override
    public String getCircuitBreakerState() {
        try {
            io.github.resilience4j.circuitbreaker.CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("insuranceCB");
            return cb.getState().name();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
