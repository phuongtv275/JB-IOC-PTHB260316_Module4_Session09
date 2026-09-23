package com.example.pharmacyservice.config;

import com.example.pharmacyservice.entity.Inventory;
import com.example.pharmacyservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Khởi tạo dữ liệu tồn kho cục bộ ban đầu (Data Seeder) cho hiệu thuốc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        if (inventoryRepository.count() == 0) {
            log.info("[DATA-SEEDER] Khởi tạo dữ liệu tồn kho thuốc cục bộ ban đầu...");

            java.time.LocalDate today = java.time.LocalDate.now();
            Inventory p1 = Inventory.builder()
                    .medicineCode("PARA500")
                    .medicineName("Paracetamol 500mg")
                    .localStockQuantity(150)
                    .unitPrice(BigDecimal.valueOf(50000))
                    .shelfLocation("Tủ A1 - Ngăn 1")
                    .expiryDate(today.plusMonths(12))
                    .status("AVAILABLE")
                    .build();

            Inventory p2 = Inventory.builder()
                    .medicineCode("VITA1000")
                    .medicineName("Vitamin C 1000mg")
                    .localStockQuantity(80)
                    .unitPrice(BigDecimal.valueOf(95000))
                    .shelfLocation("Tủ B2 - Ngăn 3")
                    .expiryDate(today.plusMonths(6))
                    .status("AVAILABLE")
                    .build();

            // AMOX500 giả lập sắp hết hạn (15 ngày tới) để kích hoạt cảnh báo ở Bài 6
            Inventory p3 = Inventory.builder()
                    .medicineCode("AMOX500")
                    .medicineName("Amoxicillin 500mg (Kháng sinh)")
                    .localStockQuantity(30)
                    .unitPrice(BigDecimal.valueOf(120000))
                    .shelfLocation("Tủ C3 - Ngăn Kháng Sinh")
                    .expiryDate(today.plusDays(15))
                    .status("AVAILABLE")
                    .build();

            inventoryRepository.saveAll(List.of(p1, p2, p3));
            log.info("[DATA-SEEDER] Đã nạp thành công 3 loại thuốc vào kho cục bộ.");
        } else {
            // Cập nhật bổ sung expiryDate nếu dữ liệu cũ chưa có
            List<Inventory> all = inventoryRepository.findAll();
            java.time.LocalDate today = java.time.LocalDate.now();
            boolean needUpdate = false;
            for (Inventory item : all) {
                if (item.getExpiryDate() == null) {
                    needUpdate = true;
                    if ("AMOX500".equals(item.getMedicineCode())) {
                        item.setExpiryDate(today.plusDays(15));
                    } else if ("VITA1000".equals(item.getMedicineCode())) {
                        item.setExpiryDate(today.plusMonths(6));
                    } else {
                        item.setExpiryDate(today.plusMonths(12));
                    }
                }
                if (item.getStatus() == null) {
                    item.setStatus("AVAILABLE");
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                inventoryRepository.saveAll(all);
                log.info("[DATA-SEEDER] Đã cập nhật expiryDate và status cho dữ liệu thuốc hiện có.");
            }
        }
    }
}
