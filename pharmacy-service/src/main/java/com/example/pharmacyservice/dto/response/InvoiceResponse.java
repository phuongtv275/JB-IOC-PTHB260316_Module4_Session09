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
public class InvoiceResponse {
    private String invoiceNumber;
    private String posTerminalId;
    private String customerName;
    private BigDecimal subTotalAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private String status;        // "ISSUED", "RATE_LIMITED", "RETRY_FAILED"
    private int attemptCount;     // Số lần đã thử kết nối
    private LocalDateTime issuedAt;
    private String message;
}
