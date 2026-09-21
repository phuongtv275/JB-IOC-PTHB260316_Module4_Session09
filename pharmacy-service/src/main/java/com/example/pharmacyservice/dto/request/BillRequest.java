package com.example.pharmacyservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @NotEmpty(message = "Danh sách thuốc trong hóa đơn không được để trống")
    @Valid
    private List<BillItemRequest> items;
}
