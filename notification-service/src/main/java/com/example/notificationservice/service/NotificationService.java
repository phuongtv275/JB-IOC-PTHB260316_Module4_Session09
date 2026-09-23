package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.model.NotificationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void handleOrderNotification(OrderEvent orderEvent);

    Page<NotificationRecord> getNotificationHistory(Pageable pageable);
}
