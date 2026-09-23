package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEvent;

public interface EmailService {

    /**
     * Gửi hóa đơn điện tử định dạng HTML về địa chỉ email của khách hàng.
     *
     * @param toEmail Địa chỉ email người nhận
     * @param event Thông tin đơn hàng
     * @return true nếu gửi thành công, false nếu gặp lỗi
     */
    boolean sendInvoiceEmail(String toEmail, OrderEvent event);
}
