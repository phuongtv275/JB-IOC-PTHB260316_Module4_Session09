package com.example.inventoryservice.service.impl;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.filter.CorrelationIdFilter;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service xử lý cập nhật trừ kho dược tự động khi nhận sự kiện đơn hàng.
 *
 * Đáp ứng yêu cầu Bài tập 3:
 * - Khi nhận OrderEvent, Consumer gọi xuống Database để thực hiện lệnh:
 *   UPDATE local_inventories SET local_stock_quantity = local_stock_quantity - quantity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void processStockDeduction(OrderEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[INVENTORY-SERVICE] [cid:{}] Bắt đầu trừ kho cho đơn hàng {} | Thuốc: {} | Số lượng: {}",
                correlationId, event.getOrderId(), event.getMedicineId(), event.getQuantity());

        /*
         * LOGIC GIẢI THÍCH:
         * 1. Sử dụng câu lệnh UPDATE trực tiếp có điều kiện `localStockQuantity >= :quantity`
         *    nhằm đảm bảo tính Atomic (nguyên tử), tránh Race Condition khi có nhiều luồng
         *    hoặc consumer cùng cập nhật tồn kho của cùng 1 loại thuốc.
         * 2. Nếu số dòng được update = 0, kiểm tra xem thuốc có tồn tại hay không đủ số lượng tồn.
         */
        int updatedRows = inventoryRepository.deductStock(
                event.getMedicineId(),
                event.getQuantity(),
                LocalDateTime.now()
        );

        if (updatedRows > 0) {
            Inventory updated = inventoryRepository.findByMedicineCode(event.getMedicineId()).orElse(null);
            Integer remainingStock = updated != null ? updated.getLocalStockQuantity() : null;
            log.info("[INVENTORY-SERVICE] [cid:{}] Cập nhật kho thành công! Thuốc: {} | Đã trừ: {} | Tồn kho hiện tại: {}",
                    correlationId, event.getMedicineId(), event.getQuantity(), remainingStock);
        } else {
            Inventory current = inventoryRepository.findByMedicineCode(event.getMedicineId()).orElse(null);
            if (current == null) {
                log.error("[INVENTORY-SERVICE] [cid:{}] Không tìm thấy thuốc {} trong kho để trừ tồn kho.",
                        correlationId, event.getMedicineId());
            } else {
                log.error("[INVENTORY-SERVICE] [cid:{}] Thuốc {} không đủ tồn kho để trừ (Hiện có: {}, Cần trừ: {}).",
                        correlationId, event.getMedicineId(), current.getLocalStockQuantity(), event.getQuantity());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> getInventories(Pageable pageable) {
        return inventoryRepository.findAll(pageable);
    }
}
