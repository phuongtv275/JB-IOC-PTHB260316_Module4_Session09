package com.example.pharmacyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceVerificationResponse {
    private String patientName;
    private String insuranceCardNumber;
    private String prescriptionCode;
    private BigDecimal originalAmount;       // Giá thuốc gốc chưa chiết khấu
    private BigDecimal discountAmount;       // Số tiền BHYT chi trả
    private BigDecimal finalAmount;          // Tiền người bệnh phải thanh toán
    private double coveragePercent;          // Tỷ lệ BHYT hỗ trợ (VD: 80% hoặc 0% khi fallback)
    private boolean verified;                // Đã xác thực thành công hay chưa
    private String note;                     // Ghi chú (VD: "Xác thực bảo hiểm sau")
    private String circuitBreakerState;      // Trạng thái Circuit Breaker
    private long executionTimeMs;
    private LocalDateTime processedAt;
}
