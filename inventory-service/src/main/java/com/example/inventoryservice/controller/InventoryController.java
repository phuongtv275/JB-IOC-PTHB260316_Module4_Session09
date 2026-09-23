package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.response.ApiResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.filter.CorrelationIdFilter;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API tra cứu tồn kho dược tại inventory-service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * API lấy danh sách thuốc trong kho có phân trang (Chuẩn quy định AGENTS.md)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Inventory>>> getInventories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[INVENTORY-API] [cid:{}] Lấy danh sách tồn kho - Trang: {}, Size: {}", correlationId, page, size);

        Page<Inventory> inventoryPage = inventoryService.getInventories(PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(inventoryPage, "Lấy danh sách tồn kho thành công", correlationId));
    }
}
