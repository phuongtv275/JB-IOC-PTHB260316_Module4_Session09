package com.example.notificationservice.consumer;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.filter.CorrelationIdFilter;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer lắng nghe sự kiện đơn hàng thuốc từ Kafka để gửi thông báo khách hàng (Fan-out pattern).
 *
 * Đáp ứng yêu cầu Bài tập 4:
 * - Cùng lắng nghe topic: medicine-stock-events.
 * - GroupId khác với inventory-service: notification-group (để cả 2 service cùng nhận được bản sao của tin nhắn).
 * - Tự động gửi email hóa đơn điện tử thực tế về hòm thư khách hàng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "medicine-stock-events", groupId = "notification-group")
    public void consumeOrderEvent(ConsumerRecord<String, Object> record) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);

        try {
            Object rawValue = record.value();
            OrderEvent event;

            /*
             * LOGIC GIẢI THÍCH:
             * Hỗ trợ giải mã dữ liệu linh hoạt: Nếu payload đã là đối tượng OrderEvent hoặc
             * chuỗi JSON thô (String/LinkedHashMap), ObjectMapper sẽ chuyển đổi an toàn
             * mà không bị lỗi ClassCastException do khác biệt ClassLoader hay Package.
             */
            if (rawValue instanceof OrderEvent orderEvent) {
                event = orderEvent;
            } else if (rawValue instanceof String jsonString) {
                event = objectMapper.readValue(jsonString, OrderEvent.class);
            } else {
                event = objectMapper.convertValue(rawValue, OrderEvent.class);
            }

            log.info("[KAFKA-CONSUMER-NOTIFICATION] [cid:{}] Nhận sự kiện từ Topic: {} | Partition: {} | Offset: {} | Key: {}",
                    correlationId, record.topic(), record.partition(), record.offset(), record.key());

            // Tiến hành gửi thông báo hóa đơn và email cho khách hàng (Exercise 04)
            notificationService.handleOrderNotification(event);

        } catch (Exception ex) {
            log.error("[KAFKA-CONSUMER-NOTIFICATION] [cid:{}] Lỗi khi xử lý sự kiện thông báo: {}",
                    correlationId, ex.getMessage(), ex);
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
        }
    }
}
