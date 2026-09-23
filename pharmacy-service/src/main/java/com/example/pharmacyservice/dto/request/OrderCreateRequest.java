package com.example.pharmacyservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO khi thực hiện thanh toán / tạo đơn hàng bán lẻ thuốc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "Mã thuốc (medicineId) không được để trống")
    private String medicineId;

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu phải từ 1")
    private Integer quantity;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @Email(message = "Email khách hàng không đúng định dạng")
    private String customerEmail;
}
