package com.example.pharmacyservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCheckRequest {

    @NotBlank(message = "Mã thuốc không được để trống")
    private String medicineCode;

    @NotNull(message = "Số lượng cần kiểm tra không được để trống")
    @Min(value = 1, message = "Số lượng cần kiểm tra phải lớn hơn hoặc bằng 1")
    private Integer quantity;
}
