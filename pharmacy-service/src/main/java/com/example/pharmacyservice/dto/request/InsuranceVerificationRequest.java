package com.example.pharmacyservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class InsuranceVerificationRequest {

    @NotBlank(message = "Tên bệnh nhân không được để trống")
    private String patientName;

    @NotBlank(message = "Mã thẻ bảo hiểm y tế (BHYT) không được để trống")
    private String insuranceCardNumber;

    @NotBlank(message = "Mã đơn thuốc không được để trống")
    private String prescriptionCode;

    @NotNull(message = "Tổng tiền thuốc không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tổng tiền thuốc phải lớn hơn 0")
    private BigDecimal medicineTotalAmount;
}
