package com.example.pharmacyservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event đại diện cho sự kiện đơn hàng bán thuốc.
 * Được publish lên topic 'medicine-stock-events'.
 * Message Key được gán là medicineId để đảm bảo thứ tự xử lý của cùng một loại thuốc trên cùng một Partition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String orderId;

    private String medicineId;

    private Integer quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Các thông tin mở rộng phục vụ xuất hóa đơn và gửi email thông báo (Exercise 04)
    private String medicineName;

    private String customerName;

    private String customerEmail;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}
