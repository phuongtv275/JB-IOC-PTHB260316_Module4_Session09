package com.example.pharmacyservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event cảnh báo thuốc sắp hết hạn (Exercise 06).
 * Được publish lên topic 'pharmacy-notifications' với cấu hình acks=all và retries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertEvent implements Serializable {

    private String alertId;

    private String medicineCode;

    private String medicineName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private Long daysRemaining;

    private Integer stockQuantity;

    private String alertMessage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
