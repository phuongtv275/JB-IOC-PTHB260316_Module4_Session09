package com.example.pharmacyservice.producer;

import com.example.pharmacyservice.config.KafkaTopicConfig;
import com.example.pharmacyservice.dto.event.ExpiryAlertEvent;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer gửi cảnh báo thuốc sắp hết hạn lên topic 'pharmacy-notifications'.
 *
 * Đáp ứng yêu cầu Bài tập 6:
 * - Cấu hình acks = all và retries đảm bảo độ tin cậy tuyệt đối (Reliability),
 *   sự kiện cảnh báo không bao giờ bị mất.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryAlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> sendExpiryAlert(ExpiryAlertEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        String topic = KafkaTopicConfig.PHARMACY_NOTIFICATIONS;
        String key = event.getMedicineCode();

        /*
         * LOGIC GIẢI THÍCH:
         * 1. Sử dụng topic pharmacy-notifications để tách biệt luồng cảnh báo/thông báo
         *    với luồng giao dịch mua bán kho (medicine-stock-events).
         * 2. Message key là medicineCode giúp tuần tự hóa các thông báo của từng loại thuốc.
         */
        log.info("[EXPIRY-ALERT-PRODUCER] [cid:{}] Gửi cảnh báo thuốc sắp hết hạn [{}] vào topic '{}'",
                correlationId, event.getMedicineCode(), topic);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[EXPIRY-ALERT-PRODUCER] [cid:{}] Đã gửi thành công cảnh báo [{}] | Partition: {} | Offset: {}",
                        correlationId,
                        event.getMedicineCode(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[EXPIRY-ALERT-PRODUCER] [cid:{}] Thất bại khi gửi cảnh báo [{}] | Lỗi: {}",
                        correlationId, event.getMedicineCode(), ex.getMessage(), ex);
            }
        });

        return future;
    }
}
