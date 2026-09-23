package com.example.notificationservice.controller;

import com.example.notificationservice.dto.response.ApiResponse;
import com.example.notificationservice.filter.CorrelationIdFilter;
import com.example.notificationservice.model.NotificationRecord;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tra cứu lịch sử thông báo gửi tới khách hàng.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final com.example.notificationservice.service.EmailService emailService;

    /**
     * API lấy danh sách thông báo gửi khách hàng có phân trang (Chuẩn quy định AGENTS.md)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationRecord>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[NOTIFICATION-API] [cid:{}] Lấy danh sách thông báo - Trang: {}, Size: {}", correlationId, page, size);

        Page<NotificationRecord> notificationPage = notificationService.getNotificationHistory(PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(notificationPage, "Lấy danh sách thông báo thành công", correlationId));
    }

    /**
     * API kiểm thử gửi email hóa đơn điện tử thực tế về hòm thư chỉ định (Exercise 04)
     */
    @org.springframework.web.bind.annotation.PostMapping("/test-email")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> sendTestEmail(
            @RequestParam(defaultValue = "jijojo7628@pumpoly.com") String to) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[NOTIFICATION-API] [cid:{}] Yêu cầu gửi email kiểm thử tới: {}", correlationId, to);

        com.example.notificationservice.dto.OrderEvent mockEvent = com.example.notificationservice.dto.OrderEvent.builder()
                .orderId("ORD-TEST-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .medicineId("PARA500")
                .medicineName("Paracetamol 500mg (Hạ sốt & Giảm đau)")
                .quantity(2)
                .unitPrice(java.math.BigDecimal.valueOf(50000))
                .totalPrice(java.math.BigDecimal.valueOf(100000))
                .customerName("Khách hàng Test (" + to + ")")
                .customerEmail(to)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        boolean success = emailService.sendInvoiceEmail(to, mockEvent);

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(
                    java.util.Map.of("recipient", to, "orderId", mockEvent.getOrderId(), "emailStatus", "SENT"),
                    "Đã gửi email xác nhận hóa đơn thành công tới " + to,
                    correlationId
            ));
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gửi email thất bại tới " + to + ". Vui lòng kiểm tra log.", correlationId));
        }
    }
}
