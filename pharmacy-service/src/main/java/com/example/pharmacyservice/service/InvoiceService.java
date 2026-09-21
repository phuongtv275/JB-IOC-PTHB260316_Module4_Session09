package com.example.pharmacyservice.service;

import com.example.pharmacyservice.dto.request.InvoiceRequest;
import com.example.pharmacyservice.dto.response.InvoiceResponse;

public interface InvoiceService {

    /**
     * Xuất hóa đơn điện tử với 2 lớp bảo vệ:
     * - Rate Limiter: Tối đa 5 hóa đơn / 10 giây
     * - Retry: Thử lại 3 lần (cách nhau 2s) nếu lỗi mạng
     */
    InvoiceResponse issueInvoice(InvoiceRequest request);

    /**
     * Giả lập số lần lỗi mạng tạm thời để kiểm thử cơ chế Retry
     */
    void setSimulatedNetworkFailures(int count);

    int getRemainingSimulatedFailures();
}
