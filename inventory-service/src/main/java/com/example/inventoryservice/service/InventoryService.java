package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    void processStockDeduction(OrderEvent event);

    Page<Inventory> getInventories(Pageable pageable);
}
