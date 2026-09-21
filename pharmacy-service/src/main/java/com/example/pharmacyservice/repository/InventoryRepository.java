package com.example.pharmacyservice.repository;

import com.example.pharmacyservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);
}
