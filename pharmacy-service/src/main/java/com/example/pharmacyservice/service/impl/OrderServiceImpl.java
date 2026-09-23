package com.example.pharmacyservice.service.impl;

import com.example.pharmacyservice.dto.event.OrderEvent;
import com.example.pharmacyservice.dto.request.OrderCreateRequest;
import com.example.pharmacyservice.dto.response.OrderResponse;
import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.entity.Order;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import com.example.pharmacyservice.producer.OrderKafkaProducer;
import com.example.pharmacyservice.repository.InventoryRepository;
import com.example.pharmacyservice.repository.OrderRepository;
import com.example.pharmacyservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ bán lẻ thuốc và xuất bản sự kiện đơn hàng lên Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;

    @Override
    @Transactional
    public OrderResponse createOrderAndPublishEvent(OrderCreateRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[ORDER-SERVICE] [cid:{}] Bắt đầu xử lý thanh toán đơn hàng cho thuốc: {}, số lượng: {}",
                correlationId, request.getMedicineId(), request.getQuantity());

        // 1. Kiểm tra sự tồn tại của thuốc trong kho
        Inventory inventory = inventoryRepository.findByMedicineCode(request.getMedicineId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin thuốc với mã: " + request.getMedicineId()));

        // 2. Kiểm tra tồn kho khả dụng
        if (inventory.getLocalStockQuantity() < request.getQuantity()) {
            log.warn("[ORDER-SERVICE] [cid:{}] Thuốc {} không đủ tồn kho. Hiện có: {}, Yêu cầu: {}",
                    correlationId, inventory.getMedicineCode(), inventory.getLocalStockQuantity(), request.getQuantity());
            throw new IllegalStateException("Số lượng tồn kho không đủ (Hiện còn: " + inventory.getLocalStockQuantity() + " đơn vị)");
        }

        // 3. Tính toán hóa đơn và tạo Order
        BigDecimal unitPrice = inventory.getUnitPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderId(orderId)
                .medicineId(inventory.getMedicineCode())
                .medicineName(inventory.getMedicineName())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .customerName(request.getCustomerName())
                .status("PAID")
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4. Tạo OrderEvent và gửi lên Kafka Broker (Exercise 02)
        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(savedOrder.getOrderId())
                .medicineId(savedOrder.getMedicineId())
                .medicineName(savedOrder.getMedicineName())
                .quantity(savedOrder.getQuantity())
                .unitPrice(savedOrder.getUnitPrice())
                .totalPrice(savedOrder.getTotalPrice())
                .customerName(savedOrder.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .timestamp(LocalDateTime.now())
                .build();

        orderKafkaProducer.sendOrderEvent(orderEvent);

        log.info("[ORDER-SERVICE] [cid:{}] Đơn hàng {} đã thanh toán thành công và gửi sự kiện lên Kafka",
                correlationId, orderId);

        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .medicineId(savedOrder.getMedicineId())
                .medicineName(savedOrder.getMedicineName())
                .quantity(savedOrder.getQuantity())
                .unitPrice(savedOrder.getUnitPrice())
                .totalPrice(savedOrder.getTotalPrice())
                .customerName(savedOrder.getCustomerName())
                .status(savedOrder.getStatus())
                .createdAt(savedOrder.getCreatedAt())
                .kafkaMessageStatus("PUBLISHED_TO_KAFKA")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(o -> OrderResponse.builder()
                        .orderId(o.getOrderId())
                        .medicineId(o.getMedicineId())
                        .medicineName(o.getMedicineName())
                        .quantity(o.getQuantity())
                        .unitPrice(o.getUnitPrice())
                        .totalPrice(o.getTotalPrice())
                        .customerName(o.getCustomerName())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .kafkaMessageStatus("SAVED")
                        .build());
    }
}
