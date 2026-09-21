package com.example.pharmacyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {
    private String billId;
    private String customerName;
    private List<BillItemResponse> items;
    private BigDecimal subTotalAmount; // Tổng tiền thuốc chưa thuế
    private double vatRate;            // % thuế VAT (ví dụ: 0.10 hoặc 0.08)
    private BigDecimal vatAmount;      // Tiền thuế VAT
    private BigDecimal totalAmount;    // Tổng tiền thanh toán = subTotalAmount + vatAmount
    private LocalDateTime createdAt;
    private String note;
}
