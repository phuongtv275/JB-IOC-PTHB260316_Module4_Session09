package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.request.OrderCreateRequest;
import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.dto.response.OrderResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý tạo đơn hàng bán thuốc và kích hoạt luồng sự kiện Kafka.
 *
 * Đáp ứng yêu cầu Bài tập 2:
 * - API POST /api/v1/orders: Thanh toán và gửi OrderEvent lên Kafka.
 * - API GET /api/v1/orders: Xem lịch sử đơn hàng có phân trang.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/orders", "/api/v1/order"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * API bán thuốc / thanh toán đơn hàng (Exercise 02)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[ORDER-API] [cid:{}] Tiếp nhận yêu cầu thanh toán đơn hàng thuốc: {} - SL: {}",
                correlationId, request.getMedicineId(), request.getQuantity());

        OrderResponse response = orderService.createOrderAndPublishEvent(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thanh toán đơn hàng và gửi sự kiện lên Kafka thành công", correlationId));
    }

    /**
     * API xem danh sách đơn hàng có phân trang (Chuẩn AGENTS.md)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[ORDER-API] [cid:{}] Lấy danh sách đơn hàng - Trang: {}, Size: {}", correlationId, page, size);

        Page<OrderResponse> orderPage = orderService.getOrders(PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(orderPage, "Lấy danh sách đơn hàng thành công", correlationId));
    }
}
