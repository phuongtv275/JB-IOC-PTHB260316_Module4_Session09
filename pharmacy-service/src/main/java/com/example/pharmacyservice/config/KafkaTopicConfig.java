package com.example.pharmacyservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cấu hình khởi tạo tự động các Kafka Topic trong Spring Boot.
 * <p>
 * Khi ứng dụng khởi động, Spring Boot KafkaAdmin sẽ tự động kiểm tra và tạo
 * các topic này trên Kafka Broker nếu chưa tồn tại.
 */
@Slf4j
@Configuration
public class KafkaTopicConfig {

    public static final String MEDICINE_STOCK_EVENTS = "medicine-stock-events";
    public static final String MEDICINE_PRICE_UPDATES = "medicine-price-updates";
    public static final String PHARMACY_NOTIFICATIONS = "pharmacy-notifications";

    /**
     * Topic dành cho sự kiện nhập/xuất kho.
     * Cấu hình 3 partitions để tối ưu hóa xử lý song song với lưu lượng lớn.
     */
    @Bean
    public NewTopic medicineStockEventsTopic() {
        log.info("Khởi tạo cấu hình Kafka Topic: {} với 3 partitions", MEDICINE_STOCK_EVENTS);
        return TopicBuilder.name(MEDICINE_STOCK_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic dành cho sự kiện cập nhật giá thuốc.
     * Cấu hình 1 partition để đảm bảo tính tuần tự tuyệt đối (FIFO), tránh xung đột giá.
     */
    @Bean
    public NewTopic medicinePriceUpdatesTopic() {
        log.info("Khởi tạo cấu hình Kafka Topic: {} với 1 partition (đảm bảo thứ tự)", MEDICINE_PRICE_UPDATES);
        return TopicBuilder.name(MEDICINE_PRICE_UPDATES)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Topic dành cho thông báo hiệu thuốc.
     * Cấu hình 2 partitions giúp cân bằng tải xử lý thông báo song song.
     */
    @Bean
    public NewTopic pharmacyNotificationsTopic() {
        log.info("Khởi tạo cấu hình Kafka Topic: {} với 2 partitions", PHARMACY_NOTIFICATIONS);
        return TopicBuilder.name(PHARMACY_NOTIFICATIONS)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
