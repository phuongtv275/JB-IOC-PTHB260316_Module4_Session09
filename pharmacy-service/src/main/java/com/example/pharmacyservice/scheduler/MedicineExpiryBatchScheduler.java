package com.example.pharmacyservice.scheduler;

import com.example.pharmacyservice.dto.event.ExpiryAlertEvent;
import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.producer.ExpiryAlertProducer;
import com.example.pharmacyservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled Batch Service định kỳ kiểm tra tồn kho và quét các loại thuốc sắp hết hạn.
 *
 * Đáp ứng yêu cầu Bài tập 6:
 * - Hàng ngày/định kỳ quét kho tìm thuốc sắp hết hạn (còn hạn dưới 30 ngày).
 * - Nếu tìm thấy, đóng gói sự kiện cảnh báo và gửi vào topic pharmacy-notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineExpiryBatchScheduler {

    private final InventoryRepository inventoryRepository;
    private final ExpiryAlertProducer expiryAlertProducer;

    /**
     * Tự động chạy mỗi 60 giây (bắt đầu sau 15 giây khi khởi động app).
     * Phù hợp kiểm thử thực tế và demo đồ án.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public int scanAndAlertExpiringMedicines() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);

        try {
            log.info("[BATCH-SCANNER] [cid:{}] Bắt đầu chu kỳ quét thuốc sắp hết hạn trong kho...", correlationId);

            LocalDate today = LocalDate.now();
            LocalDate warningThreshold = today.plusDays(30);

            List<Inventory> allMedicines = inventoryRepository.findAll();
            int alertCount = 0;

            for (Inventory med : allMedicines) {
                if (med.getExpiryDate() != null && !med.getExpiryDate().isAfter(warningThreshold)) {
                    // Thuốc đã hoặc sắp hết hạn trong vòng 30 ngày tới
                    long daysRemaining = ChronoUnit.DAYS.between(today, med.getExpiryDate());

                    // Chỉ gửi cảnh báo nếu thuốc chưa được đánh dấu là NEED_RESTOCK
                    if (!"NEED_RESTOCK".equalsIgnoreCase(med.getStatus())) {
                        alertCount++;
                        String alertMsg = String.format("CẢNH BÁO: Thuốc %s (%s) sắp hết hạn vào ngày %s (còn %d ngày), tồn kho: %d đơn vị.",
                                med.getMedicineName(), med.getMedicineCode(), med.getExpiryDate(), daysRemaining, med.getLocalStockQuantity());

                        log.warn("[BATCH-SCANNER] [cid:{}] Phát hiện thuốc sắp hết hạn: {}", correlationId, alertMsg);

                        ExpiryAlertEvent alertEvent = ExpiryAlertEvent.builder()
                                .alertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .medicineCode(med.getMedicineCode())
                                .medicineName(med.getMedicineName())
                                .expiryDate(med.getExpiryDate())
                                .daysRemaining(daysRemaining)
                                .stockQuantity(med.getLocalStockQuantity())
                                .alertMessage(alertMsg)
                                .timestamp(LocalDateTime.now())
                                .build();

                        expiryAlertProducer.sendExpiryAlert(alertEvent);
                    }
                }
            }

            log.info("[BATCH-SCANNER] [cid:{}] Kết thúc chu kỳ quét. Phát hiện và đã gửi cảnh báo cho {} loại thuốc.",
                    correlationId, alertCount);
            return alertCount;

        } catch (Exception ex) {
            log.error("[BATCH-SCANNER] [cid:{}] Lỗi trong quá trình quét kho: {}", correlationId, ex.getMessage(), ex);
            return 0;
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
        }
    }
}
