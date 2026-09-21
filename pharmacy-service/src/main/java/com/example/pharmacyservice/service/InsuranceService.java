package com.example.pharmacyservice.service;

import com.example.pharmacyservice.dto.request.InsuranceVerificationRequest;
import com.example.pharmacyservice.dto.response.InsuranceVerificationResponse;

import java.util.concurrent.CompletableFuture;

public interface InsuranceService {

    /**
     * Xác thực thẻ bảo hiểm y tế với Full Resilience Stack:
     * - TimeLimiter: Ngắt khi quá thời gian timeout (mặc định 3s)
     * - Retry: Thử lại 3 lần (cách nhau 2s) nếu lỗi mạng
     * - Circuit Breaker: Ngắt mạch khi tỷ lệ lỗi vượt quá 60%
     * - Fallback: Trả về giá gốc chưa chiết khấu kèm ghi chú "Xác thực bảo hiểm sau"
     */
    CompletableFuture<InsuranceVerificationResponse> verifyInsurance(InsuranceVerificationRequest request, Integer simulatedDelayMs);

    String getCircuitBreakerState();
}
