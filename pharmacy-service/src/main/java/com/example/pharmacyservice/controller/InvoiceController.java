package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.request.InvoiceRequest;
import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.dto.response.InvoiceResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller xuất hóa đơn điện tử với bảo vệ 2 tầng: Rate Limiter & Retry.
 *
 * Đáp ứng Bài tập 5:
 * - Giới hạn tần suất xuất hóa đơn 5 req / 10s (Rate Limiter).
 * - Tự động thử lại 3 lần nếu chập chờn mạng (Retry).
 * - Fallback trả về nhanh kết quả thay vì treo hệ thống.
 * - Endpoint GET có phân trang (Chuẩn AGENTS.md).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final List<InvoiceResponse> invoiceHistory = new CopyOnWriteArrayList<>();

    /**
     * API xuất hóa đơn điện tử (Bài tập 5)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> issueInvoice(@Valid @RequestBody InvoiceRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[ISSUE-INVOICE] [cid:{}] Tiếp nhận yêu cầu xuất hóa đơn máy: {}, Khách: {}",
                correlationId, request.getPosTerminalId(), request.getCustomerName());

        InvoiceResponse response = invoiceService.issueInvoice(request);
        if ("ISSUED".equals(response.getStatus())) {
            invoiceHistory.add(0, response);
        }

        HttpStatus status = switch (response.getStatus()) {
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "RETRY_FAILED" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.CREATED;
        };

        return ResponseEntity.status(status).body(ApiResponse.success(response, response.getMessage(), correlationId));
    }

    /**
     * API cấu hình giả lập lỗi mạng để test Retry
     */
    @PostMapping("/simulate-flaky-network")
    public ResponseEntity<ApiResponse<String>> simulateFlakyNetwork(@RequestParam(defaultValue = "2") int failTimes) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        invoiceService.setSimulatedNetworkFailures(failTimes);

        String msg = String.format("Đã thiết lập mạng chập chờn: %d lần gọi tới sẽ báo lỗi kết nối trước khi thành công", failTimes);
        log.info("[SIMULATE-NETWORK] [cid:{}] {}", correlationId, msg);

        return ResponseEntity.ok(ApiResponse.success(msg, msg, correlationId));
    }

    /**
     * Endpoint GET danh sách hóa đơn điện tử có phân trang (Quy chuẩn AGENTS.md)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        int totalElements = invoiceHistory.size();
        int fromIndex = page * size;
        List<InvoiceResponse> content;

        if (fromIndex >= totalElements) {
            content = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            content = invoiceHistory.subList(fromIndex, toIndex);
        }

        Page<InvoiceResponse> invoicePage = new PageImpl<>(content, PageRequest.of(page, size), totalElements);
        return ResponseEntity.ok(ApiResponse.success(invoicePage, "Lấy danh sách hóa đơn điện tử thành công", correlationId));
    }
}
