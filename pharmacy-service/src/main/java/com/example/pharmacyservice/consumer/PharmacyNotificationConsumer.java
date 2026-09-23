package com.example.pharmacyservice.consumer;

import com.example.pharmacyservice.dto.event.ExpiryAlertEvent;
import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer lắng nghe các cảnh báo dược phẩm từ topic 'pharmacy-notifications'.
 *
 * Đáp ứng yêu cầu Bài tập 6:
 * - Lắng nghe topic pharmacy-notifications.
 * - Cập nhật trạng thái thuốc sang "Cần nhập hàng" (NEED_RESTOCK) trong Database.
 * - Gửi email / log thông báo cho người quản lý hiệu thuốc biết tình trạng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PharmacyNotificationConsumer {

    private final InventoryRepository inventoryRepository;

    @Transactional
    @KafkaListener(topics = "pharmacy-notifications", groupId = "pharmacy-expiry-alert-group")
    public void consumeExpiryAlert(ConsumerRecord<String, ExpiryAlertEvent> record) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);

        try {
            ExpiryAlertEvent alert = record.value();
            log.warn("[EXPIRY-ALERT-CONSUMER] [cid:{}] Tiếp nhận cảnh báo từ topic '{}' | Partition: {} | Offset: {}",
                    correlationId, record.topic(), record.partition(), record.offset());
            log.warn("[EXPIRY-ALERT-CONSUMER] [cid:{}] Thuốc: {} ({}) | Hạn dùng: {} (còn {} ngày) | SL: {}",
                    correlationId, alert.getMedicineName(), alert.getMedicineCode(), alert.getExpiryDate(),
                    alert.getDaysRemaining(), alert.getStockQuantity());

            // 1. Cập nhật trạng thái "Cần nhập hàng" (NEED_RESTOCK) cho thuốc trong Database (PostgreSQL)
            Optional<Inventory> optMed = inventoryRepository.findByMedicineCode(alert.getMedicineCode());
            if (optMed.isPresent()) {
                Inventory med = optMed.get();
                med.setStatus("NEED_RESTOCK");
                med.setUpdatedAt(LocalDateTime.now());
                inventoryRepository.save(med);

                log.info("[EXPIRY-ALERT-CONSUMER] [cid:{}] Đã cập nhật thành công trạng thái 'Cần nhập hàng' (NEED_RESTOCK) cho thuốc {} trong DB",
                        correlationId, med.getMedicineCode());
            }

            // 2. Gửi thông báo / email cảnh báo khẩn tới Quản lý hiệu thuốc
            log.info("[EXPIRY-ALERT-EMAIL] [cid:{}] [EMAIL GỬI QUẢN LÝ HIỆU THUỐC]: " +
                            "Kính gửi Quản lý, thuốc '{}' ({}) sắp hết hạn (còn {} ngày). Đã tự động đổi trạng thái sang 'Cần nhập hàng'. Đề nghị lập kế hoạch nhập hàng mới!",
                    correlationId, alert.getMedicineName(), alert.getMedicineCode(), alert.getDaysRemaining());

        } catch (Exception ex) {
            log.error("[EXPIRY-ALERT-CONSUMER] [cid:{}] Lỗi khi xử lý cảnh báo thuốc hết hạn: {}",
                    correlationId, ex.getMessage(), ex);
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
        }
    }
}
