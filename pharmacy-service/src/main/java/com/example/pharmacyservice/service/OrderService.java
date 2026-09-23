package com.example.pharmacyservice.service;

import com.example.pharmacyservice.dto.request.OrderCreateRequest;
import com.example.pharmacyservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrderAndPublishEvent(OrderCreateRequest request);

    Page<OrderResponse> getOrders(Pageable pageable);
}
