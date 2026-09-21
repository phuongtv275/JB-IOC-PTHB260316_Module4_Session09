package com.example.pharmacyservice.service.impl;

import com.example.pharmacyservice.dto.request.StockCheckRequest;
import com.example.pharmacyservice.dto.response.StockCheckResponse;
import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.repository.InventoryRepository;
import com.example.pharmacyservice.service.WarehouseService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service xử lý kiểm tra tồn kho từ kho tổng với cơ chế Circuit Breaker & Fallback.
 *
 * Đáp ứng yêu cầu Bài tập 3 & 4:
 * - @CircuitBreaker(name = "warehouseCB", fallbackMethod = "checkWarehouseFallback"):
 *   + Theo dõi tỷ lệ lỗi kết nối kho tổng. Khi vượt 50%, ngắt mạch sang trạng thái OPEN (Fail-fast).
 *   + Thời gian chờ ở trạng thái OPEN: 20 giây (cấu hình từ Git Config Server).
 * - Hàm Fallback checkWarehouseFallback:
 *   + Khi kho tổng bị lỗi hoặc mạch ngắt, hệ thống chuyển sang sử dụng tồn kho cục bộ (Local Inventory)
 *   + Trả về thông báo chuẩn: "Không thể kết nối kho tổng. Hệ thống sẽ sử dụng dữ liệu tồn kho cục bộ để tiếp tục giao dịch"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final InventoryRepository inventoryRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Cờ giả lập sự cố kho tổng phục vụ kiểm thử bài tập 3 & 4
    private volatile boolean warehouseDown = false;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "warehouseCB",
            fallbackMethod = "checkWarehouseFallback"
    )
    public StockCheckResponse checkStock(StockCheckRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        String currentState = getCircuitBreakerState();

        log.info("[WAREHOUSE-CALL] [cid:{}] Gọi sang kho tổng kiểm tra mã: {}, SL: {}. Trạng thái CB: {}",
                correlationId, request.getMedicineCode(), request.getQuantity(), currentState);

        /*
         * LOGIC GIẢI THÍCH:
         * Giả lập cuộc gọi mạng HTTP sang warehouse-service (Kho tổng).
         * Nếu cờ warehouseDown = true, ném ngoại lệ để Resilience4j ghi nhận lỗi tính tỷ lệ ngắt mạch.
         */
        if (warehouseDown) {
            log.warn("[WAREHOUSE-ERROR] [cid:{}] Kho tổng phản hồi lỗi: 503 Service Unavailable / Connection Timeout", correlationId);
            throw new RuntimeException("Không thể kết nối đến máy chủ kho tổng (warehouse-service: 503 Service Unavailable)");
        }

        // Trường hợp kho tổng hoạt động bình thường:
        log.info("[WAREHOUSE-SUCCESS] [cid:{}] Kho tổng phản hồi thành công mã: {}", correlationId, request.getMedicineCode());
        return StockCheckResponse.builder()
                .medicineCode(request.getMedicineCode())
                .medicineName("Thuốc " + request.getMedicineCode() + " (Từ Kho Tổng)")
                .requestedQuantity(request.getQuantity())
                .availableQuantity(500) // Kho tổng có sẵn số lượng lớn
                .available(500 >= request.getQuantity())
                .unitPrice(BigDecimal.valueOf(45000))
                .stockSource("CENTRAL_WAREHOUSE")
                .circuitBreakerState(currentState)
                .message("Kiểm tra kho tổng thành công, hàng sẵn sàng xuất bán")
                .build();
    }

    /**
     * Phương án dự phòng Fallback khi mất kết nối kho tổng hoặc khi mạch OPEN (Bài tập 4).
     *
     * @param request Thông tin mã thuốc yêu cầu
     * @param ex Ngoại lệ gây ra (Timeout, lỗi kết nối hoặc CallNotPermittedException khi mạch OPEN)
     * @return Dữ liệu tồn kho lấy từ cơ sở dữ liệu cục bộ của chi nhánh
     */
    public StockCheckResponse checkWarehouseFallback(StockCheckRequest request, Throwable ex) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        String currentState = getCircuitBreakerState();

        log.warn("[WAREHOUSE-FALLBACK] [cid:{}] KÍCH HOẠT FALLBACK do: '{}'. Trạng thái CB: {}. Chuyển sang tra cứu kho cục bộ.",
                correlationId, ex.getMessage(), currentState);

        // Truy vấn dữ liệu tồn kho tại chỗ (local database)
        Optional<Inventory> localOpt = inventoryRepository.findByMedicineCode(request.getMedicineCode());

        int localStock = localOpt.map(Inventory::getLocalStockQuantity).orElse(0);
        String medicineName = localOpt.map(Inventory::getMedicineName).orElse("Thuốc " + request.getMedicineCode());
        BigDecimal price = localOpt.map(Inventory::getUnitPrice).orElse(BigDecimal.valueOf(50000));
        boolean isAvailable = localStock >= request.getQuantity();

        /*
         * Thông báo chuẩn theo yêu cầu chính xác của Exercise 04:
         * "Không thể kết nối kho tổng. Hệ thống sẽ sử dụng dữ liệu tồn kho cục bộ để tiếp tục giao dịch"
         */
        return StockCheckResponse.builder()
                .medicineCode(request.getMedicineCode())
                .medicineName(medicineName)
                .requestedQuantity(request.getQuantity())
                .availableQuantity(localStock)
                .available(isAvailable)
                .unitPrice(price)
                .stockSource("LOCAL_INVENTORY")
                .circuitBreakerState(currentState)
                .message("Không thể kết nối kho tổng. Hệ thống sẽ sử dụng dữ liệu tồn kho cục bộ để tiếp tục giao dịch")
                .build();
    }

    @Override
    public void setWarehouseDown(boolean down) {
        this.warehouseDown = down;
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[WAREHOUSE-SIMULATION] [cid:{}] Cập nhật trạng thái giả lập kho tổng: warehouseDown = {}", correlationId, down);
    }

    @Override
    public boolean isWarehouseDown() {
        return this.warehouseDown;
    }

    @Override
    public String getCircuitBreakerState() {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("warehouseCB");
            return cb.getState().name();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
