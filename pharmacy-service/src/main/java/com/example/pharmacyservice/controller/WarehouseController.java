package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.request.StockCheckRequest;
import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.dto.response.StockCheckResponse;
import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.repository.InventoryRepository;
import com.example.pharmacyservice.service.WarehouseService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller kiểm tra hàng tồn kho kho tổng & quản lý Circuit Breaker.
 *
 * Đáp ứng Bài tập 3 & 4:
 * - Gọi kiểm tra kho tổng với Circuit Breaker "warehouseCB".
 * - Khi kho sập, kích hoạt Fallback trả về dữ liệu kho thuốc tại chỗ.
 * - Endpoint GET kiểm tra danh sách tồn kho có phân trang (Chuẩn AGENTS.md).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final InventoryRepository inventoryRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * API kiểm tra hàng tồn kho (Bài tập 3 & Bài tập 4)
     */
    @PostMapping("/check-stock")
    public ResponseEntity<ApiResponse<StockCheckResponse>> checkStock(@Valid @RequestBody StockCheckRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[CHECK-STOCK] [cid:{}] Tiếp nhận yêu cầu kiểm tra hàng tồn mã: {}, số lượng: {}",
                correlationId, request.getMedicineCode(), request.getQuantity());

        StockCheckResponse response = warehouseService.checkStock(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage(), correlationId));
    }

    /**
     * Endpoint mô phỏng sự cố kho tổng (Sập / Bình thường) phục vụ kiểm thử
     */
    @PostMapping("/simulate-outage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulateOutage(@RequestParam boolean down) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        warehouseService.setWarehouseDown(down);

        Map<String, Object> result = new HashMap<>();
        result.put("warehouseDown", down);
        result.put("circuitBreakerState", warehouseService.getCircuitBreakerState());
        result.put("message", down ? "Đã giả lập KHO TỔNG BỊ SẬP (503)" : "Đã khôi phục KHO TỔNG HOẠT ĐỘNG BÌNH THƯỜNG");

        log.info("[SIMULATE-OUTAGE] [cid:{}] Trạng thái kho tổng: down={}, CB State: {}",
                correlationId, down, warehouseService.getCircuitBreakerState());

        return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật trạng thái giả lập thành công", correlationId));
    }

    /**
     * Endpoint theo dõi trạng thái và chỉ số Circuit Breaker warehouseCB
     */
    @GetMapping("/circuit-breaker-state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCircuitBreakerMetrics() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);

        Map<String, Object> metrics = new HashMap<>();
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("warehouseCB");
            metrics.put("name", cb.getName());
            metrics.put("state", cb.getState().name());
            metrics.put("failureRate", cb.getMetrics().getFailureRate() + "%");
            metrics.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            metrics.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            metrics.put("totalBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            metrics.put("warehouseSimulatedDown", warehouseService.isWarehouseDown());
        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(metrics, "Lấy thông số Circuit Breaker thành công", correlationId));
    }

    /**
     * Endpoint GET danh sách tồn kho thuốc cục bộ có phân trang (Quy chuẩn AGENTS.md)
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Page<Inventory>>> getLocalInventories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[LOCAL-INVENTORY] [cid:{}] Lấy danh sách tồn kho cục bộ. Trang: {}, Size: {}, Sắp xếp: {}",
                correlationId, page, size, sortBy);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Inventory> inventoryPage = inventoryRepository.findAll(pageRequest);

        return ResponseEntity.ok(ApiResponse.success(inventoryPage, "Lấy danh sách tồn kho cục bộ thành công", correlationId));
    }
}
