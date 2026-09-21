package com.example.pharmacyservice.service.impl;

import com.example.pharmacyservice.dto.request.BillItemRequest;
import com.example.pharmacyservice.dto.request.InvoiceRequest;
import com.example.pharmacyservice.dto.response.InvoiceResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.service.InvoiceService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service xử lý xuất hóa đơn điện tử cho máy bán thuốc (POS Terminal).
 *
 * Đáp ứng yêu cầu Bài tập 5:
 * - Rate Limiter (invoiceRateLimiter): Giới hạn mỗi máy chỉ được xuất tối đa 5 hóa đơn / 10s.
 * - Retry (invoiceRetry): Tự động thử lại 3 lần nếu gặp lỗi mạng (mỗi lần cách 2s).
 * - Fallback methods: Trả về nhanh kết quả khi bị chặn tần suất hoặc khi kết nối mạng thất bại sau 3 lần thử.
 * - Tất cả thông số được cấu hình tập trung từ Config Server.
 */
@Slf4j
@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Value("${pharmacy.vat-rate:0.08}")
    private double vatRate;

    // Số lần lỗi mạng giả lập còn lại phục vụ kiểm thử Retry
    private final AtomicInteger simulatedNetworkFailures = new AtomicInteger(0);

    // Đếm số lần gọi thực tế cho request hiện tại
    private final ThreadLocal<Integer> callCounter = ThreadLocal.withInitial(() -> 0);

    @Override
    @RateLimiter(name = "invoiceRateLimiter", fallbackMethod = "invoiceFallback")
    @Retry(name = "invoiceRetry", fallbackMethod = "invoiceFallback")
    public InvoiceResponse issueInvoice(InvoiceRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        int currentAttempt = callCounter.get() + 1;
        callCounter.set(currentAttempt);

        log.info("[INVOICE-CALL] [cid:{}] Thực hiện xuất hóa đơn cho máy: {}. Lần thử: {}",
                correlationId, request.getPosTerminalId(), currentAttempt);

        /*
         * LOGIC GIẢI THÍCH:
         * Giả lập lỗi mạng chập chờn khi gửi dữ liệu sang Tổng cục Thuế / Cổng hóa đơn điện tử.
         * Nếu simulatedNetworkFailures > 0, ném ngoại lệ ConnectException để kích hoạt cơ chế Retry.
         */
        int remainingFailures = simulatedNetworkFailures.get();
        if (remainingFailures > 0) {
            simulatedNetworkFailures.decrementAndGet();
            log.warn("[INVOICE-NETWORK-ERROR] [cid:{}] Lỗi kết nối cổng Hóa đơn điện tử! Còn {} lỗi giả lập. Chuẩn bị thử lại...",
                    correlationId, simulatedNetworkFailures.get());
            throw new RuntimeException(new ConnectException("Mất kết nối tạm thời tới máy chủ Hóa đơn điện tử (Connect timeout 2000ms)"));
        }

        // 1. Tính toán giá trị hóa đơn
        BigDecimal subTotal = BigDecimal.ZERO;
        for (BillItemRequest item : request.getItems()) {
            subTotal = subTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal vatAmount = subTotal.multiply(BigDecimal.valueOf(vatRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subTotal.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        String invoiceNumber = "INV-" + System.currentTimeMillis() % 1000000;
        int totalAttemptsMade = currentAttempt;
        callCounter.remove(); // Dọn dẹp ThreadLocal

        log.info("[INVOICE-SUCCESS] [cid:{}] Xuất hóa đơn thành công số: {} sau {} lần gọi.",
                correlationId, invoiceNumber, totalAttemptsMade);

        return InvoiceResponse.builder()
                .invoiceNumber(invoiceNumber)
                .posTerminalId(request.getPosTerminalId())
                .customerName(request.getCustomerName())
                .subTotalAmount(subTotal)
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .status("ISSUED")
                .attemptCount(totalAttemptsMade)
                .issuedAt(LocalDateTime.now())
                .message("Hóa đơn điện tử đã được xuất và ký số thành công")
                .build();
    }

    /**
     * Fallback Method 1: Xử lý khi bị chặn bởi Rate Limiter (Quá 5 hóa đơn / 10s)
     */
    public InvoiceResponse invoiceFallback(InvoiceRequest request, RequestNotPermitted ex) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        callCounter.remove();

        log.warn("[INVOICE-RATE-LIMITED] [cid:{}] Máy {} đã vượt quá giới hạn 5 hóa đơn / 10 giây: {}",
                correlationId, request.getPosTerminalId(), ex.getMessage());

        return InvoiceResponse.builder()
                .invoiceNumber("N/A")
                .posTerminalId(request.getPosTerminalId())
                .customerName(request.getCustomerName())
                .subTotalAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status("RATE_LIMITED")
                .attemptCount(0)
                .issuedAt(LocalDateTime.now())
                .message("Thao tác quá nhanh! Máy bán thuốc chỉ được phép xuất tối đa 5 hóa đơn trong 10 giây. Vui lòng chờ ít giây.")
                .build();
    }

    /**
     * Fallback Method 2: Xử lý khi hết 3 lần Retry vẫn không thể kết nối mạng
     */
    public InvoiceResponse invoiceFallback(InvoiceRequest request, Throwable ex) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        int finalAttempts = callCounter.get();
        callCounter.remove();

        log.error("[INVOICE-RETRY-EXHAUSTED] [cid:{}] Đã thử lại {} lần nhưng không thể kết nối: {}",
                correlationId, finalAttempts, ex.getMessage());

        return InvoiceResponse.builder()
                .invoiceNumber("PENDING-" + UUID.randomUUID().toString().substring(0, 8))
                .posTerminalId(request.getPosTerminalId())
                .customerName(request.getCustomerName())
                .subTotalAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status("RETRY_FAILED")
                .attemptCount(finalAttempts)
                .issuedAt(LocalDateTime.now())
                .message("Không thể kết nối cổng xuất hóa đơn điện tử sau 3 lần thử lại. Hóa đơn đã được lưu tạm để tự động đồng bộ sau.")
                .build();
    }

    @Override
    public void setSimulatedNetworkFailures(int count) {
        this.simulatedNetworkFailures.set(count);
        log.info("[INVOICE-SIMULATION] Thiết lập số lần lỗi mạng giả lập: {}", count);
    }

    @Override
    public int getRemainingSimulatedFailures() {
        return this.simulatedNetworkFailures.get();
    }
}
