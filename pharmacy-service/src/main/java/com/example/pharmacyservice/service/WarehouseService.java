package com.example.pharmacyservice.service;

import com.example.pharmacyservice.dto.request.StockCheckRequest;
import com.example.pharmacyservice.dto.response.StockCheckResponse;

public interface WarehouseService {

    /**
     * Kiểm tra hàng tồn kho từ kho tổng với cơ chế ngắt mạch Circuit Breaker.
     *
     * @param request Thông tin mã thuốc và số lượng cần kiểm tra
     * @return Kết quả kiểm tra tồn kho
     */
    StockCheckResponse checkStock(StockCheckRequest request);

    /**
     * Giả lập trạng thái sự cố của kho tổng phục vụ kiểm thử Circuit Breaker.
     *
     * @param down true nếu giả lập sập mạng, false nếu bình thường
     */
    void setWarehouseDown(boolean down);

    /**
     * Kiểm tra xem kho tổng đang bị giả lập sập hay không.
     */
    boolean isWarehouseDown();

    /**
     * Lấy trạng thái hiện tại của Circuit Breaker warehouseCB.
     */
    String getCircuitBreakerState();
}
