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
public class InvoiceRequest {

    @NotBlank(message = "Mã máy bán thuốc (POS Terminal ID) không được để trống")
    private String posTerminalId;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String customerTaxCode;

    @NotEmpty(message = "Danh sách thuốc không được để trống")
    @Valid
    private List<BillItemRequest> items;

    @Builder.Default
    private String paymentMethod = "CASH";
}
