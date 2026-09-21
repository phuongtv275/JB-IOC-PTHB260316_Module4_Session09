package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.request.BillItemRequest;
import com.example.pharmacyservice.dto.request.BillRequest;
import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.dto.response.BillItemResponse;
import com.example.pharmacyservice.dto.response.BillResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller xử lý tính tiền và quản lý hóa đơn thuốc.
 *
 * Đáp ứng yêu cầu Bài tập 2:
 * - Annotation @RefreshScope cho phép tự động cập nhật giá trị vatRate khi gọi POST /actuator/refresh
 *   mà không cần khởi động lại Server.
 * - Công thức tính: tổng tiền thuốc + (% thuế VAT * tổng tiền thuốc).
 * - Tuân thủ AGENTS.md:
 *   + Validate request DTO (@Valid).
 *   + Gắn Correlation ID vào mọi log.
 *   + Phân trang cho endpoint GET trả về danh sách hóa đơn.
 */
@Slf4j
@RefreshScope
@RestController
@RequestMapping({"/api/v1/bill", "/api/v1/bills"})
public class BillController {

    /**
     * Tỷ lệ thuế VAT lấy từ Spring Cloud Config Server (pharmacy.vat-rate).
     * Khi thay đổi trên Git và kích hoạt /actuator/refresh, @RefreshScope sẽ nạp lại bean này
     * với giá trị thuế mới nhất.
     */
    @Value("${pharmacy.vat-rate:0.10}")
    private double vatRate;

    // Bộ nhớ tạm lưu trữ danh sách hóa đơn phục vụ endpoint GET phân trang
    private final List<BillResponse> billHistory = new CopyOnWriteArrayList<>();

    /**
     * API tính tiền hóa đơn thuốc (Exercise 02)
     *
     * @param request Thông tin khách hàng và danh sách thuốc
     * @return Hóa đơn chi tiết gồm tiền thuốc, tiền VAT và tổng tiền thanh toán
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BillResponse>> calculateBill(@Valid @RequestBody BillRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[CALCULATE-BILL] [cid:{}] Bắt đầu tính hóa đơn cho khách: {}, Thuế VAT hiện tại: {}%",
                correlationId, request.getCustomerName(), vatRate * 100);

        // 1. Tính toán thành tiền từng loại thuốc và tổng tiền thuốc trước thuế
        BigDecimal subTotalAmount = BigDecimal.ZERO;
        List<BillItemResponse> itemResponses = new ArrayList<>();

        for (BillItemRequest item : request.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subTotalAmount = subTotalAmount.add(lineTotal);

            itemResponses.add(BillItemResponse.builder()
                    .medicineName(item.getMedicineName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subTotal(lineTotal)
                    .build());
        }

        /*
         * LOGIC GIẢI THÍCH:
         * Công thức theo yêu cầu: Tổng tiền thanh toán = Tổng tiền thuốc + (Tổng tiền thuốc * % thuế VAT)
         * Làm tròn tiền tệ 2 chữ số thập phân (HALF_UP).
         */
        BigDecimal vatRateDecimal = BigDecimal.valueOf(vatRate);
        BigDecimal vatAmount = subTotalAmount.multiply(vatRateDecimal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subTotalAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        BillResponse billResponse = BillResponse.builder()
                .billId(UUID.randomUUID().toString())
                .customerName(request.getCustomerName())
                .items(itemResponses)
                .subTotalAmount(subTotalAmount)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .note("Hóa đơn áp dụng thuế VAT " + (vatRate * 100) + "% (Dynamic Refresh)")
                .build();

        billHistory.add(0, billResponse);

        log.info("[CALCULATE-BILL] [cid:{}] Hoàn thành tính tiền. SubTotal: {}, VAT ({}%): {}, Total: {}",
                correlationId, subTotalAmount, vatRate * 100, vatAmount, totalAmount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(billResponse, "Tính tiền hóa đơn thành công", correlationId));
    }

    /**
     * Endpoint GET lấy danh sách lịch sử hóa đơn có phân trang (Chuẩn quy định AGENTS.md)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[GET-BILLS] [cid:{}] Lấy danh sách hóa đơn trang: {}, kích thước: {}", correlationId, page, size);

        int totalElements = billHistory.size();
        int fromIndex = page * size;
        List<BillResponse> pageContent;

        if (fromIndex >= totalElements) {
            pageContent = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            pageContent = billHistory.subList(fromIndex, toIndex);
        }

        Page<BillResponse> billPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalElements);
        return ResponseEntity.ok(ApiResponse.success(billPage, "Lấy danh sách hóa đơn thành công", correlationId));
    }
}
