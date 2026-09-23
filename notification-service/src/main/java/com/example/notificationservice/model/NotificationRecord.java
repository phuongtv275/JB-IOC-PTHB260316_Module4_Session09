package com.example.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {

    private String id;
    private String orderId;
    private String recipient;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime sentAt;
}
