package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByMedicineCode(String medicineCode);

    Page<Inventory> findAll(Pageable pageable);

    /**
     * Cập nhật trừ số lượng tồn kho trực tiếp qua câu lệnh UPDATE (Exercise 03)
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.localStockQuantity = i.localStockQuantity - :quantity, i.updatedAt = :now WHERE i.medicineCode = :medicineCode AND i.localStockQuantity >= :quantity")
    int deductStock(@Param("medicineCode") String medicineCode, @Param("quantity") Integer quantity, @Param("now") LocalDateTime now);
}
