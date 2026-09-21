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
public class BillItemResponse {
    private String medicineName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
