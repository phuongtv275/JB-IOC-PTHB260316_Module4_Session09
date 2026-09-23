package com.example.inventoryservice.consumer;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.filter.CorrelationIdFilter;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer lắng nghe sự kiện đơn hàng thuốc từ Kafka để tự động trừ tồn kho.
 *
 * Đáp ứng yêu cầu Bài tập 3:
 * - Lắng nghe topic: medicine-stock-events
 * - GroupId: inventory-group (Đảm bảo cân bằng tải các partition và không trùng lặp)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "medicine-stock-events", groupId = "inventory-group")
    public void consumeOrderEvent(ConsumerRecord<String, OrderEvent> record) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);

        try {
            OrderEvent event = record.value();
            log.info("[KAFKA-CONSUMER-INVENTORY] [cid:{}] Nhận được sự kiện từ Topic: {} | Partition: {} | Offset: {} | Key: {}",
                    correlationId, record.topic(), record.partition(), record.offset(), record.key());
            log.info("[KAFKA-CONSUMER-INVENTORY] [cid:{}] Chi tiết đơn hàng: Đơn ID: {}, Thuốc: {}, SL: {}",
                    correlationId, event.getOrderId(), event.getMedicineId(), event.getQuantity());

            // Tiến hành cập nhật trừ tồn kho trong Database (Exercise 03)
            inventoryService.processStockDeduction(event);

        } catch (Exception ex) {
            log.error("[KAFKA-CONSUMER-INVENTORY] [cid:{}] Lỗi khi xử lý sự kiện tồn kho: {}",
                    correlationId, ex.getMessage(), ex);
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
        }
    }
}
