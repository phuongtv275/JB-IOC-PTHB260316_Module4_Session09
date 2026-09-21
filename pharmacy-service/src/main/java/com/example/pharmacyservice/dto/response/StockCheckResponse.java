package com.example.pharmacyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCheckResponse {
    private String medicineCode;
    private String medicineName;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private boolean available;
    private BigDecimal unitPrice;
    private String stockSource;          // "CENTRAL_WAREHOUSE" hoặc "LOCAL_INVENTORY"
    private String circuitBreakerState;  // "CLOSED", "OPEN", "HALF_OPEN"
    private String message;
}
