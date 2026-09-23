package com.example.notificationservice.service.impl;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.filter.CorrelationIdFilter;
import com.example.notificationservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Service xử lý gửi email thực tế thông qua máy chủ SMTP Gmail.
 *
 * Đáp ứng yêu cầu Bài tập 4:
 * - Gửi email thông tin hóa đơn đơn hàng thuốc trực tiếp về hòm thư khách hàng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public boolean sendInvoiceEmail(String toEmail, OrderEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[EMAIL-SERVICE] [cid:{}] Chưa cấu hình tài khoản gửi SMTP (SMTP_USERNAME). Bỏ qua gửi email thực tế cho đơn hàng #{}.",
                    correlationId, event.getOrderId());
            return false;
        }

        log.info("[EMAIL-SERVICE] [cid:{}] Chuẩn bị gửi email hóa đơn đơn hàng #{} tới {}",
                correlationId, event.getOrderId(), toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, "Hiệu Thuốc Pharmacy Services");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận Đơn hàng & Hóa đơn điện tử #" + event.getOrderId() + " - Hiệu thuốc Rikkei Pharmacy");

            String timeStr = event.getTimestamp() != null
                    ? event.getTimestamp().format(DATE_FORMATTER)
                    : "N/A";

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                        .header { background: #007bff; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .content { padding: 24px; }
                        .invoice-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                        .invoice-table th, .invoice-table td { padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: left; }
                        .invoice-table th { background-color: #f8f9fa; color: #495057; font-weight: 600; }
                        .total-row { font-size: 16px; font-weight: bold; color: #d9534f; }
                        .footer { background: #f8f9fa; padding: 16px; text-align: center; font-size: 13px; color: #6c757d; }
                        .badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; background: #28a745; color: #fff; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>HÓA ĐƠN ĐIỆN TỬ BÁN LẺ DƯỢC PHẨM</h1>
                            <p style="margin: 5px 0 0 0; font-size: 14px;">Hệ thống Chuỗi Hiệu thuốc Rikkei Pharmacy</p>
                        </div>
                        <div class="content">
                            <p>Xin chào <strong>%s</strong>,</p>
                            <p>Cảm ơn bạn đã mua hàng tại hệ thống hiệu thuốc. Đơn hàng của bạn đã được thanh toán thành công và hóa đơn điện tử được khởi tạo:</p>
                            
                            <table style="width: 100%%; margin-bottom: 15px; font-size: 14px;">
                                <tr>
                                    <td><strong>Mã đơn hàng:</strong> %s</td>
                                    <td style="text-align: right;"><span class="badge">ĐÃ THANH TOÁN</span></td>
                                </tr>
                                <tr>
                                    <td><strong>Thời gian lập:</strong> %s</td>
                                    <td style="text-align: right;"><strong>Kênh mua:</strong> Bán lẻ trực tiếp</td>
                                </tr>
                            </table>

                            <table class="invoice-table">
                                <thead>
                                    <tr>
                                        <th>Mã thuốc</th>
                                        <th>Tên thuốc</th>
                                        <th style="text-align: center;">SL</th>
                                        <th style="text-align: right;">Đơn giá (VNĐ)</th>
                                        <th style="text-align: right;">Thành tiền (VNĐ)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><code>%s</code></td>
                                        <td><strong>%s</strong></td>
                                        <td style="text-align: center;">%d</td>
                                        <td style="text-align: right;">%s</td>
                                        <td style="text-align: right;">%s</td>
                                    </tr>
                                    <tr class="total-row">
                                        <td colspan="4" style="text-align: right;">Tổng tiền thanh toán:</td>
                                        <td style="text-align: right;">%s VNĐ</td>
                                    </tr>
                                </tbody>
                            </table>

                            <div style="background-color: #e8f4fd; border-left: 4px solid #007bff; padding: 12px; margin-top: 15px; font-size: 13px;">
                                <strong>Lưu ý sử dụng thuốc:</strong> Vui lòng đọc kỹ hướng dẫn sử dụng trước khi dùng hoặc tham khảo ý kiến bác sĩ/dược sĩ. Bảo quản thuốc ở nơi khô ráo, tránh ánh sáng trực tiếp.
                            </div>
                        </div>
                        <div class="footer">
                            <p>Hệ thống tự động gửi email từ Rikkei Pharmacy Services | Hotline: 1900 9999</p>
                            <p style="margin: 0;">Trace Correlation ID: <code>%s</code></p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                    event.getCustomerName() != null ? event.getCustomerName() : "Quý khách",
                    event.getOrderId(),
                    timeStr,
                    event.getMedicineId(),
                    event.getMedicineName() != null ? event.getMedicineName() : event.getMedicineId(),
                    event.getQuantity(),
                    event.getUnitPrice() != null ? String.format("%,.0f", event.getUnitPrice()) : "N/A",
                    event.getTotalPrice() != null ? String.format("%,.0f", event.getTotalPrice()) : "N/A",
                    event.getTotalPrice() != null ? String.format("%,.0f", event.getTotalPrice()) : "N/A",
                    correlationId != null ? correlationId : "N/A"
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("[EMAIL-SERVICE] [cid:{}] Gửi thành công email hóa đơn cho đơn hàng #{} tới {}",
                    correlationId, event.getOrderId(), toEmail);
            return true;

        } catch (Exception ex) {
            log.error("[EMAIL-SERVICE] [cid:{}] Thất bại khi gửi email tới {}: {}",
                    correlationId, toEmail, ex.getMessage(), ex);
            return false;
        }
    }
}
