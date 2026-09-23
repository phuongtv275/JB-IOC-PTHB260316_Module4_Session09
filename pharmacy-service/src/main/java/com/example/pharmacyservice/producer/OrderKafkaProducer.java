package com.example.pharmacyservice.producer;

import com.example.pharmacyservice.dto.event.OrderEvent;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer gửi sự kiện đơn hàng thuốc lên Kafka Broker.
 *
 * Đáp ứng yêu cầu Bài tập 2 & Bài tập 5:
 * - Gửi OrderEvent dưới định dạng JSON.
 * - Sử dụng Message Key là medicineId để các đơn hàng cùng loại thuốc luôn được phân phối
 *   vào cùng một Partition (đảm bảo thứ tự xuất nhập kho chính xác theo từng thuốc).
 * - Topic name được tiêm từ Config Server và có thể thay đổi động thông qua @RefreshScope (Exercise 05).
 */
@Slf4j
@RefreshScope
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.template.default-topic:medicine-stock-events}")
    private String defaultTopic;

    /**
     * Gửi OrderEvent lên Kafka Broker với message key là medicineId.
     *
     * @param orderEvent Dữ liệu sự kiện đơn hàng
     * @return CompletableFuture chứa kết quả gửi tin nhắn
     */
    public CompletableFuture<SendResult<String, Object>> sendOrderEvent(OrderEvent orderEvent) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        String key = orderEvent.getMedicineId();

        /*
         * LOGIC GIẢI THÍCH:
         * 1. Message Key = medicineId đảm bảo cơ chế DefaultPartitioner của Kafka sẽ hash key
         *    và đưa các sự kiện của cùng 1 loại thuốc vào chính xác cùng 1 Partition.
         * 2. Thao tác gửi là asynchronous qua CompletableFuture, gắn callback để log thông tin
         *    Partition và Offset phục vụ debug/tracing theo AGENTS.md.
         */
        log.info("[KAFKA-PRODUCER] [cid:{}] Đang gửi OrderEvent [orderId: {}, key(medicineId): {}] vào topic '{}'",
                correlationId, orderEvent.getOrderId(), key, defaultTopic);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(defaultTopic, key, orderEvent);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCER] [cid:{}] Gửi thành công OrderEvent [orderId: {}] | Topic: {} | Partition: {} | Offset: {}",
                        correlationId,
                        orderEvent.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[KAFKA-PRODUCER] [cid:{}] Gửi thất bại OrderEvent [orderId: {}] | Lỗi: {}",
                        correlationId, orderEvent.getOrderId(), ex.getMessage(), ex);
            }
        });

        return future;
    }
}
