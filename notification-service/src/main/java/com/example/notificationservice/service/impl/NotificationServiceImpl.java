package com.example.notificationservice.service.impl;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.filter.CorrelationIdFilter;
import com.example.notificationservice.model.NotificationRecord;
import com.example.notificationservice.service.EmailService;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service xử lý thông báo khách hàng và gửi hóa đơn điện tử.
 *
 * Đáp ứng yêu cầu Bài tập 4:
 * - In ra log thông báo: "Hóa đơn cho đơn hàng [orderId] đã được gửi tới khách hàng".
 * - Mô phỏng tính năng gửi email chi tiết cho khách hàng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationRecord> notificationHistory = new CopyOnWriteArrayList<>();
    private final EmailService emailService;

    @Override
    public void handleOrderNotification(OrderEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);

        /*
         * LOGIC GIẢI THÍCH:
         * 1. Khi nhận được OrderEvent từ Kafka, tiến hành format nội dung hóa đơn điện tử.
         * 2. In dòng log chuẩn theo yêu cầu Exercise 04:
         *    "Hóa đơn cho đơn hàng [orderId] đã được gửi tới khách hàng"
         * 3. Thực hiện gửi email thực tế qua Gmail SMTP nếu có địa chỉ email khách hàng.
         * 4. Lưu lại bản ghi vào lịch sử thông báo phục vụ tra cứu phân trang.
         */
        String recipient = event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()
                ? event.getCustomerEmail()
                : (event.getCustomerName() != null ? event.getCustomerName() : "Khách hàng");

        String subject = "Hóa đơn điện tử cho đơn hàng #" + event.getOrderId();
        String messageBody = String.format("Kính gửi %s, Hóa đơn cho đơn hàng [%s] đã được gửi tới khách hàng. " +
                        "Chi tiết: Thuốc %s, Số lượng: %d, Tổng tiền: %s VNĐ.",
                event.getCustomerName(),
                event.getOrderId(),
                event.getMedicineName() != null ? event.getMedicineName() : event.getMedicineId(),
                event.getQuantity(),
                event.getTotalPrice() != null ? event.getTotalPrice() : "N/A");

        // Dòng log trọng tâm theo yêu cầu Exercise 04
        log.info("[NOTIFICATION-SERVICE] [cid:{}] Hóa đơn cho đơn hàng [{}] đã được gửi tới khách hàng",
                correlationId, event.getOrderId());

        boolean emailSent = false;
        if (event.getCustomerEmail() != null && event.getCustomerEmail().contains("@")) {
            emailSent = emailService.sendInvoiceEmail(event.getCustomerEmail(), event);
        } else {
            log.info("[EMAIL-DISPATCHER] [cid:{}] Đã gửi thông báo tới [{}]: Tiêu đề '{}' | Nội dung: '{}'",
                    correlationId, recipient, subject, messageBody);
            emailSent = true;
        }

        NotificationRecord record = NotificationRecord.builder()
                .id(UUID.randomUUID().toString())
                .orderId(event.getOrderId())
                .recipient(recipient)
                .subject(subject)
                .message(messageBody)
                .status(emailSent ? "SENT" : "FAILED")
                .sentAt(LocalDateTime.now())
                .build();

        notificationHistory.add(0, record);
    }

    @Override
    public Page<NotificationRecord> getNotificationHistory(Pageable pageable) {
        int total = notificationHistory.size();
        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(notificationHistory.subList(start, end), pageable, total);
    }
}
